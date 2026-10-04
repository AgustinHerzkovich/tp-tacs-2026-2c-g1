"use client";

import { useState } from "react";
import { CalendarCheck, Plus, Trash2 } from "lucide-react";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  compareLocalDateTimes,
  formatActivityWhen,
  fromDateTimeLocalValue,
  parseLocalDateTime,
  toDateTimeLocalValue,
} from "@/lib/formatDate";
import type { UseVoting } from "@/hooks/useVoting";

interface VotationAdminDialogProps {
  voting: UseVoting;
  onOpenChange: (open: boolean) => void;
}

/** One alternative in the editable list: the raw `datetime-local` value, the
 * votes the option currently carries (so the organizer can see what removing it
 * costs) and whether the date is still editable. */
interface DraftOption {
  value: string;
  votes: number;
  /** Only dates added in this session are editable: the ones already published
   * are what participants are voting on, so they are shown as fixed text. */
  editable: boolean;
}

/** Organizer-only sheet to run a votation: quorum, how long it stays open and
 * the list of alternative dates (add/remove).
 *
 * The two groups go through two different endpoints, each with its own button,
 * so a rejection of one never leaves the other half-applied without the
 * organizer noticing.
 *
 * The votation is replaced wholesale by the options endpoint, so the drafts are
 * seeded from the votation in the `useState` initializers: every visit starts
 * from what the backend actually has, never from an abandoned previous edit.
 * The component is mounted only while open (see `VotingRoom`), which is what
 * guarantees that seeding.
 *
 * Saving the alternatives is where votes can be destroyed: the options endpoint
 * keeps the votes of the dates that survive the edit and drops the rest, and
 * rejects the whole request with `VOTATION_VOTES_AT_RISK` when that happens
 * without the organizer acknowledging it. The sheet knows which published dates
 * it is about to drop (their votes are shown in the list), so the first save only
 * reveals an inline warning inside the panel - next to the dates and the count
 * that makes the loss concrete - and replays the same save with
 * `allowVoteLoss` from there. The warning replaces no overlay: the organizer
 * keeps reading the list while deciding, and the list itself stays frozen so the
 * count in the warning can never go stale. */
export function VotationAdminDialog({ voting, onOpenChange }: VotationAdminDialogProps) {
  const votation = voting.votation;
  const [options, setOptions] = useState<DraftOption[]>(() =>
    (votation?.options ?? [])
      .map((option) => ({
        value: toDateTimeLocalValue(option.dateTime),
        votes: option.voteCount,
        editable: false,
      }))
      .sort((a, b) => compareLocalDateTimes(a.value, b.value)),
  );
  const [quorum, setQuorum] = useState(() => String(Math.round((votation?.minQuorum ?? 0.5) * 100)));
  const [durationHours, setDurationHours] = useState(() =>
    String(remainingHours(votation?.closingDate ?? null)),
  );
  const [saving, setSaving] = useState<"settings" | "options" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmVoteLoss, setConfirmVoteLoss] = useState(false);

  if (!votation) return null;

  const closed = votation.status !== "ACTIVE";
  const filledValues = options.map((option) => option.value).filter(Boolean);
  const duplicates = new Set(filledValues).size !== filledValues.length;
  const optionsInvalid = options.length === 0 || filledValues.length !== options.length || duplicates;
  const settingsInvalid =
    !Number.isFinite(Number(quorum)) ||
    Number(quorum) < 1 ||
    Number(quorum) > 100 ||
    !Number.isFinite(Number(durationHours)) ||
    Number(durationHours) < 1;

  // Votes of the published dates this draft would delete, i.e. the cost of the
  // save the organizer is about to trigger. Comparing against the votation (not
  // against the draft) keeps re-adding a date in the same session vote-safe.
  const votesAtRisk = (votation.options ?? [])
    .filter((option) => !filledValues.includes(toDateTimeLocalValue(option.dateTime)))
    .reduce((sum, option) => sum + option.voteCount, 0);

  const save = async (section: "settings" | "options", run: () => Promise<void>) => {
    setError(null);
    setSaving(section);
    try {
      await run();
      onOpenChange(false);
    } catch (cause: unknown) {
      setError(
        cause instanceof Error
          ? cause.message
          : section === "settings"
            ? "No pudimos guardar la configuración."
            : "No pudimos guardar las alternativas.",
      );
    } finally {
      setSaving(null);
    }
  };

  const optionDates = () => options.map((option) => fromDateTimeLocalValue(option.value));

  const requestSaveOptions = () => {
    if (votesAtRisk > 0) {
      setConfirmVoteLoss(true);
      return;
    }
    void save("options", () => voting.updateOptions(optionDates()));
  };

  /** Replays the very same save, this time acknowledging the destroyed votes.
   * The warning clears on both outcomes: a rejection surfaces its error in the
   * sheet, and a retry goes through the same two steps again. */
  const confirmSaveOptions = async () => {
    try {
      await save("options", () => voting.updateOptions(optionDates(), true));
    } finally {
      setConfirmVoteLoss(false);
    }
  };

  return (
    <Sheet open onOpenChange={onOpenChange}>
      <SheetContent
        side="bottom"
        className="flex max-h-[85dvh] flex-col gap-0 overflow-hidden rounded-t-3xl border-0"
      >
        <SheetHeader className="gap-1 px-5 pb-3">
          <SheetTitle className="font-brand text-xl">Administrar votación</SheetTitle>
          <SheetDescription className="text-[13px] font-bold leading-snug">
            {closed
              ? "Esta votación ya no está abierta: no se pueden cambiar sus fechas."
              : "Ajustá el quórum y el plazo, y agregá o quitá fechas alternativas."}
          </SheetDescription>
        </SheetHeader>

        <div className="scrollbar-thin flex min-h-0 flex-1 flex-col gap-5 overflow-y-auto overscroll-contain px-5 pb-4">
          <section className="flex flex-col gap-3">
            <h3
              className="text-[11px] font-extrabold uppercase tracking-wide"
              style={{ color: "var(--muted-foreground)" }}
            >
              Quórum y plazo
            </h3>
            <p className="text-[12.5px] font-bold">
              Cierra el {votation.closingDate ? formatActivityWhen(votation.closingDate) : "—"}
            </p>
            <div className="grid grid-cols-2 gap-3">
              <label className="text-xs font-extrabold">
                Quórum (%)
                <Input
                  type="number"
                  min={1}
                  max={100}
                  value={quorum}
                  disabled={closed}
                  onChange={(event) => setQuorum(event.target.value)}
                  className="mt-1"
                />
              </label>
              <label className="text-xs font-extrabold">
                Duración (horas)
                <Input
                  type="number"
                  min={1}
                  value={durationHours}
                  disabled={closed}
                  onChange={(event) => setDurationHours(event.target.value)}
                  className="mt-1"
                />
              </label>
            </div>
            <p className="text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
              El quórum es el porcentaje de votos necesario para definir el resultado. Al guardar, el
              plazo se cuenta desde ahora y tiene que cerrar antes de la primera alternativa.
            </p>
            <Button
              type="button"
              className="w-full rounded-xl"
              disabled={closed || saving !== null || settingsInvalid}
              onClick={() =>
                void save("settings", () =>
                  voting.updateSettings(Number(quorum) / 100, Number(durationHours)),
                )
              }
            >
              {saving === "settings" ? "Guardando..." : "Guardar configuración"}
            </Button>
          </section>

          <section className="flex flex-col gap-3">
            <h3
              className="text-[11px] font-extrabold uppercase tracking-wide"
              style={{ color: "var(--muted-foreground)" }}
            >
              Fechas alternativas
            </h3>
            <ul className="flex flex-col gap-2">
              {options.map((option, index) => (
                <li key={index} className="flex items-center gap-2">
                  {option.editable ? (
                    <Input
                      type="datetime-local"
                      aria-label={`Fecha alternativa ${index + 1}`}
                      value={option.value}
                      disabled={closed}
                      onChange={(event) =>
                        setOptions((all) =>
                          all.map((item, itemIndex) =>
                            itemIndex === index ? { ...item, value: event.target.value } : item,
                          ),
                        )
                      }
                    />
                  ) : (
                    <span
                      className="flex h-8 min-w-0 flex-1 items-center gap-1.5 rounded-lg border border-input px-2.5 text-[13px] font-bold"
                      title="Las fechas ya publicadas no se editan: quitála y agregá la nueva si la cambiás."
                    >
                      <CalendarCheck
                        className="size-3.5 shrink-0"
                        style={{ color: "var(--muted-foreground)" }}
                        aria-hidden="true"
                      />
                      {option.value ? formatActivityWhen(option.value) : "—"}
                    </span>
                  )}
                  {option.votes > 0 && (
                    <span
                      className="shrink-0 rounded-full px-2 py-1 text-[10.5px] font-black"
                      style={{ background: "var(--mint)", color: "var(--mint-ink)" }}
                    >
                      {option.votes} {option.votes === 1 ? "voto" : "votos"}
                    </span>
                  )}
                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
disabled={closed || confirmVoteLoss}
                aria-label={
                  option.value
                    ? `Quitar ${formatActivityWhen(fromDateTimeLocalValue(option.value))}`
                    : `Quitar alternativa ${index + 1}`
                }
                    onClick={() =>
                      setOptions((all) => all.filter((_, itemIndex) => itemIndex !== index))
                    }
                  >
                    <Trash2 className="size-4" />
                  </Button>
                </li>
              ))}
            </ul>
            <Button
              type="button"
              variant="outline"
              className="w-full rounded-xl border-2 border-dashed py-3 text-[13px] font-extrabold hover:brightness-[0.98]"
              style={{
                borderColor: "var(--border)",
                background: "var(--muted)",
                color: "var(--foreground)",
              }}
              disabled={closed || confirmVoteLoss}
              onClick={() => setOptions((all) => [...all, { value: "", votes: 0, editable: true }])}
            >
              <span
                className="flex size-6 shrink-0 items-center justify-center rounded-lg bg-white"
                style={{ boxShadow: "0 3px 0 var(--lav)" }}
                aria-hidden="true"
              >
                <Plus className="size-3.5" style={{ color: "var(--primary)" }} />
              </span>
              Agregar alternativa
            </Button>
            <p className="text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
              Las fechas tienen que estar dentro del rango de reprogramación y con buen clima. Las que
              ya están publicadas no se editan: si las cambiás, quitá la fecha y agregá la nueva. Si
              quitás una que ya tiene votos, te lo avisamos antes de guardar.
            </p>
            {confirmVoteLoss && (
              <div
                aria-label="Confirmación de pérdida de votos"
                className="flex flex-col gap-2 rounded-xl p-3"
                style={{ border: "2px solid var(--destructive)", background: "var(--muted)" }}
              >
                <p className="text-[13px] font-extrabold" style={{ color: "var(--destructive)" }}>
                  {votesAtRisk === 1
                    ? "Vas a perder 1 voto"
                    : `Vas a perder ${votesAtRisk} votos`}
                </p>
                <p className="text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
                  {votesAtRisk === 1
                    ? "La fecha que quitás ya tiene un voto de los participantes. Ese voto no se puede recuperar."
                    : "Las fechas que quitás ya tienen votos de los participantes. Esos votos no se pueden recuperar."}
                </p>
                <div className="flex flex-col gap-2">
                  <Button
                    type="button"
                    variant="destructive"
                    className="w-full rounded-xl"
                    disabled={saving !== null}
                    onClick={() => void confirmSaveOptions()}
                  >
                    {saving === "options" ? "Guardando..." : "Perder los votos y guardar"}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    className="w-full rounded-xl"
                    disabled={saving !== null}
                    onClick={() => setConfirmVoteLoss(false)}
                  >
                    Volver
                  </Button>
                </div>
              </div>
            )}
            <Button
              type="button"
              className="w-full rounded-xl"
              disabled={closed || saving !== null || optionsInvalid}
              onClick={requestSaveOptions}
            >
              {saving === "options" ? "Guardando..." : "Guardar alternativas"}
            </Button>
          </section>

          {error && (
            <p role="alert" className="text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
              {error}
            </p>
          )}
        </div>

        <SheetFooter className="px-5 pb-6">
          <Button
            type="button"
            variant="outline"
            size="xl"
            className="w-full rounded-xl"
            onClick={() => onOpenChange(false)}
          >
            Cerrar
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

/** Whole hours left until the votation closes, at least 1 so the input never
 * starts empty or below the minimum the backend accepts. */
function remainingHours(closingDate: string | null): number {
  if (!closingDate) return 24;
  const ms = parseLocalDateTime(closingDate).getTime() - new Date().getTime();
  return Math.max(1, Math.ceil(ms / 3_600_000));
}

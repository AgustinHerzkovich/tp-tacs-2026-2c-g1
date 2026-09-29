import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { useState, type CSSProperties } from "react";
import { Plus, Settings2, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import type { UseVoting } from "@/hooks/useVoting";

interface VotingRoomProps {
  voting: UseVoting;
  warningText?: string;
  organizer?: boolean;
  /** Caps the card's height to match the sibling info column, measured via
   * ResizeObserver in the parent — keeps the header and vote button always
   * visible on desktop, with only the options list scrolling. Below `lg`
   * (where the columns stack instead of sitting side by side) a fixed
   * viewport-relative cap applies instead, see the `max-h-[75vh]` fallback. */
  maxHeightPx?: number;
}

export function VotingRoom({ voting, warningText, organizer = false, maxHeightPx }: VotingRoomProps) {
  const { options, total, selectedId, votedId, select, requestVote, pending } = voting;
  const [editing, setEditing] = useState(false);
  const [dates, setDates] = useState<string[]>([]);
  const [quorum, setQuorum] = useState("50");
  const [duration, setDuration] = useState("24");
  const [adminError, setAdminError] = useState<string | null>(null);

  return (
    <Card
      className="p-[18px] mb-6 flex flex-col border-2 border-dashed rounded-[24px] shadow-[0_8px_20px_-12px_rgba(58,51,82,.25)] max-h-[75vh] lg:max-h-[var(--voting-max-h,75vh)]"
      style={
        {
          borderColor: "var(--border)",
          ...(maxHeightPx ? { "--voting-max-h": `${maxHeightPx}px` } : {}),
        } as CSSProperties
      }
    >
      <div className="flex items-center justify-between gap-2 mb-2 px-4 shrink-0">
        <span
          className="inline-flex items-center rounded-tl-[10px] rounded-tr-[10px] rounded-br-[10px] rounded-bl-[2px] border-2 border-white px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wide"
          style={{ background: "var(--sun)", color: "var(--sun-ink)", boxShadow: "0 3px 0 var(--sun-ink)" }}
        >
          🗳️ Votación de reprogramación
        </span>
        <Badge variant="secondary" className="shrink-0">{total} votos</Badge>
      </div>
      <p className="text-[11.5px] font-bold leading-snug mb-4 px-4 shrink-0" style={{ color: "var(--muted-foreground)" }}>
        {warningText ?? "El clima no acompaña — elegí la fecha alternativa que más te sirva."}
      </p>

      <div className="flex-1 min-h-0 overflow-y-auto space-y-3 mb-4 px-4">
        {options.map((option) => {
          const pct = total ? Math.round((option.votes / total) * 100) : 0;
          const isSelected = selectedId === option.id;
          const isVoted = votedId === option.id;
          const active = isSelected || isVoted;
          return (
            <button
              key={option.id}
              type="button"
              onClick={() => select(option.id)}
              aria-pressed={active}
              className="tap block w-full text-left rounded-xl border-2 p-3"
              style={{ borderColor: active ? "var(--primary)" : "var(--border)", background: active ? "var(--secondary)" : "#fff" }}
            >
              <div className="flex items-center justify-between mb-2">
                <span className="flex items-center gap-2 font-extrabold text-[13px]">
                  <span className="w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0" style={{ borderColor: active ? "var(--primary)" : "var(--muted-foreground)" }}>
                    {active && <span className="w-2.5 h-2.5 rounded-full" style={{ background: "var(--primary)" }} />}
                  </span>
                  {option.label} {isVoted && "✓"}
                </span>
                <span className="font-display font-semibold text-[13px] tabular-nums" style={{ color: "var(--accent-foreground)" }}>
                  {pct}%
                </span>
              </div>
              <Progress value={pct} className="h-2.5 [&_[data-slot=progress-indicator]]:bg-[var(--primary)]" />
            </button>
          );
        })}
      </div>

      <div className="px-4 shrink-0">
        <Button size="xl" className="w-full" disabled={pending || !selectedId || selectedId === votedId} onClick={requestVote}>
          {pending ? "Guardando voto..." : votedId ? "Cambiar voto" : "Votar fecha alternativa"}
        </Button>
      </div>

      {organizer && (
        <div className="mx-4 mt-4 pt-4 border-t-2 shrink-0" style={{ borderColor: "var(--border)" }}>
          <Button type="button" variant="outline" className="w-full rounded-xl" onClick={() => setEditing((value) => !value)}><Settings2 className="size-4" /> Administrar votación</Button>
          {editing && (
            <div className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <label className="text-xs font-extrabold">Quórum (%)<Input type="number" min="0" max="100" value={quorum} onChange={(event) => setQuorum(event.target.value)} className="mt-1" /></label>
                <label className="text-xs font-extrabold">Duración (horas)<Input type="number" min="1" value={duration} onChange={(event) => setDuration(event.target.value)} className="mt-1" /></label>
              </div>
              <Button type="button" className="w-full rounded-xl" onClick={() => void voting.updateSettings(Number(quorum) / 100, Number(duration)).catch((error: unknown) => setAdminError(error instanceof Error ? error.message : "No se pudo actualizar."))}>Guardar configuración</Button>
              <div>
                <p className="text-xs font-extrabold mb-2">Nuevas alternativas</p>
                {dates.map((date, index) => <div key={index} className="flex gap-2 mb-2"><Input type="datetime-local" value={date} onChange={(event) => setDates((all) => all.map((item, itemIndex) => itemIndex === index ? event.target.value : item))} /><Button type="button" variant="outline" size="icon" onClick={() => setDates((all) => all.filter((_, itemIndex) => itemIndex !== index))}><X className="size-4" /></Button></div>)}
                <Button type="button" variant="outline" className="w-full rounded-xl" onClick={() => setDates((all) => [...all, ""])}><Plus className="size-4" /> Agregar alternativa</Button>
              </div>
              <Button type="button" className="w-full rounded-xl" disabled={dates.length === 0 || dates.some((date) => !date)} onClick={() => void voting.updateOptions(dates.map((date) => `${date}:00`)).then(() => setDates([])).catch((error: unknown) => setAdminError(error instanceof Error ? error.message : "No se pudieron actualizar las opciones."))}>Reemplazar alternativas</Button>
              {adminError && <p className="text-xs font-extrabold" style={{ color: "var(--destructive)" }}>{adminError}</p>}
            </div>
          )}
        </div>
      )}
    </Card>
  );
}

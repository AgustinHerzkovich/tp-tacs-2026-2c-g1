import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { useState } from "react";
import { AlertTriangle, Plus, Settings2, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import type { UseVoting } from "@/hooks/useVoting";

interface VotingRoomProps {
  voting: UseVoting;
  warningText?: string;
  organizer?: boolean;
}

export function VotingRoom({ voting, warningText, organizer = false }: VotingRoomProps) {
  const { options, total, selectedId, votedId, select, requestVote, pending } = voting;
  const [editing, setEditing] = useState(false);
  const [dates, setDates] = useState<string[]>([]);
  const [quorum, setQuorum] = useState("50");
  const [duration, setDuration] = useState("24");
  const [adminError, setAdminError] = useState<string | null>(null);

  return (
    <Card className="p-4 mb-4 rounded-2xl border-2" style={{ borderColor: "#F7DE6B" }}>
      <div className="flex gap-2.5 rounded-xl p-3 mb-4 mx-4" style={{ background: "var(--sun)" }}>
        <AlertTriangle className="size-[18px] shrink-0 mt-0.5" style={{ color: "var(--sun-ink)" }} />
        <p className="text-[11.5px] font-extrabold leading-snug" style={{ color: "var(--sun-ink)" }}>
          {warningText ?? "El clima pronosticado no cumple las condiciones esperadas. Elegí una fecha alternativa para reprogramar."}
        </p>
      </div>

      <div className="flex items-center justify-between mb-3 px-4">
        <p className="font-display font-semibold text-[14.5px]">Sala de Votación</p>
        <Badge variant="secondary">{total} votos</Badge>
      </div>

      <div className="space-y-3 mb-4 px-4">
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

      <div className="px-4">
        <Button className="w-full h-auto py-3.5 rounded-2xl font-display font-semibold" disabled={pending || !selectedId || selectedId === votedId} onClick={requestVote}>
          {pending ? "Guardando voto..." : votedId ? "Cambiar voto" : "Votar fecha alternativa"}
        </Button>
      </div>

      {organizer && (
        <div className="mx-4 mt-4 pt-4 border-t-2" style={{ borderColor: "var(--border)" }}>
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

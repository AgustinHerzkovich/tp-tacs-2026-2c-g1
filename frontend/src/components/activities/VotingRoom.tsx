import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { AlertTriangle } from "lucide-react";
import type { UseVoting } from "@/hooks/useVoting";

export function VotingRoom({ voting }: { voting: UseVoting }) {
  const { options, total, selectedId, votedId, select, requestVote } = voting;

  return (
    <Card className="p-4 mb-4 rounded-2xl border-2" style={{ borderColor: "#F7DE6B" }}>
      <div className="flex gap-2.5 rounded-xl p-3 mb-4 mx-4" style={{ background: "var(--sun)" }}>
        <AlertTriangle className="size-[18px] shrink-0 mt-0.5" style={{ color: "var(--sun-ink)" }} />
        <p className="text-[11.5px] font-extrabold leading-snug" style={{ color: "var(--sun-ink)" }}>
          Se superó el máximo de lluvia permitido (40%). Elegí una fecha alternativa para reprogramar.
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
              disabled={!!votedId}
              className="tap block w-full text-left rounded-xl border-2 p-3 disabled:cursor-default"
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
        <Button className="w-full h-auto py-3.5 rounded-2xl font-display font-semibold" disabled={!selectedId || !!votedId} onClick={requestVote}>
          {votedId ? "¡Voto registrado! ✓" : "Votar fecha alternativa"}
        </Button>
      </div>
    </Card>
  );
}

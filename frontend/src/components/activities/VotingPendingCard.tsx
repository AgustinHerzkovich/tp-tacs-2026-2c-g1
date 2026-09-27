import Link from "next/link";
import { Card } from "@/components/ui/card";
import type { PendingVote } from "@/hooks/useMisActividades";

/** Highlighted card for a "still need your vote" activity — the one place
 * organized and joined activities mix together, so it carries its own
 * 👑/✋ role tag (the two regular feeds don't need one, the section header
 * already says which role it is). */
export function VotingPendingCard({ activity, rotate = -1 }: { activity: PendingVote; rotate?: number }) {
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card
        className="tap relative mb-3.5 lg:mb-0 lg:h-full overflow-visible gap-0 py-0 border-2 border-white"
        style={{
          background: "linear-gradient(160deg, #FFF6D6, #FEF08A)",
          borderRadius: "4px 20px 20px 20px",
          boxShadow: "0 8px 20px -10px rgba(58,51,82,.35)",
          transform: `rotate(${rotate}deg)`,
        }}
      >
        <span
          className="absolute -top-2.5 right-3.5 border-2 border-white rounded-full px-2.5 py-1 text-[9px] font-black"
          style={{
            background: activity.isOrganizer ? "var(--violet)" : "var(--mint)",
            color: activity.isOrganizer ? "var(--violet-ink)" : "var(--mint-ink)",
            boxShadow: "0 2px 0 rgba(0,0,0,.08)",
          }}
        >
          {activity.isOrganizer ? "👑 Organizás" : "✋ Sumado"}
        </span>
        <div className="p-4 pt-4.5">
          <div className="flex items-center gap-3">
            <span className="emoji-3d text-4xl shrink-0">🗳️</span>
            <div className="min-w-0 flex-1">
              <h3 className="font-display font-extrabold text-[15.5px] leading-tight truncate" style={{ color: "var(--sun-ink)" }}>
                {activity.title}
              </h3>
              <p className="text-[11.5px] font-extrabold mt-0.5" style={{ color: "var(--sun-ink)", opacity: 0.85 }}>
                🌧️ Mal pronóstico · elegí una fecha
              </p>
            </div>
            <span className="font-black text-[17px] shrink-0" style={{ color: "var(--sun-ink)" }}>
              →
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

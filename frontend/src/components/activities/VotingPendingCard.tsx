import Link from "next/link";
import { Card } from "@/components/ui/card";
import { PillBadge } from "@/components/common/PillBadge";
import type { Activity } from "@/types/domain";

export function VotingPendingCard({ activity }: { activity: Activity }) {
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card
        className="tap mb-3 lg:mb-0 lg:h-full overflow-hidden gap-0 py-0 rounded-2xl pulse-card border-2"
        style={{ borderColor: "#FFC9A8" }}
      >
        <div className="p-4" style={{ background: "linear-gradient(160deg, #FFF6D6, #FEF08A)" }}>
          <PillBadge bg="var(--rose-ink)" ink="#fff" className="mb-3">
            ⚡ Acción requerida: ¡Votá ahora!
          </PillBadge>
          <div className="flex items-center gap-3">
            <div className="text-4xl emoji-3d floaty">🗳️</div>
            <div className="min-w-0 flex-1">
              <h3 className="font-display font-semibold text-[16px] leading-tight" style={{ color: "var(--sun-ink)" }}>
                {activity.title}
              </h3>
              <p className="text-[12px] font-extrabold mt-0.5" style={{ color: "var(--sun-ink)" }}>
                🌧️ Mal pronóstico · elegí una nueva fecha
              </p>
            </div>
            <span className="font-display font-semibold text-[13px]" style={{ color: "var(--sun-ink)" }}>
              →
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

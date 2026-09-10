import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Scene } from "@/components/common/Scene";
import { TypeBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { PEOPLE } from "@/data/mockData";
import type { ExploreActivity } from "@/types/domain";

export function ExploreCard({ activity }: { activity: ExploreActivity }) {
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card className="tap mb-5 overflow-hidden gap-0 py-0 rounded-2xl">
        <div className="relative">
          <Scene scene={activity.scene} height={150} />
          <TypeBadge type={activity.type} className="absolute top-3 left-3" />
        </div>
        <div className="p-4">
          <h3 className="font-display font-semibold text-[16.5px] leading-snug">{activity.title}</h3>
          <p className="text-[12.5px] font-bold mt-1" style={{ color: "var(--muted-foreground)" }}>
            🗓️ {activity.when}
          </p>
          <p className="text-[12.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
            📍 {activity.where}
          </p>
          <div className="flex items-center justify-between mt-3">
            <AvatarStack names={PEOPLE.slice(0, 3)} extra={Math.max(0, activity.people - 3)} size="sm" />
            <span className="font-display font-semibold text-[13px]" style={{ color: "var(--primary)" }}>
              Ver actividad →
            </span>
          </div>
        </div>
      </Card>
    </Link>
  );
}

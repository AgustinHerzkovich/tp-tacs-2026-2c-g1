import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Scene } from "@/components/common/Scene";
import { StatusBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { TYPE_META, PEOPLE } from "@/data/mockData";
import type { MisActivity } from "@/types/domain";

export function MisCard({ activity }: { activity: MisActivity }) {
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card className="tap mb-4 overflow-hidden gap-0 py-0 rounded-2xl">
        <div className="flex gap-3.5 p-3.5">
          <Scene scene={activity.scene} height={84} className="w-[84px] shrink-0 rounded-2xl" />
          <div className="min-w-0 flex-1 py-0.5">
            <StatusBadge status={activity.status} className="mb-1.5" />
            <h3 className="font-display font-semibold text-[15px] leading-tight truncate">{activity.title}</h3>
            <p className="text-[11.5px] font-bold mt-0.5" style={{ color: "var(--muted-foreground)" }}>
              {TYPE_META[activity.type].icon} {activity.when}
            </p>
            <div className="flex items-center justify-between mt-1.5">
              <AvatarStack names={PEOPLE.slice(0, 2)} size="sm" />
              <span className="font-display font-semibold text-[12px]" style={{ color: "var(--muted-foreground)" }}>
                {activity.joined}/{activity.cap}
              </span>
            </div>
          </div>
        </div>
      </Card>
    </Link>
  );
}

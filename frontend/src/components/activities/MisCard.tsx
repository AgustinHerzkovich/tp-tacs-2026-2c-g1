"use client";

import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Scene } from "@/components/common/Scene";
import { StatusBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { TYPE_META } from "@/lib/activityVisuals";
import { useAuth } from "@/hooks/useAuth";
import type { MisActivity } from "@/types/domain";

export function MisCard({ activity, onRefreshImages }: { activity: MisActivity; onRefreshImages?: () => void }) {
  const { user } = useAuth();
  const names = activity.participantNames.slice(0, 2);
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card className="tap mb-4 overflow-hidden gap-0 py-0 transition-shadow lg:mb-0 lg:h-full lg:hover:shadow-lg lg:hover:shadow-[rgba(58,51,82,0.12)]">
        <div className="flex gap-3.5 p-3.5">
          <Scene scene={activity.scene} pattern={activity.pattern} imageUrl={activity.imageUrl} alt={activity.title} height={84} className="w-[84px] shrink-0 rounded-2xl" onRefresh={onRefreshImages} />
          <div className="min-w-0 flex-1 py-0.5">
            <StatusBadge status={activity.status} className="mb-1.5" />
            <h3 className="font-brand-title text-[15.5px] leading-tight truncate">{activity.title}</h3>
            <p className="text-[11.5px] font-bold mt-0.5" style={{ color: "var(--muted-foreground)" }}>
              {TYPE_META[activity.type].icon} {activity.when}
            </p>
            <div className="flex items-center justify-between mt-1.5">
              <AvatarStack names={names} size="sm" />
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

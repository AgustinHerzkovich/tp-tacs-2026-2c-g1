"use client";

import Link from "next/link";
import { Card } from "@/components/ui/card";
import { Scene } from "@/components/common/Scene";
import { TypeBadge, StatusBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { CardGoLink } from "@/components/common/CardGoLink";
import { useAuth } from "@/hooks/useAuth";
import type { ExploreActivity } from "@/types/domain";

export function ExploreCard({ activity, onRefreshImages }: { activity: ExploreActivity; onRefreshImages?: () => void }) {
  const { user } = useAuth();
  const names = activity.participantNames.slice(0, 3);
  return (
    <Link href={`/actividades/${activity.id}`}>
      <Card className="tap mb-5 flex flex-col overflow-hidden gap-0 py-0 transition-shadow lg:mb-0 lg:h-full lg:hover:shadow-lg lg:hover:shadow-[rgba(58,51,82,0.12)]">
        <div className="relative">
          <Scene scene={activity.scene} pattern={activity.pattern} imageUrl={activity.imageUrl} alt={activity.title} height={180} onRefresh={onRefreshImages} />
          <TypeBadge type={activity.type} className="absolute top-3 left-3" />
          <StatusBadge status={activity.status} className="absolute top-3 right-3" />
        </div>
        <div className="flex flex-1 flex-col p-4">
          <h3 className="font-brand-title text-[17px] leading-snug">{activity.title}</h3>
          <p className="text-[12.5px] font-bold mt-1 line-clamp-1" style={{ color: "var(--muted-foreground)" }}>
            🗓️ {activity.when}
          </p>
          <p className="text-[12.5px] font-bold line-clamp-1" style={{ color: "var(--muted-foreground)" }}>
            📍 {activity.where}
          </p>
          <div className="flex items-center justify-between mt-auto pt-3">
            <AvatarStack names={names} extra={Math.max(0, activity.people - names.length)} size="sm" />
            <CardGoLink />
          </div>
        </div>
      </Card>
    </Link>
  );
}

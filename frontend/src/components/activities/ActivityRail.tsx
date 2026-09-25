"use client";

import { StickerTag } from "@/components/common/StickerTag";
import { RailCard } from "@/components/activities/RailCard";
import type { Tone } from "@/lib/activityVisuals";
import type { ExploreActivity } from "@/types/domain";

interface ActivityRailProps {
  icon: string;
  label: string;
  tone: Tone;
  activities: ExploreActivity[];
  loading: boolean;
  /** Builds each card's secondary line from the activity (date for "Arranca
   * pronto", place for "Al aire libre"). */
  meta: (activity: ExploreActivity) => string;
  onViewAll: () => void;
}

/** Horizontal-scroll highlight row above Explorar's grid — see
 * ExplorarPage's `showRails` for when it's shown. Hidden while loading or
 * empty so it never flashes an empty shell; no arrows or page dots, the
 * next card peeking in from the edge is the only scroll affordance. */
export function ActivityRail({ icon, label, tone, activities, loading, meta, onViewAll }: ActivityRailProps) {
  if (loading || activities.length === 0) return null;

  return (
    <div className="mb-6">
      <div className="flex items-center gap-2 mb-3">
        <StickerTag tone={tone}>
          {icon} {label}
        </StickerTag>
        <button
          type="button"
          onClick={onViewAll}
          className="ml-auto text-[11.5px] font-extrabold whitespace-nowrap"
          style={{ color: "var(--primary)" }}
        >
          Ver todas →
        </button>
      </div>
      <div className="flex gap-3 overflow-x-auto scrollbar-none -mx-5 px-5 pb-2 lg:mx-0 lg:px-0">
        {activities.map((a) => (
          <RailCard key={a.id} activity={a} meta={meta(a)} />
        ))}
      </div>
    </div>
  );
}

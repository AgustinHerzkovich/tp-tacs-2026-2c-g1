"use client";

import Link from "next/link";
import { Scene } from "@/components/common/Scene";
import type { ExploreActivity } from "@/types/domain";

interface RailCardProps {
  activity: ExploreActivity;
  /** Secondary line under the title — each rail picks whichever detail
   * matters most for its theme (date for "Arranca pronto", place for "Al
   * aire libre"). */
  meta: string;
}

/** Compact card for a horizontal highlight rail (see ActivityRail) — same
 * visual language as ExploreCard, just narrow enough that a few sit side by
 * side with the next one peeking at the edge to hint the row scrolls. */
export function RailCard({ activity, meta }: RailCardProps) {
  return (
    <Link
      href={`/actividades/${activity.id}`}
      className="tap block w-[132px] shrink-0 overflow-hidden rounded-2xl bg-card ring-1 ring-foreground/5 shadow-[0_6px_16px_-10px_rgba(58,51,82,.22)]"
    >
      <Scene scene={activity.scene} pattern={activity.pattern} imageUrl={activity.imageUrl} alt={activity.title} height={84} />
      <div className="px-2.5 pt-2 pb-2.5">
        <h4 className="truncate text-xs font-extrabold leading-tight">{activity.title}</h4>
        <p className="mt-0.5 truncate text-[10px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          {meta}
        </p>
      </div>
    </Link>
  );
}

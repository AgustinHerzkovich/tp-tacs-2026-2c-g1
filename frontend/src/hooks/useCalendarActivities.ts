"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toMisActivity } from "@/lib/activityMapping";
import { activityDateKey, monthDateRange } from "@/lib/calendar";
import { useAuth } from "@/hooks/useAuth";
import type { MisActivity } from "@/types/domain";

/** Large enough that a single month of "mine" activities fits in one page —
 * the backend caps `size` at 100 (see ActivityController#pageRequest). A
 * month with more than 100 organized (or joined) activities isn't realistic
 * for this app, so no further pagination is needed here. */
const MONTH_PAGE_SIZE = 100;

interface UseCalendarActivities {
  /** Activities the user organizes or joined, grouped by `dateKey`, for the
   * given month only (see `monthDateRange`). An activity the organizer also
   * joined is kept only once, under `organizerId`. */
  activitiesByDay: Map<string, MisActivity[]>;
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

/** "Mis actividades" data for the calendar view: organized + joined
 * activities restricted to one visible month (`year`/`month0`), instead of
 * `useMisActividades`'s paginated feeds. See the module doc there for the
 * organizer/participant dedup rule, reused as-is here. */
export function useCalendarActivities(year: number, month0: number): UseCalendarActivities {
  const { user } = useAuth();
  const userId = user?.id ?? null;
  const [activitiesByDay, setActivitiesByDay] = useState<Map<string, MisActivity[]>>(new Map());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;
    const range = monthDateRange(year, month0);

    Promise.all([
      api.activities.organized(0, MONTH_PAGE_SIZE, range),
      api.activities.mine(0, MONTH_PAGE_SIZE, range),
    ])
      .then(([organized, joined]) => {
        if (cancelled) return;
        const joinedOnly = joined.content.filter((a) => a.organizerId !== userId);
        const activities = [...organized.content, ...joinedOnly].map(toMisActivity);

        const grouped = new Map<string, MisActivity[]>();
        for (const activity of activities) {
          const key = activityDateKey(activity.dateTime);
          grouped.set(key, [...(grouped.get(key) ?? []), activity]);
        }

        setActivitiesByDay(grouped);
        setError(null);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "No pudimos cargar tu calendario.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [year, month0, reloadKey, userId]);

  const refresh = useCallback(() => setReloadKey((k) => k + 1), []);

  return { activitiesByDay, loading, error, refresh };
}

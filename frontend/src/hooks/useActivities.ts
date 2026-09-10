"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toExploreActivity, toMisActivity } from "@/lib/activityMapping";
import type { ActivityResponse } from "@/types/backend";
import type { ExploreActivity, MisActivity } from "@/types/domain";

interface UseActivities {
  exploreFeed: ExploreActivity[];
  misFeed: MisActivity[];
  /** Own activities currently up for a reprogramming vote — shown in the
   * "Votaciones pendientes" section. */
  votingPending: MisActivity[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

/** Real activity feeds for Explorar and Mis Actividades, backed by the
 * backend through /api/activities/**. */
export function useActivities(): UseActivities {
  const [exploreFeed, setExploreFeed] = useState<ExploreActivity[]>([]);
  const [misFeed, setMisFeed] = useState<MisActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    // Reset happens once the fetch actually resolves (see .then below), not
    // synchronously here — an effect should only set state from a callback
    // reacting to the external system (the fetch), never as a direct,
    // synchronous statement in its body.
    Promise.all([api.activities.list(), api.activities.organized(), api.activities.mine()])
      .then(([all, organized, joined]) => {
        if (cancelled) return;
        setExploreFeed(all.map(toExploreActivity));

        const byId = new Map<string, ActivityResponse>();
        [...organized, ...joined].forEach((activity) => byId.set(activity.id, activity));
        setMisFeed(Array.from(byId.values()).map(toMisActivity));
        setError(null);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No pudimos cargar las actividades.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [reloadKey]);

  const refresh = useCallback(() => setReloadKey((k) => k + 1), []);

  return {
    exploreFeed,
    misFeed,
    votingPending: misFeed.filter((a) => a.status === "propuesta"),
    loading,
    error,
    refresh,
  };
}

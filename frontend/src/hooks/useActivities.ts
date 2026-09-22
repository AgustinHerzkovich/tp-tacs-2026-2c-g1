"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toExploreActivity, toMisActivity } from "@/lib/activityMapping";
import type { ActivityFilterParams, ActivityResponse } from "@/types/backend";
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
  explorePage: number;
  exploreTotalPages: number;
  setExplorePage: (page: number) => void;
  misPage: number;
  misTotalPages: number;
  setMisPage: (page: number) => void;
}

/** Real activity feeds for Explorar and Mis Actividades, backed by the
 * backend through /api/activities/**. */
export function useActivities(filters?: ActivityFilterParams): UseActivities {
  const type = filters?.type;
  const city = filters?.city;
  const dateFrom = filters?.dateFrom;
  const dateTo = filters?.dateTo;
  const availability = filters?.availability;
  const status = filters?.status;
  const [exploreFeed, setExploreFeed] = useState<ExploreActivity[]>([]);
  const [misFeed, setMisFeed] = useState<MisActivity[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadedRequest, setLoadedRequest] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [explorePage, setExplorePage] = useState(0);
  const [misPage, setMisPage] = useState(0);
  const [exploreTotalPages, setExploreTotalPages] = useState(0);
  const [misTotalPages, setMisTotalPages] = useState(0);
  const requestKey = [type, city, dateFrom, dateTo, availability, status, explorePage, misPage, reloadKey]
    .map((value) => String(value ?? ""))
    .join("|");
  const requestPending = loading || loadedRequest !== requestKey;

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      api.activities.list({ type, city, dateFrom, dateTo, availability, status, page: explorePage, size: 12 }),
      api.activities.organized(misPage, 12),
      api.activities.mine(misPage, 12),
    ])
      .then(([all, organized, joined]) => {
        if (cancelled) return;
        setExploreFeed(all.content.map(toExploreActivity));
        setExploreTotalPages(all.totalPages);

        const byId = new Map<string, ActivityResponse>();
        [...organized.content, ...joined.content].forEach((activity) => byId.set(activity.id, activity));
        setMisFeed(Array.from(byId.values()).map(toMisActivity));
        setMisTotalPages(Math.max(organized.totalPages, joined.totalPages));
        setError(null);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No pudimos cargar las actividades.");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
          setLoadedRequest(requestKey);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [availability, city, dateFrom, dateTo, explorePage, misPage, reloadKey, requestKey, type]);

  const refresh = useCallback(() => setReloadKey((k) => k + 1), []);

  return {
    exploreFeed,
    misFeed,
    votingPending: misFeed.filter((a) => a.status === "propuesta"),
    loading: requestPending,
    error,
    refresh,
    explorePage,
    exploreTotalPages,
    setExplorePage,
    misPage,
    misTotalPages,
    setMisPage,
  };
}

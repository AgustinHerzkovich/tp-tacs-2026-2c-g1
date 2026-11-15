"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toExploreActivity } from "@/lib/activityMapping";
import { initBrowserTracing } from "@/lib/telemetry/browserTracing";
import { ActivitySearchTrace } from "@/lib/telemetry/searchTrace";
import type { ActivityFilterParams } from "@/types/backend";
import type { ExploreActivity } from "@/types/domain";

interface UseActivities {
  exploreFeed: ExploreActivity[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
  explorePage: number;
  exploreTotalPages: number;
  setExplorePage: (page: number) => void;
}

/** Real Explorar feed, backed by the backend through GET /api/activities.
 * Mis Actividades (organized/joined) is a separate hook — see
 * useMisActividades.ts — since it needs a different fetch shape (two
 * independently paginated feeds plus voting status) and Explorar never
 * touched that data anyway.
 *
 * `enabled: false` skips the fetch entirely (no loading flicker, no
 * request) — used for Explorar's highlight rails, which only need to fetch
 * while they're actually shown on screen. */
export function useActivities(filters?: ActivityFilterParams, enabled = true): UseActivities {
  const type = filters?.type;
  const city = filters?.city;
  const title = filters?.title;
  const dateFrom = filters?.dateFrom;
  const dateTo = filters?.dateTo;
  const availability = filters?.availability;
  const status = filters?.status;
  const size = filters?.size ?? 12;
  const [exploreFeed, setExploreFeed] = useState<ExploreActivity[]>([]);
  const [loading, setLoading] = useState(enabled);
  const [loadedRequest, setLoadedRequest] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [explorePage, setExplorePage] = useState(0);
  const [exploreTotalPages, setExploreTotalPages] = useState(0);
  const requestKey = [type, title, city, dateFrom, dateTo, availability, status, size, explorePage, reloadKey]
    .map((value) => String(value ?? ""))
    .join("|");
  const requestPending = enabled && (loading || loadedRequest !== requestKey);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;

    // The whole search is one span, from the request until its result is in state.
    // Filters are recorded only as "used or not"; the searched text never leaves the browser.
    initBrowserTracing();
    const search = new ActivitySearchTrace({
      "search.filter.type": type ?? "any",
      "search.filter.text": Boolean(title),
      "search.filter.city": Boolean(city),
      "search.filter.dates": Boolean(dateFrom || dateTo),
      "search.filter.availability": availability !== undefined,
      "search.filter.status": Boolean(status),
      "search.page": explorePage,
      "search.page_size": size,
    });

    search
      .run(() =>
        api.activities.list({ type, title, city, dateFrom, dateTo, availability, status, page: explorePage, size }),
      )
      .then((all) => {
        if (cancelled) return;
        setExploreFeed(all.content.map(toExploreActivity));
        setExploreTotalPages(all.totalPages);
        setError(null);
        search.succeed(all.content.length);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No pudimos cargar las actividades.");
          search.fail(err);
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
      search.cancel();
    };
  }, [availability, city, dateFrom, dateTo, enabled, explorePage, reloadKey, requestKey, size, status, title, type]);

  const refresh = useCallback(() => setReloadKey((k) => k + 1), []);

  return {
    exploreFeed,
    loading: requestPending,
    error,
    refresh,
    explorePage,
    exploreTotalPages,
    setExplorePage,
  };
}

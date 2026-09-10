"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { ActivityResponse } from "@/types/backend";

interface UseActivity {
  activity: ActivityResponse | null;
  loading: boolean;
  notFound: boolean;
  error: string | null;
  refresh: () => void;
}

/** A single activity's real, full-fidelity data (used by the detail page,
 * which needs fields — participants, weather conditions, image URLs — that
 * don't survive the list-card mapping in src/lib/activityMapping.ts). */
export function useActivity(id: string): UseActivity {
  const [activity, setActivity] = useState<ActivityResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    api.activities
      .get(id)
      .then((dto) => {
        if (cancelled) return;
        setActivity(dto);
        setNotFound(false);
        setError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 404) {
          setNotFound(true);
        } else {
          setError(err instanceof Error ? err.message : "No pudimos cargar la actividad.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, reloadKey]);

  return { activity, loading, notFound, error, refresh: () => setReloadKey((k) => k + 1) };
}

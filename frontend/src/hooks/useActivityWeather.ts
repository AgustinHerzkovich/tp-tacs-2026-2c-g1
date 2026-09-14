"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { ActivityWeatherResponse } from "@/types/backend";

interface UseActivityWeather {
  weather: ActivityWeatherResponse | null;
  loading: boolean;
  /** True when the backend couldn't get a forecast (provider down, activity
   * too far in the future, etc.) — see the backend's circuit-breaker notes in
   * backend/README.md. Never treat this as "good weather". */
  unavailable: boolean;
}

/** Real forecast for one activity via GET /api/activities/:id/weather. This
 * hits Open-Meteo on the backend side, so it can be slow or briefly
 * unavailable — always show `unavailable` rather than guessing a fallback
 * value. */
export function useActivityWeather(activityId: string): UseActivityWeather {
  const [weather, setWeather] = useState<ActivityWeatherResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [unavailable, setUnavailable] = useState(false);

  useEffect(() => {
    let cancelled = false;

    api.activities
      .weather(activityId)
      .then((data) => {
        if (cancelled) return;
        setWeather(data);
        setUnavailable(false);
      })
      .catch(() => {
        if (!cancelled) setUnavailable(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [activityId]);

  return { weather, loading, unavailable };
}

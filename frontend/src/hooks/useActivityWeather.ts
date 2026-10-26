"use client";

import { useEffect, useState } from "react";
import { ApiError, api } from "@/lib/api";
import type { ActivityWeatherResponse } from "@/types/backend";

interface UseActivityWeather {
  weather: ActivityWeatherResponse | null;
  loading: boolean;
  /** True when there is no forecast to show. Never treat this as "good
   * weather". {@link tooEarly} says why. */
  unavailable: boolean;
  /** True when the forecast is missing only because the activity is still
   * beyond the provider's horizon (16 days), as opposed to a failed request. */
  tooEarly: boolean;
  refresh: () => void;
}

/** Real forecast for one activity via GET /api/activities/:id/weather. Any
 * signed-in user can read it, participant or not. This hits Open-Meteo on the
 * backend side, so it can be slow or briefly unavailable — always show
 * `unavailable` rather than guessing a fallback value.
 *
 * {@link refresh} re-requests it without flipping `loading`, so the widget
 * doesn't flash its skeleton. */
export function useActivityWeather(activityId: string): UseActivityWeather {
  const [weather, setWeather] = useState<ActivityWeatherResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [unavailable, setUnavailable] = useState(false);
  const [tooEarly, setTooEarly] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let cancelled = false;

    api.activities
      .weather(activityId)
      .then((data) => {
        if (cancelled) return;
        setWeather(data);
        setUnavailable(false);
        setTooEarly(false);
      })
      .catch((cause: unknown) => {
        if (cancelled) return;
        setUnavailable(true);
        setTooEarly(cause instanceof ApiError && cause.code === "FORECAST_NOT_YET_AVAILABLE");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [activityId, reloadKey]);

  return { weather, loading, unavailable, tooEarly, refresh: () => setReloadKey((key) => key + 1) };
}

"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { WeatherProvider } from "@/types/backend";

export interface UseWeatherProvider {
  /** The provider in effect for the current user, or null while it loads. */
  provider: WeatherProvider | null;
  saving: boolean;
  error: string | null;
  choose: (provider: WeatherProvider) => Promise<void>;
}

/** Reads and changes the current user's weather provider via
 * GET/PUT /api/users/me/preferences. The choice is stored in the backend, so
 * it applies to the forecast shown in the app and to the periodic monitoring
 * of the user's activities. Pass `enabled = false` to skip the request (for
 * example while the profile menu is closed). */
export function useWeatherProvider(enabled = true): UseWeatherProvider {
  const [provider, setProvider] = useState<WeatherProvider | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    api.users
      .getPreferences()
      .then((preferences) => {
        if (!cancelled) setProvider(preferences.weatherProvider);
      })
      .catch((cause: unknown) => {
        if (!cancelled) setError(cause instanceof Error ? cause.message : "No pudimos cargar tu configuración.");
      });
    return () => {
      cancelled = true;
    };
  }, [enabled]);

  const choose = useCallback(async (next: WeatherProvider) => {
    setSaving(true);
    setError(null);
    try {
      const preferences = await api.users.updatePreferences({ weatherProvider: next });
      setProvider(preferences.weatherProvider);
    } catch (cause: unknown) {
      setError(cause instanceof Error ? cause.message : "No pudimos guardar tu configuración.");
    } finally {
      setSaving(false);
    }
  }, []);

  return { provider, saving, error, choose };
}

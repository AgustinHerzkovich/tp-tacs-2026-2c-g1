import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useActivityWeather } from "@/hooks/useActivityWeather";
import type { ActivityWeatherResponse } from "@/types/backend";

const { weather } = vi.hoisted(() => ({ weather: vi.fn() }));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { weather },
  },
}));

const FORECAST: ActivityWeatherResponse = {
  activityId: "a1",
  location: { city: "CABA", latitude: -34.6, longitude: -58.4 },
  activityDateTime: "2026-09-20T14:00:00",
  currentWeather: { dateTime: "2026-09-13T12:00:00", temperature: 18, chanceOfRain: 20, windSpeed: 10 },
  activityForecast: { dateTime: "2026-09-20T14:00:00", temperature: 22, chanceOfRain: 40, windSpeed: 15 },
};

describe("useActivityWeather", () => {
  it("loads the forecast when the provider responds", async () => {
    weather.mockResolvedValueOnce(FORECAST);
    const { result } = renderHook(() => useActivityWeather("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.weather).toEqual(FORECAST);
    expect(result.current.unavailable).toBe(false);
  });

  it("marks the weather as unavailable instead of guessing when the provider fails (slow/timed out/down)", async () => {
    weather.mockRejectedValueOnce(new Error("provider timeout"));
    const { result } = renderHook(() => useActivityWeather("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.weather).toBeNull();
    expect(result.current.unavailable).toBe(true);
  });
});
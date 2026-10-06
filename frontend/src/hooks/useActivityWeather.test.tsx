import { act, renderHook, waitFor } from "@testing-library/react";
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

  it("re-requests the forecast on refresh, which is what joining does", async () => {
    // The backend answers 403 to a non-participant, so the first load lands on
    // `unavailable` and only a refresh (the page's join handler) can turn it
    // into a real forecast. Without it the widget stays empty until the page is
    // reopened.
    weather.mockRejectedValueOnce(new Error("403")).mockResolvedValueOnce(FORECAST);
    const { result } = renderHook(() => useActivityWeather("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.unavailable).toBe(true);

    act(() => result.current.refresh());

    await waitFor(() => expect(result.current.weather).toEqual(FORECAST));
    expect(result.current.unavailable).toBe(false);
    expect(weather).toHaveBeenCalledTimes(2);
  });

  it("keeps showing the previous forecast while refreshing", async () => {
    weather.mockResolvedValueOnce(FORECAST).mockResolvedValueOnce(FORECAST);
    const { result } = renderHook(() => useActivityWeather("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));

    act(() => result.current.refresh());

    await waitFor(() => expect(weather).toHaveBeenCalledTimes(2));
    expect(result.current.loading).toBe(false);
    expect(result.current.weather).toEqual(FORECAST);
  });
});
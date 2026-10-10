import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useActivityWeather } from "@/hooks/useActivityWeather";
import { ApiError } from "@/lib/api";
import type { ActivityWeatherResponse } from "@/types/backend";

const { weather } = vi.hoisted(() => ({ weather: vi.fn() }));

vi.mock("@/lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("@/lib/api")>()),
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
    expect(result.current.tooEarly).toBe(false);
  });

  it("flags too early only when the backend says the date is beyond the forecast horizon", async () => {
    weather.mockRejectedValueOnce(new ApiError(503, "Todavía es pronto", "FORECAST_NOT_YET_AVAILABLE"));
    const { result } = renderHook(() => useActivityWeather("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.unavailable).toBe(true);
    expect(result.current.tooEarly).toBe(true);
  });

  it("re-requests the forecast on refresh", async () => {
    // A failed first load lands on `unavailable`; a refresh can turn it into a
    // real forecast without reopening the page.
    weather.mockRejectedValueOnce(new Error("provider timeout")).mockResolvedValueOnce(FORECAST);
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
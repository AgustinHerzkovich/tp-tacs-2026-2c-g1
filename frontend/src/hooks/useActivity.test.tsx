import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useActivity } from "@/hooks/useActivity";
import type { ActivityResponse } from "@/types/backend";

const { get, MockApiError } = vi.hoisted(() => {
  class MockApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.name = "MockApiError";
      this.status = status;
    }
  }
  return { get: vi.fn(), MockApiError };
});

vi.mock("@/lib/api", () => ({
  ApiError: MockApiError,
  api: {
    activities: { get },
  },
}));

const ACTIVITY: ActivityResponse = {
  id: "a1",
  title: "Vóley",
  description: null,
  type: "OUTDOOR",
  location: { city: "CABA", latitude: null, longitude: null },
  dateTime: "2026-09-20T14:00:00",
  availability: true,
  minParticipants: 4,
  maxParticipants: 12,
  participantCount: 2,
  participants: [],
  weatherConditions: { maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null },
  anticipationWindow: 24,
  reprogramationRange: { maxDays: 3, initialHour: "09:00:00", finalHour: "21:00:00" },
  status: "CONFIRMED",
  imageUrls: [],
  organizerId: "organizer-1",
};

describe("useActivity", () => {
  it("loads a single activity at full fidelity", async () => {
    get.mockResolvedValueOnce(ACTIVITY);
    const { result } = renderHook(() => useActivity("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.activity).toEqual(ACTIVITY);
    expect(result.current.notFound).toBe(false);
    expect(result.current.error).toBeNull();
  });

  it("flags 404 responses as notFound", async () => {
    get.mockRejectedValueOnce(new MockApiError(404, "No existe"));
    const { result } = renderHook(() => useActivity("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.notFound).toBe(true);
    expect(result.current.error).toBeNull();
  });

  it("surfaces non-404 failures (backend down, 500, 503...)", async () => {
    get.mockRejectedValueOnce(new MockApiError(503, "Servicio no disponible"));
    const { result } = renderHook(() => useActivity("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.notFound).toBe(false);
    expect(result.current.error).toBe("Servicio no disponible");
  });

  it("refetches on refresh", async () => {
    get.mockResolvedValueOnce(ACTIVITY).mockResolvedValueOnce({ ...ACTIVITY, title: "Actualizada" });
    const { result } = renderHook(() => useActivity("a1"));
    await waitFor(() => expect(result.current.activity).not.toBeNull());

    act(() => result.current.refresh());
    await waitFor(() => expect(result.current.activity?.title).toBe("Actualizada"));
    expect(get).toHaveBeenCalledTimes(2);
  });
});
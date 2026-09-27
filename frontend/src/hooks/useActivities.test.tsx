import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useActivities } from "@/hooks/useActivities";
import type { ActivityResponse, PageResponse } from "@/types/backend";

const { list } = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { list },
  },
}));

function makeActivity(id: string, overrides: Partial<ActivityResponse> = {}): ActivityResponse {
  return {
    id,
    title: `Actividad ${id}`,
    description: null,
    type: "OUTDOOR",
    location: { city: "CABA", latitude: null, longitude: null },
    dateTime: "2026-09-20T14:00:00",
    availability: true,
    minParticipants: 4,
    maxParticipants: 12,
    participantCount: 5,
    participants: [{ userId: "u1", name: "Ana Pérez" }],
    weatherConditions: { maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null },
    anticipationWindow: 24,
    reprogramationRange: { maxDays: 3, initialHour: "09:00:00", finalHour: "21:00:00" },
    status: "CONFIRMED",
    imageUrls: [],
    organizerId: "organizer-1",
    ...overrides,
  };
}

function pageOf(...activities: ActivityResponse[]): PageResponse<ActivityResponse> {
  return {
    content: activities,
    page: 0,
    size: 12,
    totalElements: activities.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

describe("useActivities", () => {
  it("loads the Explorar feed", async () => {
    list.mockResolvedValueOnce(pageOf(makeActivity("a1")));

    const { result } = renderHook(() => useActivities());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.exploreFeed).toHaveLength(1);
    expect(result.current.exploreFeed[0]).toMatchObject({ id: "a1", title: "Actividad a1" });
  });

  it("forwards the exploration filters to the API", async () => {
    list.mockResolvedValueOnce(pageOf(makeActivity("a1")));

    renderHook(() =>
      useActivities({ type: "INDOOR", city: "CABA", availability: true, dateFrom: "2026-09-01T00:00:00" }),
    );
    await waitFor(() => expect(list).toHaveBeenCalled());
    expect(list).toHaveBeenCalledWith({
      type: "INDOOR",
      city: "CABA",
      dateFrom: "2026-09-01T00:00:00",
      dateTo: undefined,
      availability: true,
      page: 0,
      size: 12,
    });
  });

  it("pagination refetches with the requested page", async () => {
    list
      .mockResolvedValueOnce(pageOf(makeActivity("a1")))
      .mockResolvedValueOnce({ ...pageOf(makeActivity("a2")), page: 1 });

    const { result } = renderHook(() => useActivities());
    await waitFor(() => expect(result.current.exploreFeed).toHaveLength(1));

    act(() => result.current.setExplorePage(1));
    await waitFor(() => expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 })));
    await waitFor(() => expect(result.current.exploreFeed[0]?.id).toBe("a2"));
  });

  it("refresh re-requests the feed", async () => {
    list.mockResolvedValue(pageOf(makeActivity("a1")));

    const { result } = renderHook(() => useActivities());
    await waitFor(() => expect(list).toHaveBeenCalledTimes(1));

    act(() => result.current.refresh());
    await waitFor(() => expect(list).toHaveBeenCalledTimes(2));
  });

  it("skips the fetch while disabled, then fetches once enabled", async () => {
    list.mockResolvedValueOnce(pageOf(makeActivity("a1")));

    const { result, rerender } = renderHook(({ enabled }) => useActivities({ size: 8 }, enabled), {
      initialProps: { enabled: false },
    });

    expect(result.current.loading).toBe(false);
    expect(list).not.toHaveBeenCalled();

    rerender({ enabled: true });
    await waitFor(() => expect(list).toHaveBeenCalledWith(expect.objectContaining({ size: 8 })));
    await waitFor(() => expect(result.current.exploreFeed).toHaveLength(1));
  });

  it("surfaces load errors", async () => {
    list.mockRejectedValueOnce(new Error("No pudimos cargar las actividades."));

    const { result } = renderHook(() => useActivities());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("No pudimos cargar las actividades.");
    expect(result.current.exploreFeed).toEqual([]);
  });
});

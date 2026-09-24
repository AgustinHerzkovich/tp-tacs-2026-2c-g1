import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useMisActividades } from "@/hooks/useMisActividades";
import type { ActivityResponse, PageResponse, VotationDTO } from "@/types/backend";

const { organized, mine, get, votationsMine } = vi.hoisted(() => ({
  organized: vi.fn(),
  mine: vi.fn(),
  get: vi.fn(),
  votationsMine: vi.fn(),
}));

const { useAuthMock } = vi.hoisted(() => ({ useAuthMock: vi.fn() }));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { organized, mine, get },
    votations: { mine: votationsMine },
  },
}));

vi.mock("@/hooks/useAuth", () => ({
  useAuth: useAuthMock,
}));

const ME = "me-id";

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
    organizerId: "someone-else",
    ...overrides,
  };
}

function pageOf<T>(...items: T[]): PageResponse<T> {
  return {
    content: items,
    page: 0,
    size: 12,
    totalElements: items.length,
    totalPages: 1,
    first: true,
    last: true,
  };
}

function makeVotation(activityId: string): VotationDTO {
  return {
    id: `v-${activityId}`,
    activityId,
    creationDate: "2026-09-01T00:00:00",
    status: "ACTIVE",
    options: [{ dateTime: "2026-09-21T10:00:00", voteCount: 0, voterNames: [] }],
    votedOption: null,
  };
}

describe("useMisActividades", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthMock.mockReturnValue({ user: { id: ME, name: "Yo" } });
    votationsMine.mockResolvedValue(pageOf<VotationDTO>());
  });

  it("splits organized and joined into separate feeds", async () => {
    organized.mockResolvedValueOnce(pageOf(makeActivity("b1", { organizerId: ME })));
    mine.mockResolvedValueOnce(pageOf(makeActivity("c1")));

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.organizedFeed.map((a) => a.id)).toEqual(["b1"]);
    expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c1"]);
  });

  it("keeps an activity the user organizes and also joined only in the organized feed", async () => {
    organized.mockResolvedValueOnce(pageOf<ActivityResponse>());
    // b1 is organized by the user but not on the organized page loaded now:
    // organizerId still identifies it, no matter which pages are loaded.
    mine.mockResolvedValueOnce({
      ...pageOf(makeActivity("b1", { organizerId: ME }), makeActivity("c1")),
      totalElements: 2,
    });

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c1"]);
    expect(result.current.joinedTotal).toBe(1);
  });

  it("asks the backend for active votations the user has not voted in", async () => {
    organized.mockResolvedValueOnce(pageOf<ActivityResponse>());
    mine.mockResolvedValueOnce(pageOf<ActivityResponse>());

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(votationsMine).toHaveBeenCalledWith({ status: "ACTIVE", votedByMe: false, size: 20 });
  });

  it("shows a pending vote even when its activity is not on the loaded pages", async () => {
    organized.mockResolvedValueOnce(pageOf<ActivityResponse>());
    mine.mockResolvedValueOnce(pageOf<ActivityResponse>());
    votationsMine.mockResolvedValueOnce(pageOf(makeVotation("a1"), makeVotation("a2")));
    get.mockImplementation((id: string) =>
      Promise.resolve(makeActivity(id, { status: "PROPOSED", organizerId: id === "a1" ? ME : "someone-else" })),
    );

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(get).toHaveBeenCalledWith("a1");
    expect(result.current.votingPending.map((a) => [a.id, a.isOrganizer])).toEqual([
      ["a1", true],
      ["a2", false],
    ]);
  });

  it("skips pending votes whose activity is no longer PROPOSED or can't be loaded", async () => {
    organized.mockResolvedValueOnce(pageOf<ActivityResponse>());
    mine.mockResolvedValueOnce(pageOf<ActivityResponse>());
    votationsMine.mockResolvedValueOnce(pageOf(makeVotation("a1"), makeVotation("a2"), makeVotation("a3")));
    get.mockImplementation((id: string) => {
      if (id === "a2") return Promise.resolve(makeActivity(id, { status: "RESCHEDULED" }));
      if (id === "a3") return Promise.reject(new Error("No encontramos esta actividad."));
      return Promise.resolve(makeActivity(id, { status: "PROPOSED" }));
    });

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.votingPending.map((a) => a.id)).toEqual(["a1"]);
    expect(result.current.error).toBeNull();
  });

  it("paginates organized and joined feeds independently", async () => {
    organized.mockResolvedValue(pageOf(makeActivity("b1", { organizerId: ME })));
    mine
      .mockResolvedValueOnce(pageOf(makeActivity("c1")))
      .mockResolvedValueOnce({ ...pageOf(makeActivity("c2")), page: 1 });

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c1"]));

    act(() => result.current.setJoinedPage(1));
    await waitFor(() => expect(mine).toHaveBeenLastCalledWith(1, 12));
    await waitFor(() => expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c2"]));
    // organized wasn't touched by paginating the joined feed.
    expect(organized).toHaveBeenCalledTimes(2);
    expect(result.current.organizedFeed.map((a) => a.id)).toEqual(["b1"]);
  });

  it("surfaces load errors", async () => {
    organized.mockRejectedValueOnce(new Error("No pudimos cargar tus actividades."));
    mine.mockResolvedValueOnce(pageOf<ActivityResponse>());

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("No pudimos cargar tus actividades.");
  });
});

import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useMisActividades } from "@/hooks/useMisActividades";
import type { ActivityResponse, PageResponse, VotationDTO } from "@/types/backend";

const { organized, mine, votationsMine } = vi.hoisted(() => ({
  organized: vi.fn(),
  mine: vi.fn(),
  votationsMine: vi.fn(),
}));

const { useAuthMock } = vi.hoisted(() => ({ useAuthMock: vi.fn() }));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { organized, mine },
    votations: { mine: votationsMine },
  },
}));

vi.mock("@/hooks/useAuth", () => ({
  useAuth: useAuthMock,
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

function makeVotation(overrides: Partial<VotationDTO> = {}): VotationDTO {
  return {
    id: "v1",
    activityId: "a1",
    creationDate: "2026-09-01T00:00:00",
    status: "ACTIVE",
    options: [{ dateTime: "2026-09-21T10:00:00", voteCount: 1, voterNames: [] }],
    ...overrides,
  };
}

describe("useMisActividades", () => {
  beforeEach(() => {
    useAuthMock.mockReturnValue({ user: { id: "me-id", name: "Yo" } });
  });

  it("splits organized and joined into separate feeds", async () => {
    organized.mockResolvedValueOnce(pageOf(makeActivity("b1")));
    mine.mockResolvedValueOnce(pageOf(makeActivity("c1")));
    votationsMine.mockResolvedValueOnce([]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.organizedFeed.map((a) => a.id)).toEqual(["b1"]);
    expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c1"]);
  });

  it("joinedTotal reflects the deduped count, not the raw totalElements", async () => {
    organized.mockResolvedValueOnce(pageOf(makeActivity("b1")));
    // Both "joined" activities are also organized by the same user — the
    // dedup drops them from joinedFeed, and joinedTotal must follow suit
    // instead of showing the backend's raw count for /participants/me.
    mine.mockResolvedValueOnce({ ...pageOf(makeActivity("b1")), totalElements: 2 });
    votationsMine.mockResolvedValueOnce([]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.joinedFeed).toEqual([]);
    expect(result.current.joinedTotal).toBe(1);
  });

  it("counts an activity the user both organizes and joined as organized only", async () => {
    organized.mockResolvedValueOnce(pageOf(makeActivity("b1")));
    mine.mockResolvedValueOnce(pageOf(makeActivity("b1"), makeActivity("c1")));
    votationsMine.mockResolvedValueOnce([]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.organizedFeed.map((a) => a.id)).toEqual(["b1"]);
    expect(result.current.joinedFeed.map((a) => a.id)).toEqual(["c1"]);
  });

  it("includes a PROPOSED activity in votingPending only while its votation is ACTIVE and unvoted", async () => {
    organized.mockResolvedValueOnce(
      pageOf(
        makeActivity("a1", { status: "PROPOSED" }),
        makeActivity("a2", { status: "PROPOSED" }),
        makeActivity("a3", { status: "PROPOSED" }),
      ),
    );
    mine.mockResolvedValueOnce(pageOf());
    votationsMine.mockResolvedValueOnce([
      makeVotation({ activityId: "a1", status: "ACTIVE" }),
      makeVotation({ activityId: "a2", status: "CLOSED" }),
      makeVotation({
        activityId: "a3",
        status: "ACTIVE",
        options: [{ dateTime: "2026-09-21T10:00:00", voteCount: 1, voterNames: ["Yo"] }],
      }),
    ]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    // a2's votation is CLOSED and a3's is ACTIVE but the user already voted
    // ("Yo" is in voterNames) — only a1 should surface as pending.
    expect(result.current.votingPending.map((a) => a.id)).toEqual(["a1"]);
    expect(result.current.votingPending[0]?.isOrganizer).toBe(true);
  });

  it("marks a pending vote as not-organizer when it's a joined activity", async () => {
    organized.mockResolvedValueOnce(pageOf());
    mine.mockResolvedValueOnce(pageOf(makeActivity("a1", { status: "PROPOSED" })));
    votationsMine.mockResolvedValueOnce([makeVotation({ activityId: "a1", status: "ACTIVE" })]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.votingPending.map((a) => a.id)).toEqual(["a1"]);
    expect(result.current.votingPending[0]?.isOrganizer).toBe(false);
  });

  it("excludes a PROPOSED activity with no matching votation", async () => {
    organized.mockResolvedValueOnce(pageOf(makeActivity("a1", { status: "PROPOSED" })));
    mine.mockResolvedValueOnce(pageOf());
    votationsMine.mockResolvedValueOnce([]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.votingPending).toEqual([]);
  });

  it("paginates organized and joined feeds independently", async () => {
    organized.mockResolvedValue(pageOf(makeActivity("b1")));
    mine
      .mockResolvedValueOnce(pageOf(makeActivity("c1")))
      .mockResolvedValueOnce({ ...pageOf(makeActivity("c2")), page: 1 });
    votationsMine.mockResolvedValue([]);

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
    mine.mockResolvedValueOnce(pageOf());
    votationsMine.mockResolvedValueOnce([]);

    const { result } = renderHook(() => useMisActividades());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("No pudimos cargar tus actividades.");
  });
});

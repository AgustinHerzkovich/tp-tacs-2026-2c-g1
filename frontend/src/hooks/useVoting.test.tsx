import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useVoting } from "@/hooks/useVoting";
import type { PageResponse, VotationDTO } from "@/types/backend";

const { mine, vote, updateOptions, updateSettings } = vi.hoisted(() => ({
  mine: vi.fn(),
  vote: vi.fn(),
  updateOptions: vi.fn(),
  updateSettings: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  api: {
    votations: { mine, vote, updateOptions, updateSettings },
  },
}));

const VOTATION: VotationDTO = {
  id: "v1",
  activityId: "a1",
  creationDate: "2026-09-01T10:00:00",
  status: "ACTIVE",
  options: [
    { dateTime: "2026-09-20T18:00:00", voteCount: 3, voterNames: ["A"] },
    { dateTime: "2026-09-21T18:00:00", voteCount: 1, voterNames: ["B"] },
  ],
  votedOption: null,
};

function pageOf(...votations: VotationDTO[]): PageResponse<VotationDTO> {
  return {
    content: votations,
    page: 0,
    size: 1,
    totalElements: votations.length,
    totalPages: votations.length === 0 ? 0 : 1,
    first: true,
    last: true,
  };
}

describe("useVoting", () => {
  it("asks only for the latest votation of the activity and maps its options", async () => {
    mine.mockResolvedValueOnce(pageOf(VOTATION));
    const { result } = renderHook(() => useVoting("a1"));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(mine).toHaveBeenCalledWith({ activityId: "a1", size: 1 });
    expect(result.current.votation).toMatchObject({ id: "v1", activityId: "a1" });
    expect(result.current.options).toHaveLength(2);
    expect(result.current.options[0]).toMatchObject({ id: "2026-09-20T18:00:00", votes: 3 });
    expect(result.current.options[0]?.label).toContain("20 sep");
    expect(result.current.total).toBe(4);
    expect(result.current.error).toBeNull();
  });

  it("leaves the votation null when the activity has no votation for the user", async () => {
    mine.mockResolvedValueOnce(pageOf());
    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.votation).toBeNull();
    expect(result.current.options).toEqual([]);
    expect(result.current.votedId).toBeNull();
  });

  it("restores a previous vote from the backend's votedOption", async () => {
    mine.mockResolvedValueOnce(pageOf({ ...VOTATION, votedOption: "2026-09-20T18:00:00" }));
    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());
    expect(result.current.votedId).toBe("2026-09-20T18:00:00");
  });

  it("selects an option, submits the vote and takes votedId from the response", async () => {
    mine.mockResolvedValueOnce(pageOf(VOTATION));
    const updated: VotationDTO = {
      ...VOTATION,
      options: [{ dateTime: "2026-09-21T18:00:00", voteCount: 2, voterNames: ["B"] }],
      votedOption: "2026-09-21T18:00:00",
    };
    vote.mockResolvedValueOnce(updated);

    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());

    act(() => result.current.select("2026-09-21T18:00:00"));
    expect(result.current.selectedOption?.label).toBe(result.current.options[1]?.label);

    act(() => result.current.requestVote());
    expect(result.current.confirmOpen).toBe(true);

    await act(() => result.current.confirmVote());
    expect(vote).toHaveBeenCalledWith("v1", "2026-09-21T18:00:00");
    expect(result.current.votedId).toBe("2026-09-21T18:00:00");
    expect(result.current.confirmOpen).toBe(false);
    expect(result.current.votation?.options[0]?.voteCount).toBe(2);
  });

  it("does not submit a vote without a selection", async () => {
    mine.mockResolvedValueOnce(pageOf(VOTATION));
    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());

    act(() => result.current.requestVote());
    expect(result.current.confirmOpen).toBe(false);
    await act(async () => {
      await result.current.confirmVote().catch(() => undefined);
    });
    expect(vote).not.toHaveBeenCalled();
  });

  it("lets the organizer replace the options", async () => {
    mine.mockResolvedValueOnce(pageOf(VOTATION));
    const updated: VotationDTO = { ...VOTATION, options: [{ dateTime: "2026-09-24T18:00:00", voteCount: 0, voterNames: [] }] };
    updateOptions.mockResolvedValueOnce(updated);

    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());

    await act(() => result.current.updateOptions(["2026-09-24T18:00:00"]));
    expect(updateOptions).toHaveBeenCalledWith("v1", { dates: ["2026-09-24T18:00:00"] });
    expect(result.current.votation?.options).toHaveLength(1);
  });

  it("lets the organizer edit quorum and duration", async () => {
    mine.mockResolvedValueOnce(pageOf(VOTATION));
    updateSettings.mockResolvedValueOnce({ ...VOTATION });

    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());

    await act(() => result.current.updateSettings(5, 24));
    expect(updateSettings).toHaveBeenCalledWith("v1", { minQuorum: 5, duration: "PT24H" });
  });

  it("surfaces load errors", async () => {
    mine.mockRejectedValueOnce(new Error("No pudimos cargar la votación."));
    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("No pudimos cargar la votación.");
    expect(result.current.votation).toBeNull();
  });
});

import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useVoting } from "@/hooks/useVoting";
import type { VotationDTO } from "@/types/backend";

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
};

describe("useVoting", () => {
  it("loads the activity's votation and maps the options for display", async () => {
    mine.mockResolvedValueOnce([VOTATION, { ...VOTATION, id: "v2", activityId: "other" }]);
    const { result } = renderHook(() => useVoting("a1"));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.votation).toMatchObject({ id: "v1", activityId: "a1" });
    expect(result.current.options).toHaveLength(2);
    expect(result.current.options[0]).toMatchObject({ id: "2026-09-20T18:00:00", votes: 3 });
    expect(result.current.options[0]?.label).toContain("20 sep");
    expect(result.current.total).toBe(4);
    expect(result.current.error).toBeNull();
  });

  it("leaves the votation null when the activity is not among the user's votations", async () => {
    mine.mockResolvedValueOnce([
      { ...VOTATION, id: "v9", activityId: "another-activity" },
    ]);
    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.votation).toBeNull();
    expect(result.current.options).toEqual([]);
  });

  it("selects an option and submits the vote through the API", async () => {
    mine.mockResolvedValueOnce([VOTATION]);
    const updated: VotationDTO = { ...VOTATION, options: [{ dateTime: "2026-09-21T18:00:00", voteCount: 2, voterNames: ["B"] }] };
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
    mine.mockResolvedValueOnce([VOTATION]);
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
    mine.mockResolvedValueOnce([VOTATION]);
    const updated: VotationDTO = { ...VOTATION, options: [{ dateTime: "2026-09-24T18:00:00", voteCount: 0, voterNames: [] }] };
    updateOptions.mockResolvedValueOnce(updated);

    const { result } = renderHook(() => useVoting("a1"));
    await waitFor(() => expect(result.current.votation).not.toBeNull());

    await act(() => result.current.updateOptions(["2026-09-24T18:00:00"]));
    expect(updateOptions).toHaveBeenCalledWith("v1", { dates: ["2026-09-24T18:00:00"] });
    expect(result.current.votation?.options).toHaveLength(1);
  });

  it("lets the organizer edit quorum and duration", async () => {
    mine.mockResolvedValueOnce([VOTATION]);
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
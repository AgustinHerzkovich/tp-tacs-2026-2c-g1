import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useJoinActivity } from "@/hooks/useJoinActivity";

const { join, leave } = vi.hoisted(() => ({
  join: vi.fn<() => Promise<void>>(),
  leave: vi.fn<() => Promise<void>>(),
}));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { join, leave },
  },
}));

describe("useJoinActivity", () => {
  it("starts reflecting the activity's joined state", () => {
    const { result } = renderHook(() => useJoinActivity("a1", false));
    expect(result.current.joined).toBe(false);
    expect(result.current.pending).toBe(false);
  });

  it("tracks the activity's joined state when it loads asynchronously", () => {
    const { rerender, result } = renderHook(({ joined }) => useJoinActivity("a1", joined), {
      initialProps: { joined: false },
    });
    rerender({ joined: true });
    expect(result.current.joined).toBe(true);
  });

  it("joins with confirmation and notifies the parent", async () => {
    join.mockResolvedValueOnce();
    const onChanged = vi.fn();
    const { result } = renderHook(() => useJoinActivity("a1", false, onChanged));

    act(() => result.current.requestJoin());
    expect(result.current.confirmOpen).toBe(true);

    await act(() => result.current.confirmJoin());
    expect(join).toHaveBeenCalledWith("a1");
    expect(result.current.joined).toBe(true);
    expect(result.current.confirmOpen).toBe(false);
    expect(result.current.pending).toBe(false);
    expect(onChanged).toHaveBeenCalledTimes(1);
  });

  it("propagates join errors without flipping the state", async () => {
    join.mockRejectedValueOnce(new Error("server down"));
    const { result } = renderHook(() => useJoinActivity("a1", false));

    await expect(act(() => result.current.confirmJoin())).rejects.toThrow("server down");
    expect(result.current.joined).toBe(false);
    expect(result.current.confirmOpen).toBe(false);
    expect(result.current.pending).toBe(false);
  });

  it("leaves with confirmation and notifies the parent", async () => {
    leave.mockResolvedValueOnce();
    const onChanged = vi.fn();
    const { result } = renderHook(() => useJoinActivity("a1", true, onChanged));

    act(() => result.current.requestLeave());
    expect(result.current.leaveConfirmOpen).toBe(true);

    await act(() => result.current.confirmLeave());
    expect(leave).toHaveBeenCalledWith("a1");
    expect(result.current.joined).toBe(false);
    expect(onChanged).toHaveBeenCalledTimes(1);
  });

  it("propagates leave errors without flipping the state", async () => {
    leave.mockRejectedValueOnce(new Error("conflict"));
    const { result } = renderHook(() => useJoinActivity("a1", true));

    await expect(act(() => result.current.confirmLeave())).rejects.toThrow("conflict");
    expect(result.current.joined).toBe(true);
  });

  it("cancels without side effects", async () => {
    const { result } = renderHook(() => useJoinActivity("a1", false));
    act(() => {
      result.current.requestJoin();
      result.current.cancelJoin();
    });
    expect(result.current.confirmOpen).toBe(false);
    await waitFor(() => expect(join).not.toHaveBeenCalled());
  });
});
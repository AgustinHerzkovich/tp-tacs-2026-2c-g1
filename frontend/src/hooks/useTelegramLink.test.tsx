import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { parseTelegramChatId, useTelegramLink } from "@/hooks/useTelegramLink";

const { linkTelegram } = vi.hoisted(() => ({
  linkTelegram: vi.fn<(chatId: number) => Promise<void>>(),
}));

vi.mock("@/lib/api", () => ({
  api: {
    users: { linkTelegram },
  },
}));

describe("parseTelegramChatId", () => {
  it("accepts integer chat ids, including negative group ids", () => {
    expect(parseTelegramChatId("1279457429")).toBe(1279457429);
    expect(parseTelegramChatId("-100123")).toBe(-100123);
  });

  it("rejects missing or non-integer values", () => {
    expect(parseTelegramChatId(null)).toBeNull();
    expect(parseTelegramChatId("")).toBeNull();
    expect(parseTelegramChatId("12a")).toBeNull();
    expect(parseTelegramChatId("1279457429 ")).toBeNull();
    expect(parseTelegramChatId("99999999999999999999")).toBeNull();
  });
});

describe("useTelegramLink", () => {
  beforeEach(() => linkTelegram.mockReset());

  it("waits until the user is authenticated", () => {
    const { result } = renderHook(() => useTelegramLink(123, false));
    expect(result.current.status).toBe("idle");
    expect(linkTelegram).not.toHaveBeenCalled();
  });

  it("links the chat once when authenticated", async () => {
    linkTelegram.mockResolvedValue();
    const { result, rerender } = renderHook(({ enabled }) => useTelegramLink(123, enabled), {
      initialProps: { enabled: true },
    });

    await waitFor(() => expect(result.current.status).toBe("linked"));
    rerender({ enabled: true });
    expect(linkTelegram).toHaveBeenCalledTimes(1);
    expect(linkTelegram).toHaveBeenCalledWith(123);
  });

  it("does nothing without a chat id", () => {
    renderHook(() => useTelegramLink(null, true));
    expect(linkTelegram).not.toHaveBeenCalled();
  });

  it("reports errors and retries on demand", async () => {
    linkTelegram.mockRejectedValueOnce(new Error("boom")).mockResolvedValueOnce();
    const { result } = renderHook(() => useTelegramLink(123, true));

    await waitFor(() => expect(result.current.status).toBe("error"));
    act(() => result.current.retry());
    await waitFor(() => expect(result.current.status).toBe("linked"));
    expect(linkTelegram).toHaveBeenCalledTimes(2);
  });
});

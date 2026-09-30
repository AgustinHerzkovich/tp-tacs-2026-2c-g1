import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { parseTelegramLinkCode, useTelegramLink } from "@/hooks/useTelegramLink";

const { linkTelegram, MockApiError } = vi.hoisted(() => {
  class MockApiError extends Error {
    constructor(
      public status: number,
      message: string,
      public code: string | null = null,
    ) {
      super(message);
    }
  }
  return { linkTelegram: vi.fn<(code: string) => Promise<void>>(), MockApiError };
});

vi.mock("@/lib/api", () => ({
  api: {
    users: { linkTelegram },
  },
  ApiError: MockApiError,
}));

const CODE = "Xb7k2mQ9_pL4vN8rT1wYz3Hc6Fj0Ds5Ae-GuKoPiWqM";

describe("parseTelegramLinkCode", () => {
  it("accepts the URL-safe codes issued by the backend", () => {
    expect(parseTelegramLinkCode(CODE)).toBe(CODE);
  });

  it("rejects missing values, raw chat ids and anything with other characters", () => {
    expect(parseTelegramLinkCode(null)).toBeNull();
    expect(parseTelegramLinkCode("")).toBeNull();
    expect(parseTelegramLinkCode("1279457429")).toBeNull();
    expect(parseTelegramLinkCode(`${CODE}<script>`)).toBeNull();
  });
});

describe("useTelegramLink", () => {
  beforeEach(() => linkTelegram.mockReset());

  it("does not link anything until the user confirms", () => {
    const { result } = renderHook(() => useTelegramLink(CODE));
    expect(result.current.status).toBe("idle");
    expect(linkTelegram).not.toHaveBeenCalled();
  });

  it("redeems the code once when the user confirms", async () => {
    linkTelegram.mockResolvedValue();
    const { result } = renderHook(() => useTelegramLink(CODE));

    act(() => result.current.link());
    await waitFor(() => expect(result.current.status).toBe("linked"));
    act(() => result.current.link());

    expect(linkTelegram).toHaveBeenCalledTimes(1);
    expect(linkTelegram).toHaveBeenCalledWith(CODE);
  });

  it("does nothing without a code", () => {
    const { result } = renderHook(() => useTelegramLink(null));
    act(() => result.current.link());
    expect(linkTelegram).not.toHaveBeenCalled();
  });

  it("allows retrying after a transient error", async () => {
    linkTelegram.mockRejectedValueOnce(new MockApiError(503, "No pudimos completar la operación.")).mockResolvedValueOnce();
    const { result } = renderHook(() => useTelegramLink(CODE));

    act(() => result.current.link());
    await waitFor(() => expect(result.current.status).toBe("error"));
    expect(result.current.canRetry).toBe(true);

    act(() => result.current.link());
    await waitFor(() => expect(result.current.status).toBe("linked"));
    expect(linkTelegram).toHaveBeenCalledTimes(2);
  });

  it("explains that an expired or used code needs a new link and offers no retry", async () => {
    linkTelegram.mockRejectedValueOnce(
      new MockApiError(400, "El link para vincular Telegram venció o ya se usó.", "TELEGRAM_LINK_CODE_INVALID"),
    );
    const { result } = renderHook(() => useTelegramLink(CODE));

    act(() => result.current.link());
    await waitFor(() => expect(result.current.status).toBe("error"));

    expect(result.current.canRetry).toBe(false);
    expect(result.current.error).toBe("El link para vincular Telegram venció o ya se usó.");
  });
});

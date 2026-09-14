import { act, renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useNotifications } from "@/hooks/useNotifications";
import type { NotificationResponse, PageResponse } from "@/types/backend";

const { list, markRead } = vi.hoisted(() => ({
  list: vi.fn(),
  markRead: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  api: {
    notifications: { list, markRead },
  },
}));

beforeEach(() => {
  list.mockReset();
  markRead.mockReset();
});

function pageOf(content: NotificationResponse[], totalPages = 1): PageResponse<NotificationResponse> {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages, first: true, last: true };
}

const N1: NotificationResponse = {
  id: "n1",
  activityId: "a1",
  type: "BAD_WEATHER_ALERT",
  title: "Clima",
  message: "Lluvia probable",
  createdAt: "2026-09-13T11:48:00",
};
const N2: NotificationResponse = {
  id: "n2",
  activityId: "a2",
  type: "REPROGRAMMED",
  title: "Reprogramada",
  message: "Paso al sábado",
  createdAt: "2026-09-13T10:00:00",
};

describe("useNotifications", () => {
  it("loads and maps unread notifications, deriving the counter from the page", async () => {
    list.mockResolvedValueOnce(pageOf([N1, N2]));
    const { result } = renderHook(() => useNotifications());

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(list).toHaveBeenCalledWith(0);
    expect(result.current.notifications).toHaveLength(2);
    expect(result.current.notifications[0]).toMatchObject({
      id: "n1",
      activityId: "a1",
      kind: "warn",
      icon: "🌧️",
      title: "Clima",
      body: "Lluvia probable",
    });
    expect(typeof result.current.notifications[0]?.time).toBe("string");
    expect(result.current.notifications[1]?.kind).toBe("reprog");
    expect(result.current.unreadCount).toBe(2);
    expect(result.current.totalPages).toBe(1);
  });

  it("marks a notification as read: removes it and decrements the counter", async () => {
    list.mockResolvedValueOnce(pageOf([N1, N2])).mockResolvedValueOnce(pageOf([N2]));
    markRead.mockResolvedValueOnce({ ...N1 });

    const { result } = renderHook(() => useNotifications());
    await waitFor(() => expect(result.current.notifications).toHaveLength(2));

    await act(() => result.current.markRead("n1"));
    expect(markRead).toHaveBeenCalledWith("n1");
    expect(result.current.notifications).toHaveLength(1);
    expect(result.current.notifications[0]?.id).toBe("n2");
    expect(result.current.unreadCount).toBe(1);
  });

  it("moves to the previous page when the last item of a page is read", async () => {
    list
      .mockResolvedValueOnce(pageOf([N1], 2))
      .mockResolvedValueOnce(pageOf([N2], 2))
      .mockResolvedValueOnce(pageOf([N1], 2));
    markRead.mockResolvedValueOnce({ ...N2 });

    const { result } = renderHook(() => useNotifications());
    await waitFor(() => expect(result.current.notifications).toHaveLength(1));

    act(() => result.current.setPage(1));
    await waitFor(() => expect(list).toHaveBeenLastCalledWith(1));
    await waitFor(() => expect(result.current.notifications[0]?.id).toBe("n2"));

    await act(() => result.current.markRead("n2"));
    expect(markRead).toHaveBeenCalledWith("n2");
    await waitFor(() => expect(list).toHaveBeenLastCalledWith(0));
    expect(result.current.notifications[0]?.id).toBe("n1");
  });

  it("surfaces load errors", async () => {
    list.mockRejectedValueOnce(new Error("No pudimos cargar las notificaciones."));
    const { result } = renderHook(() => useNotifications());
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe("No pudimos cargar las notificaciones.");
  });
});
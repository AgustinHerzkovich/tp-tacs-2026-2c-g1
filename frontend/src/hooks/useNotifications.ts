"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatRelativeTime } from "@/lib/formatDate";
import { iconForNotificationKind, mapNotificationKind } from "@/lib/notificationMapping";
import type { NotificationKind } from "@/types/domain";

export interface NotificationView {
  id: string;
  activityId: string;
  kind: NotificationKind;
  icon: string;
  title: string;
  body: string;
  time: string;
  read: boolean;
}

interface UseNotifications {
  /** GET /notifications returns all notifications, with unread ones first. */
  notifications: NotificationView[];
  unreadCount: number;
  /** Total notifications across all pages (`totalElements`), for the
   * drawer's "X sin leer · Y en total" header — distinct from `page.length`. */
  total: number;
  loading: boolean;
  error: string | null;
  markRead: (id: string) => Promise<void>;
  page: number;
  totalPages: number;
  setPage: (page: number) => void;
}

/** Real notification feed via GET /api/notifications. */
export function useNotifications(): UseNotifications {
  const [notifications, setNotifications] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [unreadCount, setUnreadCount] = useState(0);
  const [total, setTotal] = useState(0);

  const load = useCallback(() => {
    api.notifications
      .list(page)
      .then((all) => {
        setNotifications(
          all.content.map((n) => {
            const kind = mapNotificationKind(n.type);
            return {
              id: n.id,
              activityId: n.activityId,
              kind,
              icon: iconForNotificationKind(kind),
              title: n.title,
              body: n.message,
              time: formatRelativeTime(n.createdAt),
              read: n.read,
            };
          }),
        );
        setTotalPages(all.totalPages);
        setTotal(all.totalElements);
        setUnreadCount(all.content.filter((notification) => !notification.read).length);
        setError(null);
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "No pudimos cargar las notificaciones."))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  const markRead = async (id: string) => {
    await api.notifications.markRead(id);
    setNotifications((all) => all.filter((n) => n.id !== id));
    setUnreadCount((count) => Math.max(0, count - 1));
    if (notifications.length === 1 && page > 0) setPage(page - 1);
    else load();
  };

  return { notifications, unreadCount, total, loading, error, markRead, page, totalPages, setPage };
}

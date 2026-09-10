"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { formatRelativeTime } from "@/lib/formatDate";
import { iconForNotificationKind, mapNotificationKind } from "@/lib/notificationMapping";
import type { NotificationKind } from "@/types/domain";

export interface NotificationView {
  id: string;
  kind: NotificationKind;
  icon: string;
  title: string;
  body: string;
  time: string;
}

interface UseNotifications {
  /** GET /notifications only ever returns the user's UNREAD notifications
   * (see backend's NotificationService.getNotificationsByUser) — there is no
   * "history" of already-read ones, so every item here is unread by
   * definition and markRead removes it from this list rather than flagging
   * it, matching what a refetch would return. */
  notifications: NotificationView[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
  markRead: (id: string) => Promise<void>;
}

/** Real notification feed via GET /api/notifications. */
export function useNotifications(): UseNotifications {
  const [notifications, setNotifications] = useState<NotificationView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api.notifications
      .list()
      .then((all) => {
        setNotifications(
          all.map((n) => {
            const kind = mapNotificationKind(n.type);
            return {
              id: n.id,
              kind,
              icon: iconForNotificationKind(kind),
              title: n.title,
              body: n.message,
              time: formatRelativeTime(n.createdAt),
            };
          }),
        );
        setError(null);
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "No pudimos cargar las notificaciones."))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const markRead = async (id: string) => {
    await api.notifications.markRead(id);
    setNotifications((all) => all.filter((n) => n.id !== id));
  };

  return { notifications, unreadCount: notifications.length, loading, error, markRead };
}

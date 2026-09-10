"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { NotificationResponse } from "@/types/backend";

interface UseNotifications {
  notifications: NotificationResponse[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
}

/** Real notification feed via GET /api/notifications. The backend's
 * `type` field is a free-form string (see NotificationResponse) — the UI
 * maps it to a color-coded kind in NotifDrawer, defaulting to "info" for
 * anything it doesn't recognize yet. */
export function useNotifications(): UseNotifications {
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api.notifications
      .list()
      .then((list) => {
        if (!cancelled) setNotifications(list);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "No pudimos cargar las notificaciones.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { notifications, unreadCount: notifications.length, loading, error };
}

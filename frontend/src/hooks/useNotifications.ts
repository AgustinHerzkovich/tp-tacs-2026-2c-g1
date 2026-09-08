import { NOTIFS } from "@/data/mockData";
import type { NotificationItem } from "@/types/domain";

interface UseNotifications {
  notifications: NotificationItem[];
  unreadCount: number;
}

/** Read access to the user's notification feed. Mock-backed for now; will
 * call GET /api/notifications once real auth/login exists. */
export function useNotifications(): UseNotifications {
  return { notifications: NOTIFS, unreadCount: NOTIFS.length };
}

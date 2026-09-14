// Maps the backend's real notification `type` codes (see
// backend/src/main/java/com/solnotfound/entity/notification/*NotificationType.java)
// to the UI's presentation-only NotificationKind (icon/color bucket).

import type { NotificationKind } from "@/types/domain";

export function mapNotificationKind(type: string): NotificationKind {
  switch (type) {
    case "BAD_WEATHER_ALERT":
      return "warn";
    case "CANCELLED":
      return "cancel";
    case "REPROGRAMMED":
      return "reprog";
    case "STARTED":
    case "STARTING_SOON":
      return "info";
    default:
      return "info";
  }
}

export function iconForNotificationKind(kind: NotificationKind): string {
  switch (kind) {
    case "warn":
      return "🌧️";
    case "cancel":
      return "🚫";
    case "reprog":
      return "🔁";
    case "info":
      return "⏰";
    default:
      return "🔔";
  }
}

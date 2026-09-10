import type { NotificationKind } from "@/types/domain";

/** Backend notification `type` codes (see the `NotificationType` implementations
 * under backend/src/main/java/com/solnotfound/entity/notification) mapped to the
 * drawer's color-coded kind. Anything unrecognized falls back to "info". */
export function mapNotificationKind(type: string): NotificationKind {
  switch (type) {
    case "BAD_WEATHER_ALERT":
      return "warn";
    case "REPROGRAMMED":
      return "reprog";
    case "CANCELLED":
      return "cancel";
    case "STARTING_SOON":
    case "STARTED":
    default:
      return "info";
  }
}

const ICONS: Record<NotificationKind, string> = {
  warn: "🌧️",
  info: "⏰",
  reprog: "🔁",
  cancel: "🚫",
};

export function iconForNotificationKind(kind: NotificationKind): string {
  return ICONS[kind];
}

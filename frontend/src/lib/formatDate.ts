const WEEKDAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MONTHS = [
  "ene",
  "feb",
  "mar",
  "abr",
  "may",
  "jun",
  "jul",
  "ago",
  "sep",
  "oct",
  "nov",
  "dic",
];

/** Formats a backend `LocalDateTime` string (no timezone — treated as local
 * wall-clock time, matching how the backend stores it) into the app's short
 * display style, e.g. "Sáb 5 sep · 9:00". Prefixes "era " for past dates,
 * matching how the UI flags activities that already happened. */
export function formatActivityWhen(isoLocalDateTime: string): string {
  const date = new Date(isoLocalDateTime);
  const weekday = WEEKDAYS[date.getDay()];
  const day = date.getDate();
  const month = MONTHS[date.getMonth()];
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const prefix = date.getTime() < Date.now() ? "era " : "";
  return `${prefix}${weekday} ${day} ${month} · ${hours}:${minutes}`;
}

/** Formats a past ISO timestamp as a short relative string ("hace 12 min",
 * "ayer"), matching the notification drawer's mock copy style. */
export function formatRelativeTime(isoDateTime: string): string {
  const date = new Date(isoDateTime);
  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.round(diffMs / 60000);

  if (diffMinutes < 1) return "recién";
  if (diffMinutes < 60) return `hace ${diffMinutes} min`;
  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) return `hace ${diffHours} h`;
  const diffDays = Math.round(diffHours / 24);
  if (diffDays === 1) return "ayer";
  return `hace ${diffDays} días`;
}

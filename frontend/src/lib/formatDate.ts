// Formats the backend's ISO LocalDateTime strings (e.g. "2026-09-15T14:00:00")
// for display. These are naive local date-times (no timezone/offset), so we
// parse the components directly instead of going through `new Date(iso)` —
// that would apply the browser's local timezone on top of a value the
// backend never attached one to.

const WEEKDAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MONTHS = [
  "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
];

function parseLocalDateTime(iso: string): Date {
  const [datePart, timePart = "00:00:00"] = iso.split("T");
  const [year, month, day] = (datePart ?? "").split("-").map(Number);
  const [hour = 0, minute = 0] = timePart.split(":").map(Number);
  return new Date(year ?? 1970, (month ?? 1) - 1, day ?? 1, hour, minute);
}

/** "Mar 15 sep · 14:00" */
export function formatActivityWhen(isoLocalDateTime: string): string {
  const d = parseLocalDateTime(isoLocalDateTime);
  const weekday = WEEKDAYS[d.getDay()];
  const month = MONTHS[d.getMonth()];
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${weekday} ${d.getDate()} ${month} · ${hh}:${mm}`;
}

/** "hace 12 min" / "hace 3 h" / "ayer" / "hace 5 d" — for notification
 * timestamps, which the backend sends as real Instant/LocalDateTime values. */
export function formatRelativeTime(isoDateTime: string): string {
  const then = parseLocalDateTime(isoDateTime).getTime();
  const diffMs = Date.now() - then;
  const minutes = Math.floor(diffMs / 60_000);

  if (minutes < 1) return "recién";
  if (minutes < 60) return `hace ${minutes} min`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `hace ${hours} h`;

  const days = Math.floor(hours / 24);
  if (days === 1) return "ayer";
  return `hace ${days} d`;
}

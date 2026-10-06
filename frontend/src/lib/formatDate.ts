// Formats the backend's ISO LocalDateTime strings (e.g. "2026-09-15T14:00:00")
// for display. These are naive local date-times (no timezone/offset), so we
// parse the components directly instead of going through `new Date(iso)` —
// that would apply the browser's local timezone on top of a value the
// backend never attached one to.

const WEEKDAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MONTHS = [
  "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
];

export function parseLocalDateTime(iso: string): Date {
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

/** True when the backend's naive local date-time is already behind the clock —
 *  the activity happened (or is happening right now). A weather provider only
 *  forecasts the future, so this is what tells the UI to stop talking about a
 *  pending forecast.
 *
 *  `timeZone` is the activity's own zone (the backend stores the one the creator
 *  was in). Without it the browser's zone is assumed, which is right only while
 *  every activity belongs to the same place as the reader. */
export function isPastLocalDateTime(isoLocalDateTime: string, timeZone?: string | null): boolean {
  return instantOfLocalDateTime(isoLocalDateTime, timeZone).getTime() < Date.now();
}

/** Whole hours from now until a naive local date-time — negative once it passed.
 * Same zone rules as {@link isPastLocalDateTime}. */
export function hoursUntilLocalDateTime(
  isoLocalDateTime: string,
  timeZone?: string | null,
): number {
  return (instantOfLocalDateTime(isoLocalDateTime, timeZone).getTime() - Date.now()) / 3_600_000;
}

/** The instant a wall-clock reading stands for.
 *
 * The backend sends naive local date-times: "2026-09-15T14:00:00" means 14:00
 * *in the activity's zone*, with no offset attached. Turning that into an instant
 * therefore needs that zone — without one the browser's own zone is the best guess,
 * which is what the data meant before zones were recorded. */
export function instantOfLocalDateTime(iso: string, timeZone?: string | null): Date {
  if (!timeZone) return parseLocalDateTime(iso);

  const [datePart, timePart = "00:00:00"] = iso.split("T");
  const [year, month, day] = (datePart ?? "").split("-").map(Number);
  const [hour = 0, minute = 0, second = 0] = timePart.split(":").map(Number);
  const wallClockAsUtc = Date.UTC(year ?? 1970, (month ?? 1) - 1, day ?? 1, hour, minute, second);

  // The zone's offset depends on the instant, so it is read once at the guessed
  // instant and applied once more: the second pass settles daylight-saving dates,
  // where the first pass is off by an hour.
  const guess = new Date(wallClockAsUtc);
  return new Date(wallClockAsUtc - zoneOffsetMs(guess, timeZone));
}

/** Milliseconds that `timeZone` is ahead of UTC at the given instant (east of
 * Greenwich is positive). `Intl` is the only portable source of that offset. */
function zoneOffsetMs(at: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  }).formatToParts(at);
  const part = (type: string) => Number(parts.find((p) => p.type === type)?.value);
  const asUtc = Date.UTC(
    part("year"),
    part("month") - 1,
    part("day"),
    part("hour"),
    part("minute"),
    part("second"),
  );
  return asUtc - at.getTime();
}

/** The backend's naive local date-time as the `YYYY-MM-DDTHH:mm` value an
 * `<input type="datetime-local">` expects. Rebuilt from the parsed parts
 * instead of sliced off the string, so a date without a time part (or with
 * seconds) still yields a value the input accepts. */
export function toDateTimeLocalValue(isoLocalDateTime: string): string {
  const d = parseLocalDateTime(isoLocalDateTime);
  const hhmm = `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
    d.getDate(),
  ).padStart(2, "0")}T${hhmm}`;
}

/** The inverse of {@link toDateTimeLocalValue}: a `datetime-local` value back
 * to the naive local date-time the API expects. Empty input yields an empty
 * string so callers can treat "not filled in yet" as a missing value. */
export function fromDateTimeLocalValue(value: string): string {
  return value ? `${value}:00` : "";
}

/** Orders naive local date-times from earliest to latest — day first, then time
 * of day — so a list of alternative dates reads as a calendar instead of
 * whatever order it arrived in. Usable directly as an `Array#sort` comparator. */
export function compareLocalDateTimes(a: string, b: string): number {
  return parseLocalDateTime(a).getTime() - parseLocalDateTime(b).getTime();
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

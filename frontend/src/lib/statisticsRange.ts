const DATE_KEY = /^(\d{4})-(\d{2})-(\d{2})$/;

function pad(value: number): string {
  return String(value).padStart(2, "0");
}

export function dateKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseDateKey(key: string): Date | null {
  const match = DATE_KEY.exec(key);
  if (!match) return null;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day);
  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day ? date : null;
}

export function sanitizeDate(value: string | null | undefined): string {
  return value && parseDateKey(value) ? value : "";
}

export function shiftDays(key: string, days: number): string {
  const date = parseDateKey(key);
  if (!date) return "";
  date.setDate(date.getDate() + days);
  return dateKey(date);
}

/** Converts a local calendar day to inclusive UTC instants, respecting DST at each boundary. */
export function localDayRange(from: string, to: string): { from?: string; to?: string } {
  const fromDate = from ? parseDateKey(from) : null;
  const toDate = to ? parseDateKey(to) : null;
  return {
    from: fromDate ? new Date(fromDate.getFullYear(), fromDate.getMonth(), fromDate.getDate()).toISOString() : undefined,
    to: toDate ? new Date(new Date(toDate.getFullYear(), toDate.getMonth(), toDate.getDate() + 1).getTime() - 1).toISOString() : undefined,
  };
}

export function formatLocalDate(iso: string): string {
  return new Date(iso).toLocaleDateString("es-AR", { day: "numeric", month: "short", year: "numeric" });
}

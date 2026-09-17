/** Quick date-range shortcuts for the "Cuándo" search field. Every preset
 * only fills in the same `dateFrom`/`dateTo` (YYYY-MM-DD) strings the page
 * already tracks — no new filter, just a faster way to set the two that
 * exist today (the old "Hoy" chip did the same, inline, for one case). */

export type DatePresetKey = "hoy" | "finde" | "semana" | "mes";

export const DATE_PRESETS: { key: DatePresetKey; label: string }[] = [
  { key: "hoy", label: "Hoy" },
  { key: "finde", label: "Este fin de semana" },
  { key: "semana", label: "Esta semana" },
  { key: "mes", label: "Este mes" },
];

export function dateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

/** Parses a `YYYY-MM-DD` key as a local-midnight Date — `new Date(key)`
 * parses as UTC, which shifts the displayed day back by a day in UTC-3. */
export function parseDateKey(key: string): Date | undefined {
  if (!key) return undefined;
  const [year, month, day] = key.split("-").map(Number);
  if (!year || !month || !day) return undefined;
  return new Date(year, month - 1, day);
}

export function getDatePresetRange(key: DatePresetKey, now = new Date()): { from: string; to: string } {
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  switch (key) {
    case "hoy":
      return { from: dateKey(start), to: dateKey(start) };
    case "finde": {
      const daysUntilSaturday = (6 - start.getDay() + 7) % 7;
      const saturday = new Date(start);
      saturday.setDate(start.getDate() + daysUntilSaturday);
      const sunday = new Date(saturday);
      sunday.setDate(saturday.getDate() + 1);
      return { from: dateKey(saturday), to: dateKey(sunday) };
    }
    case "semana": {
      const end = new Date(start);
      end.setDate(start.getDate() + 6);
      return { from: dateKey(start), to: dateKey(end) };
    }
    case "mes": {
      const end = new Date(start.getFullYear(), start.getMonth() + 1, 0);
      return { from: dateKey(start), to: dateKey(end) };
    }
  }
}

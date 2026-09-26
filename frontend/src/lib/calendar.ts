// Month-grid math for the "Mis actividades" calendar view. Dates here are
// plain local `Date`s (no timezone concerns — the browser's own local time is
// what the calendar UI shows), but activity dateTimes coming from the backend
// are naive ISO strings, so `activityDateKey` goes through `parseLocalDateTime`
// (see formatDate.ts) instead of `new Date(iso)`.

import { parseLocalDateTime } from "@/lib/formatDate";

export interface CalendarCell {
  date: Date;
  /** Whether this cell belongs to the visible month, vs. a leading/trailing day
   * from the previous/next month shown only to complete the week row. */
  inMonth: boolean;
}

/** Monday-first weekday order, matching the grid's column order. */
export const WEEKDAY_LABELS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

export const MONTH_NAMES = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
];

// Sunday-start, matching Date#getDay()'s own indexing (0=Sunday..6=Saturday) —
// unlike WEEKDAY_LABELS above, which is Monday-first to match the grid columns.
const WEEKDAY_NAMES_FULL = ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"];

/** "Viernes 25 de septiembre" — the selected day's agenda heading. */
export function formatFullDate(date: Date): string {
  const weekday = WEEKDAY_NAMES_FULL[date.getDay()];
  const month = MONTH_NAMES[date.getMonth()]?.toLowerCase();
  return `${weekday} ${date.getDate()} de ${month}`;
}

/** Full Monday-start weeks covering `month0` (0-11) of `year`, including the
 * leading/trailing days of the adjacent months needed to complete the
 * first/last week — always a multiple of 7 cells (5 or 6 rows). */
export function getMonthGrid(year: number, month0: number): CalendarCell[] {
  const firstOfMonth = new Date(year, month0, 1);
  // Date#getDay(): 0=Sunday..6=Saturday — shift to a Monday-first offset.
  const leadingDays = (firstOfMonth.getDay() + 6) % 7;
  const start = new Date(year, month0, 1 - leadingDays);

  const daysInMonth = new Date(year, month0 + 1, 0).getDate();
  const totalCells = Math.ceil((leadingDays + daysInMonth) / 7) * 7;

  return Array.from({ length: totalCells }, (_, i) => {
    const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + i);
    return { date, inMonth: date.getMonth() === month0 };
  });
}

/** "YYYY-MM-DD" in local time, for grouping activities by day and comparing
 * against a selected/"today" cell. Never `toISOString()` here — it converts
 * to UTC first and can shift the date across midnight. */
export function dateKey(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** `dateKey` of an activity's own naive backend ISO datetime. */
export function activityDateKey(isoLocalDateTime: string): string {
  return dateKey(parseLocalDateTime(isoLocalDateTime));
}

function formatLocalDateTime(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  const hh = String(date.getHours()).padStart(2, "0");
  const mm = String(date.getMinutes()).padStart(2, "0");
  const ss = String(date.getSeconds()).padStart(2, "0");
  return `${y}-${m}-${d}T${hh}:${mm}:${ss}`;
}

/** The `dateFrom`/`dateTo` window covering an entire calendar month, as naive
 * ISO LocalDateTime strings — matches what the backend's date-range filter on
 * `/activities/organizers/me` and `/activities/participants/me` expects. */
export function monthDateRange(year: number, month0: number): { dateFrom: string; dateTo: string } {
  return {
    dateFrom: formatLocalDateTime(new Date(year, month0, 1, 0, 0, 0)),
    dateTo: formatLocalDateTime(new Date(year, month0 + 1, 0, 23, 59, 59)),
  };
}

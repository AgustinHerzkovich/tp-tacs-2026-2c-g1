import { describe, expect, it } from "vitest";
import { activityDateKey, dateKey, formatFullDate, getMonthGrid, monthDateRange } from "@/lib/calendar";

describe("getMonthGrid", () => {
  it("pads a month that starts mid-week with the previous month's trailing days", () => {
    // September 2026 starts on a Tuesday, so the grid should lead with Monday Aug 31.
    const cells = getMonthGrid(2026, 8);

    expect(cells[0]?.date).toEqual(new Date(2026, 7, 31));
    expect(cells[0]?.inMonth).toBe(false);
    expect(cells[1]?.date).toEqual(new Date(2026, 8, 1));
    expect(cells[1]?.inMonth).toBe(true);
  });

  it("always returns a whole number of Monday-first weeks", () => {
    const cells = getMonthGrid(2026, 8);

    expect(cells).toHaveLength(35);
    expect(cells[0]?.date.getDay()).toBe(1); // Monday
    expect(cells.at(-1)?.date.getDay()).toBe(0); // Sunday
  });

  it("uses 6 rows for a month that needs them", () => {
    // March 2026 starts on a Sunday and has 31 days: 6 leading days + 31 = 37,
    // which only fits a whole number of weeks at 6 rows (42 cells).
    const cells = getMonthGrid(2026, 2);

    expect(cells).toHaveLength(42);
  });

  it("marks the visible month's own days as inMonth", () => {
    const cells = getMonthGrid(2026, 8);
    const septemberCells = cells.filter((c) => c.inMonth);

    expect(septemberCells).toHaveLength(30);
    expect(septemberCells[0]?.date.getDate()).toBe(1);
    expect(septemberCells.at(-1)?.date.getDate()).toBe(30);
  });
});

describe("dateKey / activityDateKey", () => {
  it("formats a local date as YYYY-MM-DD", () => {
    expect(dateKey(new Date(2026, 8, 6))).toBe("2026-09-06");
  });

  it("reads the date from a naive backend datetime string", () => {
    expect(activityDateKey("2026-09-25T19:00:00")).toBe("2026-09-25");
  });
});

describe("monthDateRange", () => {
  it("spans from the first to the last day of the month, naive local time", () => {
    expect(monthDateRange(2026, 8)).toEqual({
      dateFrom: "2026-09-01T00:00:00",
      dateTo: "2026-09-30T23:59:59",
    });
  });

  it("resolves the last day correctly for a leap February", () => {
    expect(monthDateRange(2024, 1).dateTo).toBe("2024-02-29T23:59:59");
  });
});

describe("formatFullDate", () => {
  it("spells out the weekday and month in Spanish", () => {
    // 2026-09-25 is a Friday.
    expect(formatFullDate(new Date(2026, 8, 25))).toBe("Viernes 25 de septiembre");
  });
});

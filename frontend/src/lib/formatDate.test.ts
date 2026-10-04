import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  compareLocalDateTimes,
  formatActivityWhen,
  formatRelativeTime,
  fromDateTimeLocalValue,
  isPastLocalDateTime,
  toDateTimeLocalValue,
} from "@/lib/formatDate";

describe("formatActivityWhen", () => {
  it('renders "Mar 15 sep · 14:00" for a LocalDateTime string', () => {
    // parseLocalDateTime builds a local Date, so the result is timezone-independent.
    expect(formatActivityWhen("2026-09-15T14:00:00")).toBe("Mar 15 sep · 14:00");
  });

  it("pads hours and minutes", () => {
    expect(formatActivityWhen("2026-01-05T09:05")).toBe("Lun 5 ene · 09:05");
  });

  it("handles a missing time part", () => {
    expect(formatActivityWhen("2026-10-31")).toContain("31 oct · 00:00");
  });
});

describe("isPastLocalDateTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 13, 12, 0)); // 2026-09-13 12:00 local
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it("is false for a future activity", () => {
    expect(isPastLocalDateTime("2026-09-20T14:00:00")).toBe(false);
  });

  it("is true for an activity whose date-time already went by", () => {
    expect(isPastLocalDateTime("2026-09-12T23:59:00")).toBe(true);
  });

  it("treats the exact current minute as not past yet", () => {
    expect(isPastLocalDateTime("2026-09-13T12:00:00")).toBe(false);
  });
});

describe("toDateTimeLocalValue / fromDateTimeLocalValue", () => {
  it("renders a value a datetime-local input accepts", () => {
    expect(toDateTimeLocalValue("2026-09-15T14:30:00")).toBe("2026-09-15T14:30");
  });

  it("fills in midnight when the backend sent no time part", () => {
    expect(toDateTimeLocalValue("2026-10-31")).toBe("2026-10-31T00:00");
  });

  it("round-trips back to the naive local date-time the API expects", () => {
    expect(fromDateTimeLocalValue(toDateTimeLocalValue("2026-01-05T09:05:00"))).toBe(
      "2026-01-05T09:05:00",
    );
  });

  it("treats an empty input as no value", () => {
    expect(fromDateTimeLocalValue("")).toBe("");
  });
});

describe("compareLocalDateTimes", () => {
  it("sorts by day first and then by time of day", () => {
    const dates = [
      "2026-09-21T09:00:00",
      "2026-09-20T18:00:00",
      "2026-09-20T10:00:00",
      "2026-09-02T23:00:00",
    ];
    expect([...dates].sort(compareLocalDateTimes)).toEqual([
      "2026-09-02T23:00:00",
      "2026-09-20T10:00:00",
      "2026-09-20T18:00:00",
      "2026-09-21T09:00:00",
    ]);
  });

  it("returns 0 for the same instant written with and without seconds", () => {
    expect(compareLocalDateTimes("2026-09-20T18:00", "2026-09-20T18:00:00")).toBe(0);
  });

  it("does not mutate the array it sorts", () => {
    const dates = ["2026-09-21T09:00:00", "2026-09-20T18:00:00"];
    [...dates].sort(compareLocalDateTimes);
    expect(dates).toEqual(["2026-09-21T09:00:00", "2026-09-20T18:00:00"]);
  });
});

describe("formatRelativeTime", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 13, 12, 0)); // 2026-09-13 12:00 local
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it.each([
    [new Date(2026, 8, 13, 12, 0), "recién"],
    [new Date(2026, 8, 13, 11, 48), "hace 12 min"],
    [new Date(2026, 8, 13, 9, 0), "hace 3 h"],
    [new Date(2026, 8, 12, 12, 0), "ayer"],
    [new Date(2026, 8, 8, 12, 0), "hace 5 d"],
  ])("formats %s", (when, expected) => {
    const iso = `${when.getFullYear()}-${String(when.getMonth() + 1).padStart(2, "0")}-${String(
      when.getDate(),
    ).padStart(2, "0")}T${String(when.getHours()).padStart(2, "0")}:${String(when.getMinutes()).padStart(2, "0")}`;
    expect(formatRelativeTime(iso)).toBe(expected);
  });
});
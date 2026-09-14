import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { formatActivityWhen, formatRelativeTime } from "@/lib/formatDate";

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
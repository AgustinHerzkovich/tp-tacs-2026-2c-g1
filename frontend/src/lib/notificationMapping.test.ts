import { describe, expect, it } from "vitest";
import { iconForNotificationKind, mapNotificationKind } from "@/lib/notificationMapping";

describe("mapNotificationKind", () => {
  it.each([
    ["BAD_WEATHER_ALERT", "warn"],
    ["CANCELLED", "cancel"],
    ["REPROGRAMMED", "reprog"],
    ["STARTED", "info"],
    ["STARTING_SOON", "info"],
  ])("maps backend %s -> %s", (type, expected) => {
    expect(mapNotificationKind(type)).toBe(expected);
  });

  it("falls back to info for unknown types", () => {
    expect(mapNotificationKind("SOMETHING_ELSE")).toBe("info");
  });
});

describe("iconForNotificationKind", () => {
  it.each([
    ["warn", "🌧️"],
    ["cancel", "🚫"],
    ["reprog", "🔁"],
    ["info", "⏰"],
  ])("maps kind %s -> %s", (kind, expected) => {
    expect(iconForNotificationKind(kind as "warn" | "cancel" | "reprog" | "info")).toBe(expected);
  });

  it("returns a bell for unknown kinds", () => {
    expect(iconForNotificationKind("weird" as never)).toBe("🔔");
  });
});
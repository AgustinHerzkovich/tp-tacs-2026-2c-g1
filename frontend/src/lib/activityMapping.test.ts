import { describe, expect, it } from "vitest";
import {
  mapActivityStatus,
  mapActivityType,
  pickScene,
  toBackendActivityType,
  toExploreActivity,
  toMisActivity,
} from "@/lib/activityMapping";
import type { ActivityResponse, ActivityStatus, ActivityType } from "@/types/backend";

function makeActivity(overrides: Partial<ActivityResponse> = {}): ActivityResponse {
  return {
    id: "act-1",
    title: "Partido de vóley",
    description: "En el parque",
    type: "OUTDOOR",
    location: { city: "CABA", latitude: -34.6, longitude: -58.4 },
    dateTime: "2026-09-20T14:00:00",
    availability: true,
    minParticipants: 4,
    maxParticipants: 12,
    participantCount: 6,
    participants: [{ userId: "u1" }, { userId: "u2" }],
    weatherConditions: {
      maxRainProbability: 40,
      minTemperature: 12,
      maxTemperature: 26,
      maxWindSpeed: 20,
    },
    anticipationWindow: 24,
    reprogramationRange: { maxDays: 3, initialHour: "09:00:00", finalHour: "21:00:00" },
    status: "CONFIRMED",
    imageUrls: ["https://cdn.example.com/a.jpg", "https://cdn.example.com/b.jpg"],
    ...overrides,
  };
}

describe("pickScene", () => {
  it("is deterministic for the same id", () => {
    expect(pickScene("act-1")).toBe(pickScene("act-1"));
  });
});

describe("mapActivityType / toBackendActivityType", () => {
  it.each([
    ["OUTDOOR", "outdoor"],
    ["INDOOR", "indoor"],
    ["MIXED", "mixed"],
  ] as const)("maps %s -> %s and back", (backend, ui) => {
    expect(mapActivityType(backend)).toBe(ui);
    expect(toBackendActivityType(ui)).toBe(backend);
  });

  it("falls back to outdoor for unknown values", () => {
    expect(mapActivityType("UNKNOWN" as ActivityType)).toBe("outdoor");
    expect(toBackendActivityType("UNKNOWN" as never)).toBe("OUTDOOR");
  });
});

describe("mapActivityStatus", () => {
  it.each([
    ["CONFIRMED", "confirmada"],
    ["PROPOSED", "propuesta"],
    ["RESCHEDULED", "reprogramada"],
    ["CANCELLED", "cancelada"],
    ["FINISHED", "finalizada"],
  ] as const)("maps %s -> %s", (status, expected) => {
    expect(mapActivityStatus(status)).toBe(expected);
  });

  it("falls back to confirmada", () => {
    expect(mapActivityStatus("UNKNOWN" as ActivityStatus)).toBe("confirmada");
  });
});

describe("toExploreActivity / toMisActivity", () => {
  it("maps the shared fields and the card-specific ones", () => {
    const explore = toExploreActivity(makeActivity());
    expect(explore).toMatchObject({
      id: "act-1",
      title: "Partido de vóley",
      type: "outdoor",
      where: "CABA",
      participantIds: ["u1", "u2"],
      imageUrl: "https://cdn.example.com/a.jpg",
      scene: pickScene("act-1"),
      status: "confirmada",
    });
    expect(explore.people).toBe(6);
    expect(explore.when).toContain("20 sep");

    const mis = toMisActivity(makeActivity());
    expect(mis).toMatchObject({
      id: "act-1",
      status: "confirmada",
      joined: 6,
      cap: 12,
      imageUrl: "https://cdn.example.com/a.jpg",
    });
  });

  it("uses a fallback location and null image when absent", () => {
    const explore = toExploreActivity(makeActivity({ location: { city: null, latitude: null, longitude: null }, imageUrls: [] }));
    expect(explore.where).toBe("Ubicación a confirmar");
    expect(explore.imageUrl).toBeNull();
  });
});
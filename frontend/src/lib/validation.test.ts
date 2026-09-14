import { describe, expect, it } from "vitest";
import { firstInvalidStep, validateAll, validateStep } from "@/lib/validation";
import type { WizardFormState } from "@/types/domain";

function baseForm(overrides: Partial<WizardFormState> = {}): WizardFormState {
  return {
    title: "Partido de vóley",
    desc: "Jugamos en el parque.",
    type: "outdoor",
    place: "Parque Centenario",
    latitude: -34.6,
    longitude: -58.4,
    images: [],
    date: "2099-12-31",
    time: "18:00",
    min: 4,
    max: 12,
    rain: 40,
    wind: 30,
    tMin: 12,
    tMax: 28,
    anticipation: "24",
    reschedule: "3",
    reprogramStart: "09:00",
    reprogramEnd: "21:00",
    ...overrides,
  };
}

describe("validateStep(0) — info", () => {
  it("requires a title and a description", () => {
    const errors = validateStep(0, baseForm({ title: "", desc: "" }));
    expect(errors.title).toBeDefined();
    expect(errors.desc).toBeDefined();
  });

  it("rejects a title shorter than 3 characters", () => {
    const errors = validateStep(0, baseForm({ title: "ab" }));
    expect(errors.title).toMatch(/al menos 3 caracteres/);
  });

  it("rejects a description longer than 500 characters", () => {
    const errors = validateStep(0, baseForm({ desc: "x".repeat(501) }));
    expect(errors.desc).toMatch(/500 caracteres/);
  });

  it("passes a valid title and description", () => {
    expect(validateStep(0, baseForm())).toEqual({});
  });
});

describe("validateStep(1) — lugar y fecha", () => {
  it("requires a place and coordinates once a place is typed", () => {
    const noPlace = validateStep(1, baseForm({ place: "", latitude: null, longitude: null }));
    expect(noPlace.place).toBeDefined();

    const withoutCoords = validateStep(1, baseForm({ latitude: null, longitude: null }));
    expect(withoutCoords.place).toMatch(/Buscá una ubicación/);
  });

  it("enforces participant bounds and min <= max", () => {
    expect(validateStep(1, baseForm({ min: 0 })).min).toBeDefined();
    expect(validateStep(1, baseForm({ max: 100 })).max).toBeDefined();
    expect(validateStep(1, baseForm({ min: 10, max: 4 })).max).toMatch(/menor que el mínimo/);
  });

  it("requires a date and a time and rejects past dates", () => {
    expect(validateStep(1, baseForm({ date: "" })).date).toBeDefined();
    expect(validateStep(1, baseForm({ date: "2099-12-31", time: "" })).time).toBeDefined();
    expect(validateStep(1, baseForm({ date: "2020-01-01", time: "10:00" })).date).toMatch(/en el futuro/);
  });

  it("passes a full valid location and future date", () => {
    expect(validateStep(1, baseForm())).toEqual({});
  });
});

describe("validateStep(4) — alertas", () => {
  it("requires an end hour later than the start hour", () => {
    expect(validateStep(4, baseForm({ reprogramStart: "20:00", reprogramEnd: "09:00" })).reprogramEnd).toBeDefined();
    expect(validateStep(4, baseForm({ reprogramStart: "09:00", reprogramEnd: "09:00" })).reprogramEnd).toBeDefined();
    expect(validateStep(4, baseForm())).toEqual({});
  });
});

describe("slider/select steps validate empty", () => {
  it("returns no errors for steps 2 and 3", () => {
    expect(validateStep(2, baseForm())).toEqual({});
    expect(validateStep(3, baseForm())).toEqual({});
  });
});

describe("validateAll / firstInvalidStep", () => {
  it("merges errors from every step", () => {
    const errors = validateAll(baseForm({ title: "", reprogramEnd: "08:00" }));
    expect(errors.title).toBeDefined();
    expect(errors.reprogramEnd).toBeDefined();
  });

  it("reports the first invalid step in order", () => {
    expect(firstInvalidStep(baseForm())).toBe(-1);
    expect(firstInvalidStep(baseForm({ title: "" }))).toBe(0);
    expect(firstInvalidStep(baseForm({ place: "" }))).toBe(1);
    expect(firstInvalidStep(baseForm({ reprogramEnd: "08:00" }))).toBe(4);
  });
});
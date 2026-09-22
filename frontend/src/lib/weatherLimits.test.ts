import { describe, expect, it } from "vitest";
import { formatWeatherLimits, isWithinWeatherLimits } from "@/lib/weatherLimits";
import type { WeatherConditionsDTO, WeatherForecastDTO } from "@/types/backend";

const forecast = (temperature: number, chanceOfRain: number, windSpeed: number): WeatherForecastDTO => ({
  dateTime: "2026-09-15T14:00:00",
  temperature,
  chanceOfRain,
  windSpeed,
});

const conditions = (overrides: Partial<WeatherConditionsDTO> = {}): WeatherConditionsDTO => ({
  maxRainProbability: 30,
  minTemperature: 5,
  maxTemperature: 18,
  maxWindSpeed: 25,
  ...overrides,
});

describe("isWithinWeatherLimits", () => {
  it("is true when every field is within its limit", () => {
    expect(isWithinWeatherLimits(forecast(12, 10, 14), conditions())).toBe(true);
  });

  it.each([
    ["rain over the max", forecast(12, 45, 14)],
    ["wind over the max", forecast(12, 10, 30)],
    ["temperature below the min", forecast(2, 10, 14)],
    ["temperature above the max", forecast(25, 10, 14)],
  ])("is false when %s", (_label, badForecast) => {
    expect(isWithinWeatherLimits(badForecast, conditions())).toBe(false);
  });

  it("treats a null limit as unrestricted", () => {
    const noLimits = conditions({ maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null });
    expect(isWithinWeatherLimits(forecast(40, 100, 90), noLimits)).toBe(true);
  });
});

describe("formatWeatherLimits", () => {
  it("joins every configured clause", () => {
    expect(formatWeatherLimits(conditions())).toBe("máx. 30% lluvia · 25 km/h · 5°–18°");
  });

  it("omits a clause whose limit is unset", () => {
    expect(formatWeatherLimits(conditions({ maxWindSpeed: null }))).toBe("máx. 30% lluvia · 5°–18°");
  });

  it("falls back to a neutral message when nothing is configured", () => {
    const noLimits = conditions({ maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null });
    expect(formatWeatherLimits(noLimits)).toBe("sin límites configurados");
  });
});

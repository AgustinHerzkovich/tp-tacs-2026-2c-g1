// Client-side, informational-only comparison between a weather forecast and
// an activity's configured limits. This never decides anything by itself —
// the backend scheduler (ActivityAnticipationCheckScheduler) is the source
// of truth for opening a reprogramming votation. This only powers the
// "✓ Dentro de lo permitido" / "⚠️ Supera lo permitido" hint shown to users.

import type { WeatherConditionsDTO, WeatherForecastDTO } from "@/types/backend";

/** A `null` limit means "no restriction" — every field on `WeatherConditionsDTO`
 * is optional at activity-creation time. */
export function isWithinWeatherLimits(forecast: WeatherForecastDTO, conditions: WeatherConditionsDTO): boolean {
  const { maxRainProbability, minTemperature, maxTemperature, maxWindSpeed } = conditions;
  if (maxRainProbability != null && forecast.chanceOfRain > maxRainProbability) return false;
  if (maxWindSpeed != null && forecast.windSpeed > maxWindSpeed) return false;
  if (minTemperature != null && forecast.temperature < minTemperature) return false;
  if (maxTemperature != null && forecast.temperature > maxTemperature) return false;
  return true;
}

/** "máx. 30% lluvia · 25 km/h · 5°–18°" — omits clauses whose limit is unset. */
export function formatWeatherLimits(conditions: WeatherConditionsDTO): string {
  const { maxRainProbability, minTemperature, maxTemperature, maxWindSpeed } = conditions;
  const parts: string[] = [];
  if (maxRainProbability != null) parts.push(`${maxRainProbability}% lluvia`);
  if (maxWindSpeed != null) parts.push(`${maxWindSpeed} km/h`);
  if (minTemperature != null || maxTemperature != null) {
    parts.push(`${minTemperature ?? "-"}°–${maxTemperature ?? "-"}°`);
  }
  return parts.length > 0 ? `máx. ${parts.join(" · ")}` : "sin límites configurados";
}

// Refleja los DTOs del backend (backend/src/main/java/com/solnotfound/dto) y sus enums
// (backend/src/main/java/com/solnotfound/entity/activity) campo por campo. `LocalDateTime`
// y `LocalTime` viajan como strings ISO-8601 sin zona horaria. Mantener en sync con
// el fuente Java en lugar de adivinar las formas.

export type ActivityType = "OUTDOOR" | "INDOOR" | "MIXED";

/** PROPOSED es el estado en el que una actividad está en votación de reprogramación. */
export type ActivityStatus = "CONFIRMED" | "PROPOSED" | "RESCHEDULED" | "CANCELLED" | "FINISHED";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface LocationDTO {
  city: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface ParticipantDTO {
  userId: string;
  name: string | null;
}

export interface WeatherConditionsDTO {
  maxRainProbability: number | null;
  minTemperature: number | null;
  maxTemperature: number | null;
  maxWindSpeed: number | null;
}

export interface ReprogramationRangeDTO {
  maxDays: number;
  /** LocalTime, ej. "10:00:00" */
  initialHour: string;
  /** LocalTime, ej. "20:00:00" */
  finalHour: string;
}

export interface ActivityResponse {
  id: string;
  title: string;
  description: string | null;
  type: ActivityType;
  location: LocationDTO;
  /** LocalDateTime, ej. "2026-09-06T09:00:00" */
  dateTime: string;
  availability: boolean | null;
  minParticipants: number;
  maxParticipants: number;
  participantCount: number;
  participants: ParticipantDTO[];
  weatherConditions: WeatherConditionsDTO;
  /** Horas antes de la actividad en las que se revisa el clima. */
  anticipationWindow: number;
  reprogramationRange: ReprogramationRangeDTO;
  status: ActivityStatus;
  imageUrls: string[];
  /** Subject de Keycloak de quien organiza; es quien "posee" la actividad. */
  organizerId: string | null;
}

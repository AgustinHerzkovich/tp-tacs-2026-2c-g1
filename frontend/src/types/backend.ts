// Mirrors the backend's DTOs (backend/src/main/java/com/solnotfound/dto) and
// enums (backend/src/main/java/com/solnotfound/entity/**) field-for-field.
// Keep this in sync with the Java source rather than guessing shapes —
// `LocalDateTime`/`LocalTime`/`Instant` all serialize as ISO-8601 strings.

export type ActivityType = "OUTDOOR" | "INDOOR" | "MIXED";

/** See ActivityStatus.java: PROPOSED means an activity is currently up for a
 * reprogramming vote (there is an associated `VotationDTO` with status
 * "ACTIVE"). */
export type ActivityStatus = "CONFIRMED" | "PROPOSED" | "RESCHEDULED" | "CANCELLED" | "FINISHED";

export type VotationStatus = "ACTIVE" | "CLOSED";

export interface LocationDTO {
  city: string | null;
  latitude: number | null;
  longitude: number | null;
}

export interface ParticipantDTO {
  userId: string;
}

export interface ReprogramationRangeDTO {
  maxDays: number;
  /** LocalTime, e.g. "10:00:00" */
  initialHour: string;
  /** LocalTime, e.g. "20:00:00" */
  finalHour: string;
}

export interface WeatherConditionsDTO {
  maxRainProbability: number | null;
  minTemperature: number | null;
  maxTemperature: number | null;
  maxWindSpeed: number | null;
}

export interface ActivityResponse {
  id: string;
  title: string;
  description: string | null;
  type: ActivityType;
  location: LocationDTO;
  /** LocalDateTime, e.g. "2026-09-06T09:00:00" */
  dateTime: string;
  availability: boolean | null;
  minParticipants: number;
  maxParticipants: number;
  participantCount: number;
  participants: ParticipantDTO[];
  weatherConditions: WeatherConditionsDTO;
  anticipationWindow: number;
  reprogramationRange: ReprogramationRangeDTO;
  status: ActivityStatus;
  imageUrls: string[];
}

export interface ActivityFilterParams {
  type?: ActivityType;
  city?: string;
  /** ISO LocalDateTime */
  dateFrom?: string;
  /** ISO LocalDateTime */
  dateTo?: string;
  availability?: boolean;
}

export interface CreateActivityRequest {
  title: string;
  description?: string;
  type: ActivityType;
  location: LocationDTO;
  /** ISO LocalDateTime, must be in the future */
  dateTime: string;
  minParticipants: number;
  maxParticipants: number;
  weatherConditions: WeatherConditionsDTO;
  anticipationWindow: number;
  reprogramationRange: ReprogramationRangeDTO;
}

export interface WeatherForecastDTO {
  dateTime: string;
  temperature: number;
  chanceOfRain: number;
  windSpeed: number;
}

export interface ActivityWeatherResponse {
  activityId: string;
  location: LocationDTO;
  activityDateTime: string;
  currentWeather: WeatherForecastDTO;
  activityForecast: WeatherForecastDTO;
}

export interface VotationOptionDTO {
  dateTime: string;
  voteCount: number;
  voterNames: string[];
}

export interface VotationDTO {
  id: string;
  activityId: string;
  creationDate: string;
  status: VotationStatus;
  options: VotationOptionDTO[];
}

export interface UpdateVotationOptionsRequest {
  dates: string[];
}

export interface UpdateVotationSettingsRequest {
  minQuorum: number;
  /** ISO-8601 duration, e.g. "PT24H" */
  duration: string;
}

export interface NotificationResponse {
  id: string;
  activityId: string;
  type: string;
  title: string;
  message: string;
  createdAt: string;
}

export interface UserDTO {
  id: string;
  name: string;
}

export interface ActivityStatisticsResponse {
  created: number;
  rescheduled: number;
  cancelled: number;
  cancelledByWeather: number;
}

export interface WeatherProviderStatisticsResponse {
  requests: number;
  successful: number;
  failed: number;
  averageResponseTimeMs: number;
}

export interface StatisticsResponse {
  from: string;
  to: string;
  activities: ActivityStatisticsResponse;
  weatherProvider: WeatherProviderStatisticsResponse;
}

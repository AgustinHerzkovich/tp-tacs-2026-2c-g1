import type {
  ActivityResponse,
  ActivityStatus,
  ActivityType,
  ReprogramationRangeDTO,
  WeatherConditionsDTO,
} from "../types/backend";
import { escapeHtml } from "./sendMessage";

const WEEKDAYS = ["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"];
const MONTHS = ["ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"];

/** Mismo vocabulario que la app (frontend/src/lib/activityVisuals.ts). */
const STATUS_LABEL: Record<ActivityStatus, string> = {
  CONFIRMED: "Confirmada",
  PROPOSED: "Propuesta",
  RESCHEDULED: "Reprogramada",
  CANCELLED: "Cancelada",
  FINISHED: "Finalizada",
};

const TYPE_LABEL: Record<ActivityType, string> = {
  OUTDOOR: "Outdoor",
  INDOOR: "Indoor",
  MIXED: "Mixto",
};

function parseLocalDateTime(iso: string): Date {
  const [datePart, timePart = "00:00:00"] = iso.split("T");
  const [year, month, day] = (datePart ?? "").split("-").map(Number);
  const [hour = 0, minute = 0] = timePart.split(":").map(Number);
  return new Date(year ?? 1970, (month ?? 1) - 1, day ?? 1, hour, minute);
}

/** "Mar 15 sep · 14:00". Son fechas locales sin zona horaria, así que se parsean los componentes. */
export function formatActivityWhen(isoLocalDateTime: string): string {
  const date = parseLocalDateTime(isoLocalDateTime);
  const hour = String(date.getHours()).padStart(2, "0");
  const minute = String(date.getMinutes()).padStart(2, "0");
  return `${WEEKDAYS[date.getDay()]} ${date.getDate()} ${MONTHS[date.getMonth()]} · ${hour}:${minute}`;
}

/** "de 5° a 22°, hasta 40% de lluvia, viento hasta 25 km/h", o "sin límites cargados". */
export function formatWeatherConditions(conditions: WeatherConditionsDTO): string {
  const parts: string[] = [];

  const { minTemperature, maxTemperature, maxRainProbability, maxWindSpeed } = conditions;
  if (minTemperature !== null && maxTemperature !== null) {
    parts.push(`de ${minTemperature}° a ${maxTemperature}°`);
  } else if (maxTemperature !== null) {
    parts.push(`hasta ${maxTemperature}°`);
  } else if (minTemperature !== null) {
    parts.push(`desde ${minTemperature}°`);
  }

  if (maxRainProbability !== null) {
    parts.push(maxRainProbability === 0 ? "sin lluvia" : `hasta ${maxRainProbability}% de lluvia`);
  }
  if (maxWindSpeed !== null) {
    parts.push(`viento hasta ${maxWindSpeed} km/h`);
  }

  return parts.length > 0 ? parts.join(", ") : "sin límites cargados";
}

/** "48 h antes de la actividad", o "sin margen previo" cuando vale cero. */
export function formatAnticipation(anticipationWindow: number): string {
  if (anticipationWindow === 0) return "sin margen previo";
  return anticipationWindow === 1 ? "1 h antes de la actividad" : `${anticipationWindow} h antes de la actividad`;
}

function formatLocalTime(localTime: string): string {
  const [hour = 0, minute = 0] = localTime.split(":").map(Number);
  return `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")}`;
}

/** Recorta textos que vienen del backend para que no revienten un mensaje de Telegram. */
function truncate(value: string, maxLength: number): string {
  return value.length <= maxLength ? value : `${value.slice(0, maxLength - 1).trimEnd()}…`;
}

/** "hasta 7 días, de 10:00 a 20:00". */
export function formatReprogramation(range: ReprogramationRangeDTO): string {
  const days = range.maxDays === 1 ? "1 día" : `${range.maxDays} días`;
  return `hasta ${days}, de ${formatLocalTime(range.initialHour)} a ${formatLocalTime(range.finalHour)}`;
}

export interface ActivityCardOptions {
  /** Índice 1-based que se muestra en el título de la actividad. */
  position?: number;
  /** Nota extra, por ejemplo que la actividad está en votación. */
  note?: string;
}

/** Arma el bloque de una actividad con todo el estado que devuelve el backend. */
export function formatActivityCard(activity: ActivityResponse, options: ActivityCardOptions = {}): string {
  const { position, note } = options;
  const title = truncate(escapeHtml(activity.title), 120);
  const heading = position === undefined ? title : `${position}. ${title}`;
  const city = truncate(escapeHtml(activity.location.city ?? "ubicación a confirmar"), 60);

  const lines = [
    `<b>${heading}</b>`,
    `${formatActivityWhen(activity.dateTime)} · 📍 ${city} · ${TYPE_LABEL[activity.type]}`,
    `Estado: ${STATUS_LABEL[activity.status]}`,
  ];

  if (activity.availability === false) {
    lines.push("Inscripciones: cerradas");
  }

  lines.push(
    `Participantes: ${activity.participantCount} de ${activity.maxParticipants} (mínimo ${activity.minParticipants})`
  );
  lines.push(`Clima: ${formatWeatherConditions(activity.weatherConditions)}`);
  lines.push(`Chequeo del clima: ${formatAnticipation(activity.anticipationWindow)}`);
  lines.push(`Reprogramación: ${formatReprogramation(activity.reprogramationRange)}`);

  if (note) {
    lines.push(note);
  }

  return lines.join("\n");
}

/** Aviso para las actividades cuya reprogramación se está votando (ActivityStatus.PROPOSED). */
export const VOTATION_NOTE = "🗳️ Hay una votación abierta para decidir la reprogramación. La votación se hace en la app.";

export interface ActivityListOptions {
  /** Encabezado del mensaje, con la cantidad de actividades. */
  header: string;
  /** Texto a enviar cuando la lista queda vacía. */
  emptyMessage: string;
  /** Nota opcional para una actividad concreta, por ejemplo la votación abierta. */
  noteFor?: (activity: ActivityResponse) => string | undefined;
}

/** Arma el mensaje completo del listado; los handlers lo mandan partido con sendLongMessage. */
export function renderActivityList(
  activities: ActivityResponse[],
  options: ActivityListOptions
): string {
  if (activities.length === 0) {
    return options.emptyMessage;
  }

  const header = `${options.header} (${activities.length})`;
  const cards = activities.map((activity, index) =>
    formatActivityCard(activity, {
      position: index + 1,
      note: options.noteFor?.(activity),
    })
  );

  return [header, ...cards].join("\n\n");
}

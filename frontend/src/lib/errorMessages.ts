// User-facing messages for API errors. The backend answers every error with
// an RFC 7807 ProblemDetail carrying a stable `code` (see ErrorCode.java);
// its `detail` is an English developer message that may include ids or
// provider details, so it is never shown to the user.

const MESSAGES_BY_CODE: Record<string, string> = {
  VALIDATION_FAILED: "Revisá los datos ingresados.",
  INVALID_PARAMETER: "Revisá los datos ingresados.",
  INVALID_ACTIVITY: "Revisá los datos de la actividad.",
  ACTIVITY_DATE_IN_PAST: "La fecha de la actividad tiene que ser futura.",
  INVALID_DATE_RANGE: "La fecha de inicio no puede ser posterior a la de fin.",
  TOO_MANY_IMAGES: "Podés subir hasta 5 imágenes.",
  IMAGE_TOO_LARGE: "Cada imagen puede pesar hasta 5 MB.",
  INVALID_IMAGE: "Las imágenes tienen que ser JPG, PNG o WebP.",
  ACTIVITY_NOT_FOUND: "No encontramos esta actividad.",
  ACTIVITY_FULL: "La actividad ya no tiene lugares disponibles.",
  ACTIVITY_CLOSED: "Esta actividad ya no admite cambios de participantes.",
  NOT_ACTIVITY_MEMBER: "Tenés que estar anotado en la actividad para hacer esto.",
  INVALID_ACTIVITY_STATUS: "El estado elegido no es válido.",
  VOTATION_NOT_FOUND: "No encontramos esta votación.",
  VOTATION_OPTION_NOT_FOUND: "Esa opción ya no forma parte de la votación.",
  VOTATION_CLOSED: "La votación ya cerró.",
  NOT_ORGANIZER: "Solo el organizador puede hacer esto.",
  INVALID_VOTATION_OPTIONS: "Algunas fechas están fuera del rango permitido o tienen mal clima pronosticado.",
  INVALID_VOTATION_SETTINGS: "La votación tiene que cerrar antes de la primera alternativa.",
  ACCESS_DENIED: "No tenés permiso para hacer esto.",
  RESOURCE_NOT_FOUND: "No encontramos lo que buscabas.",
  WEATHER_UNAVAILABLE: "No pudimos consultar el pronóstico. Probá de nuevo en unos minutos.",
  STATISTICS_UNAVAILABLE: "No pudimos cargar las estadísticas. Probá de nuevo en unos minutos.",
};

export const GENERIC_ERROR_MESSAGE = "No pudimos completar la operación. Probá de nuevo más tarde.";

function messageForStatus(status: number): string {
  if (status === 401) return "Tu sesión expiró. Iniciá sesión de nuevo.";
  if (status === 403) return MESSAGES_BY_CODE.ACCESS_DENIED!;
  if (status === 404) return MESSAGES_BY_CODE.RESOURCE_NOT_FOUND!;
  if (status === 400 || status === 422) return MESSAGES_BY_CODE.VALIDATION_FAILED!;
  return GENERIC_ERROR_MESSAGE;
}

/** Extracts the backend error code from a parsed error body, if any. */
export function errorCodeOf(body: unknown): string | null {
  return body && typeof body === "object" && "code" in body && typeof body.code === "string"
    ? body.code
    : null;
}

/** Spanish message to show for a failed API call: the one for its error code
 * when the code is known, otherwise a generic one based on the HTTP status. */
export function userMessageFor(status: number, code: string | null): string {
  return (code && MESSAGES_BY_CODE[code]) || messageForStatus(status);
}

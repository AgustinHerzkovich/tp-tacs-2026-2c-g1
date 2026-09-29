import type { ActivityResponse, PageResponse } from "../types/backend";
import { backendFetch } from "./auth";

/** `size` máximo que acepta el backend (ActivityController#pageRequest). */
const PAGE_SIZE = 100;

/** Tope de páginas para que un backend que no avance el cursor no loophee. */
const MAX_PAGES = 20;

interface ActivitiesQuery {
  page?: number;
  size?: number;
}

/**
 * Trae una página de actividades del backend, con el JWT y el API token del bot.
 *
 * @throws Error si el backend responde con un error o con un cuerpo que no es una página.
 */
export async function getActivities(query: ActivitiesQuery = {}): Promise<PageResponse<ActivityResponse>> {
  const params = new URLSearchParams({
    page: String(query.page ?? 0),
    size: String(query.size ?? PAGE_SIZE),
  });

  const response = await backendFetch(`/activities?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`El backend respondió ${response.status} al pedir actividades`);
  }

  const page = (await response.json()) as Partial<PageResponse<ActivityResponse>>;
  if (!page || !Array.isArray(page.content)) {
    throw new Error("El backend respondió una página de actividades incompleta");
  }

  return {
    content: page.content,
    page: page.page ?? query.page ?? 0,
    size: page.size ?? query.size ?? PAGE_SIZE,
    totalElements: page.totalElements ?? page.content.length,
    totalPages: page.totalPages ?? 1,
    first: page.first ?? true,
    last: page.last ?? true,
  };
}

/**
 * Trae todas las actividades recorriendo las páginas de a {@link PAGE_SIZE}.
 *
 * El bot se autentica con la cuenta de servicio de Keycloak, no con la del usuario del chat, así
 * que los endpoints por usuario (`/activities/participants/me`, `/activities/organizers/me`) no
 * sirven: el filtro por usuario se hace acá, con los `participants` y el `organizerId` que ya
 * vienen en cada actividad.
 *
 * @returns todas las actividades, ordenadas de la más próxima a la más lejana.
 */
export async function fetchAllActivities(): Promise<ActivityResponse[]> {
  const activities: ActivityResponse[] = [];
  let page = 0;

  for (; page < MAX_PAGES; page++) {
    const result = await getActivities({ page, size: PAGE_SIZE });
    activities.push(...result.content);
    if (result.last || result.content.length === 0) {
      page++;
      break;
    }
  }

  if (page >= MAX_PAGES) {
    console.log(`[activities] se alcanzó el tope de ${MAX_PAGES} páginas, la lista puede estar incompleta`);
  }

  return sortByClosestDate(activities);
}

/** Ordena de la más próxima a la más lejana, como pide el listado del bot. */
export function sortByClosestDate(activities: ActivityResponse[]): ActivityResponse[] {
  return [...activities].sort((a, b) => a.dateTime.localeCompare(b.dateTime));
}

/** El usuario es organizador, o sea el dueño de la actividad. */
export function isOrganizer(activity: ActivityResponse, userId: string): boolean {
  return activity.organizerId === userId;
}

/** El usuario está entre los participantes. El organizador no se agrega a la lista. */
export function isParticipant(activity: ActivityResponse, userId: string): boolean {
  return activity.participants.some((participant) => participant.userId === userId);
}

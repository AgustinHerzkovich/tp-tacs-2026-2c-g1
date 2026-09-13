// Typed client for this app's own /api/* routes (src/pages/api/**), which in
// turn proxy to the real Spring Boot backend (see src/lib/backendProxy.ts).
// Every call goes through authFetch so it carries a fresh Keycloak access
// token — never call fetch("/api/...") directly from a hook/component.

import { authFetch } from "@/lib/authFetch";
import { beginRequest, endRequest } from "@/lib/loading";
import type {
  ActivityResponse,
  ActivityWeatherResponse,
  NotificationResponse,
  VotationDTO,
} from "@/types/backend";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  beginRequest();
  try {
    const res = await authFetch(`/api${path}`, init);
    if (!res.ok) {
      const body: unknown = await res.json().catch(() => null);
      const message =
        body && typeof body === "object" && "message" in body && typeof body.message === "string"
          ? body.message
          : `Error ${res.status} llamando a ${path}`;
      throw new ApiError(res.status, message);
    }
    if (res.status === 204) return undefined as T;
    return (await res.json()) as T;
  } finally {
    endRequest();
  }
}

function json(method: string, body: unknown): RequestInit {
  return { method, headers: { "content-type": "application/json" }, body: JSON.stringify(body) };
}

export const api = {
  activities: {
    list: () => request<ActivityResponse[]>("/activities"),
    get: (id: string) => request<ActivityResponse>(`/activities/${id}`),
    /** Activities the current user organizes. */
    organized: () => request<ActivityResponse[]>("/activities/organizers/me"),
    /** Activities the current user joined as a participant. */
    mine: () => request<ActivityResponse[]>("/activities/participants/me"),
    join: (id: string) => request<ActivityResponse>(`/activities/${id}/participants/me`, { method: "PUT" }),
    leave: (id: string) => request<ActivityResponse>(`/activities/${id}/participants/me`, { method: "DELETE" }),
    weather: (id: string) => request<ActivityWeatherResponse>(`/activities/${id}/weather`),
    /** `body` is a FormData with an `activity` JSON part and optional `images` file parts. */
    create: (body: FormData) => request<ActivityResponse>("/activities", { method: "POST", body }),
  },
  votations: {
    /** Votations for activities the current user organizes or joined. */
    mine: () => request<VotationDTO[]>("/votations"),
    /** `dateTime` is the chosen option's raw ISO LocalDateTime string. */
    vote: (votationId: string, dateTime: string) =>
      request<VotationDTO>(`/votations/${votationId}/votes/me`, json("PUT", dateTime)),
  },
  notifications: {
    list: () => request<NotificationResponse[]>("/notifications"),
    markRead: (id: string) => request<NotificationResponse>(`/notifications/${id}/read`, { method: "PATCH" }),
  },
};
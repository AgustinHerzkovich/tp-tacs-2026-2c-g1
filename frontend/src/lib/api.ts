// Typed client for this app's own /api/* routes (src/pages/api/**), which in
// turn proxy to the real Spring Boot backend (see src/lib/backendProxy.ts).
// Every call goes through authFetch so it carries a fresh Keycloak access
// token — never call fetch("/api/...") directly from a hook/component.

import { authFetch } from "@/lib/authFetch";
import { errorCodeOf, userMessageFor } from "@/lib/errorMessages";
import { beginRequest, endRequest } from "@/lib/loading";
import type {
  ActivityFilterParams,
  ActivityResponse,
  ActivityWeatherResponse,
  NotificationResponse,
  PageResponse,
  StatisticsResponse,
  UpdateVotationOptionsRequest,
  UpdateVotationSettingsRequest,
  VotationDTO,
  VotationFilterParams,
} from "@/types/backend";

/** A failed API call. `message` is always a user-facing Spanish text (see
 * errorMessages.ts); `code` is the backend's machine-readable error code,
 * for callers that need to branch on the exact reason. */
export class ApiError extends Error {
  status: number;
  code: string | null;

  constructor(status: number, message: string, code: string | null = null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  beginRequest();
  try {
    const res = await authFetch(`/api${path}`, init);
    if (!res.ok) {
      const body: unknown = await res.json().catch(() => null);
      const code = errorCodeOf(body);
      throw new ApiError(res.status, userMessageFor(res.status, code), code);
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

function queryString(
  params?: Record<string, string | number | boolean | string[] | undefined>,
): string {
  if (!params) return "";
  const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== "");
  if (entries.length === 0) return "";
  return `?${new URLSearchParams(entries.map(([key, value]) => [key, String(value)])).toString()}`;
}

export const api = {
  activities: {
    list: (filters?: ActivityFilterParams) =>
      request<PageResponse<ActivityResponse>>(`/activities${queryString({ ...filters })}`),
    get: (id: string) => request<ActivityResponse>(`/activities/${id}`),
    /** Activities the current user organizes. */
    organized: (page = 0, size = 12) =>
      request<PageResponse<ActivityResponse>>(`/activities/organizers/me${queryString({ page, size })}`),
    /** Activities the current user joined as a participant. */
    mine: (page = 0, size = 12) =>
      request<PageResponse<ActivityResponse>>(`/activities/participants/me${queryString({ page, size })}`),
    join: (id: string) => request<ActivityResponse>(`/activities/${id}/participants/me`, { method: "PUT" }),
    leave: (id: string) => request<ActivityResponse>(`/activities/${id}/participants/me`, { method: "DELETE" }),
    weather: (id: string) => request<ActivityWeatherResponse>(`/activities/${id}/weather`),
    /** `body` is a FormData with an `activity` JSON part and optional `images` file parts. */
    create: (body: FormData) => request<ActivityResponse>("/activities", { method: "POST", body }),
  },
  votations: {
    /** One page of the votations of activities the current user organizes or
     * joined, newest first. */
    mine: (filters?: VotationFilterParams) =>
      request<PageResponse<VotationDTO>>(`/votations${queryString({ ...filters })}`),
    /** `dateTime` is the chosen option's raw ISO LocalDateTime string. */
    vote: (votationId: string, dateTime: string) =>
      request<VotationDTO>(`/votations/${votationId}/votes/me`, json("PUT", dateTime)),
    updateOptions: (votationId: string, body: UpdateVotationOptionsRequest) =>
      request<VotationDTO>(`/votations/${votationId}/options`, json("PUT", body)),
    updateSettings: (votationId: string, body: UpdateVotationSettingsRequest) =>
      request<VotationDTO>(`/votations/${votationId}/settings`, json("PUT", body)),
  },
  notifications: {
    list: (page = 0, size = 10) =>
      request<PageResponse<NotificationResponse>>(`/notifications${queryString({ page, size })}`),
    markRead: (id: string) => request<void>(`/notifications/${id}/read`, { method: "PATCH" }),
  },
  statistics: {
    get: (params?: { from?: string; to?: string }) =>
      request<StatisticsResponse>(`/statistics${queryString(params)}`),
  },
};

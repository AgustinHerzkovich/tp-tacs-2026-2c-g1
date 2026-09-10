// Thin typed wrapper over fetch('/api/...'). Every call goes through the
// Next.js proxy routes in src/pages/api/** — never the backend origin
// directly (see src/lib/backendProxy.ts and AGENTS.md).

import type {
  ActivityFilterParams,
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

async function toJson<T>(res: Response): Promise<T> {
  const text = await res.text();
  if (!res.ok) {
    throw new ApiError(res.status, text || `${res.status} ${res.statusText}`);
  }
  return text ? (JSON.parse(text) as T) : (undefined as T);
}

function queryString(params?: ActivityFilterParams): string {
  if (!params) return "";
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== "");
  if (entries.length === 0) return "";
  const search = new URLSearchParams(entries.map(([k, v]) => [k, String(v)]));
  return `?${search.toString()}`;
}

export const api = {
  activities: {
    list: (filters?: ActivityFilterParams) =>
      fetch(`/api/activities${queryString(filters)}`).then((r) => toJson<ActivityResponse[]>(r)),
    get: (id: string) => fetch(`/api/activities/${id}`).then((r) => toJson<ActivityResponse>(r)),
    weather: (id: string) =>
      fetch(`/api/activities/${id}/weather`).then((r) => toJson<ActivityWeatherResponse>(r)),
    mine: () => fetch("/api/activities/participants/me").then((r) => toJson<ActivityResponse[]>(r)),
    organized: () => fetch("/api/activities/organizers/me").then((r) => toJson<ActivityResponse[]>(r)),
    join: (id: string) =>
      fetch(`/api/activities/${id}/participants/me`, { method: "PUT" }).then((r) =>
        toJson<ActivityResponse>(r),
      ),
    leave: (id: string) =>
      fetch(`/api/activities/${id}/participants/me`, { method: "DELETE" }).then((r) =>
        toJson<ActivityResponse>(r),
      ),
    create: (formData: FormData) =>
      fetch("/api/activities", { method: "POST", body: formData }).then((r) =>
        toJson<ActivityResponse>(r),
      ),
  },
  votations: {
    mine: () => fetch("/api/votations").then((r) => toJson<VotationDTO[]>(r)),
    vote: (votationId: string, dateTimeIso: string) =>
      fetch(`/api/votations/${votationId}/votes/me`, {
        method: "PUT",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(dateTimeIso),
      }).then((r) => toJson<VotationDTO>(r)),
  },
  notifications: {
    list: () => fetch("/api/notifications").then((r) => toJson<NotificationResponse[]>(r)),
  },
};

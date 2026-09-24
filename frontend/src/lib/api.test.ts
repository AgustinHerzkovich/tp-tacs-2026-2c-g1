import { beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "@/lib/api";
import type { ActivityResponse, PageResponse } from "@/types/backend";

const { authFetch, beginRequest, endRequest } = vi.hoisted(() => ({
  authFetch: vi.fn<() => Promise<Response>>(),
  beginRequest: vi.fn(),
  endRequest: vi.fn(),
}));

vi.mock("@/lib/authFetch", () => ({ authFetch }));
vi.mock("@/lib/loading", () => ({ beginRequest, endRequest }));

/** authFetch is mocked with no parameters, so its call tuples type as [] —
 *  widen them to the shape request() actually uses. */
function requestArgs(index: number): [string, RequestInit | undefined] {
  return authFetch.mock.calls[index] as unknown as [string, RequestInit | undefined];
}

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });
}

const ACTIVITY: ActivityResponse = {
  id: "a1",
  title: "Vóley",
  description: null,
  type: "OUTDOOR",
  location: { city: "CABA", latitude: null, longitude: null },
  dateTime: "2026-09-20T14:00:00",
  availability: true,
  minParticipants: 4,
  maxParticipants: 12,
  participantCount: 2,
  participants: [],
  weatherConditions: { maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null },
  anticipationWindow: 24,
  reprogramationRange: { maxDays: 3, initialHour: "09:00:00", finalHour: "21:00:00" },
  status: "CONFIRMED",
  imageUrls: [],
  organizerId: "organizer-1",
};

describe("api client", () => {
  beforeEach(() => {
    authFetch.mockReset();
    beginRequest.mockClear();
    endRequest.mockClear();
  });

  it("parses JSON responses and always wraps the call in loading signals", async () => {
    authFetch.mockResolvedValue(jsonResponse(200, ACTIVITY));
    const result = await api.activities.get("a1");
    expect(result).toEqual(ACTIVITY);
    expect(beginRequest).toHaveBeenCalledTimes(1);
    expect(endRequest).toHaveBeenCalledTimes(1);
  });

  it("returns undefined for 204 responses", async () => {
    authFetch.mockResolvedValue(new Response(null, { status: 204 }));
    const result = await api.activities.join("a1");
    expect(result).toBeUndefined();
  });

  it("serializes filters into the querystring and drops empty values", async () => {
    const emptyPage: PageResponse<ActivityResponse> = {
      content: [],
      page: 0,
      size: 12,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    };
    authFetch
      .mockResolvedValueOnce(jsonResponse(200, emptyPage))
      .mockResolvedValueOnce(jsonResponse(200, emptyPage))
      .mockResolvedValueOnce(jsonResponse(200, emptyPage));

    await api.activities.list({ type: "OUTDOOR", city: "CABA", availability: true, dateFrom: "2026-01-01T00:00:00" });
    const [url] = requestArgs(0);
    expect(url).toBe("/api/activities?type=OUTDOOR&city=CABA&availability=true&dateFrom=2026-01-01T00%3A00%3A00");

    await api.activities.list({ status: ["CONFIRMED", "FINISHED"] });
    expect(requestArgs(1)[0]).toBe("/api/activities?status=CONFIRMED%2CFINISHED");

    await api.activities.list({ city: "" });
    expect(requestArgs(2)[0]).toBe("/api/activities");
  });

  it.each([
    [409, "ACTIVITY_FULL", "La actividad ya no tiene lugares disponibles."],
    [403, "NOT_ORGANIZER", "Solo el organizador puede hacer esto."],
    [404, "VOTATION_NOT_FOUND", "No encontramos esta votación."],
    [503, "WEATHER_UNAVAILABLE", "No pudimos consultar el pronóstico. Probá de nuevo en unos minutos."],
  ])("maps the backend error code %i/%s to a Spanish message", async (status, code, message) => {
    authFetch.mockResolvedValue(
      jsonResponse(status, { code, detail: "Activity has no available spots. id=65f0", title: "Conflict" }),
    );
    const promise = api.activities.get("a1");
    await expect(promise).rejects.toSatisfy(
      (err: unknown) =>
        err instanceof ApiError && err.status === status && err.code === code && err.message === message,
    );
  });

  it.each([
    [400, "Revisá los datos ingresados."],
    [401, "Tu sesión expiró. Iniciá sesión de nuevo."],
    [403, "No tenés permiso para hacer esto."],
    [404, "No encontramos lo que buscabas."],
    [409, "No pudimos completar la operación. Probá de nuevo más tarde."],
    [500, "No pudimos completar la operación. Probá de nuevo más tarde."],
  ])("never shows the backend detail and falls back to a message for status %i", async (status, message) => {
    authFetch.mockResolvedValue(jsonResponse(status, { detail: "Provider returned an incomplete forecast range" }));
    await expect(api.activities.get("a1")).rejects.toMatchObject({ status, code: null, message });
  });

  it("falls back to the status message for an unknown error code", async () => {
    authFetch.mockResolvedValue(jsonResponse(404, { code: "SOMETHING_NEW" }));
    await expect(api.activities.get("a1")).rejects.toMatchObject({
      code: "SOMETHING_NEW",
      message: "No encontramos lo que buscabas.",
    });
  });

  it("falls back to a generic message when the error body is not JSON", async () => {
    authFetch.mockResolvedValue(new Response("<html>oops</html>", { status: 500 }));
    await expect(api.activities.get("a1")).rejects.toMatchObject({
      status: 500,
      message: "No pudimos completar la operación. Probá de nuevo más tarde.",
    });
  });

  it("still releases the loading signal when the request fails", async () => {
    authFetch.mockResolvedValue(jsonResponse(503, { message: "caído" }));
    await expect(api.activities.get("a1")).rejects.toBeInstanceOf(ApiError);
    expect(endRequest).toHaveBeenCalledTimes(1);
  });

  it("sends mutation bodies as JSON for votation updates", async () => {
    authFetch.mockResolvedValue(jsonResponse(200, { id: "v1", activityId: "a1", status: "ACTIVE", options: [] }));
    await api.votations.updateOptions("v1", { dates: ["2026-09-21T18:00:00"] });
    const [, init] = requestArgs(0);
    expect(init?.method).toBe("PUT");
    expect(init?.headers).toEqual({ "content-type": "application/json" });
    expect(JSON.parse(String(init?.body))).toEqual({ dates: ["2026-09-21T18:00:00"] });
  });

  it("sends multipart bodies without setting a JSON content-type for activity creation", async () => {
    authFetch.mockResolvedValue(jsonResponse(200, ACTIVITY));
    const form = new FormData();
    form.set("activity", "{}");
    await api.activities.create(form);
    const [, init] = requestArgs(0);
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(form);
    expect(init?.headers).toBeUndefined();
  });
});
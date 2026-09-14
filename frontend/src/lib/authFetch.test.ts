import { beforeEach, describe, expect, it, vi } from "vitest";
import { authFetch, AuthenticationRequiredError } from "@/lib/authFetch";

const fetchMock = vi.fn<typeof fetch>();
vi.stubGlobal("fetch", fetchMock);

const updateToken = vi.fn<() => Promise<boolean>>();
const keycloak = {
  authenticated: true,
  token: "expired-token",
  updateToken,
};

vi.mock("@/lib/keycloak", () => ({
  getKeycloak: () => keycloak,
}));

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
}

describe("authFetch", () => {
  beforeEach(() => {
    fetchMock.mockReset();
    updateToken.mockReset();
    keycloak.authenticated = true;
    keycloak.token = "expired-token";
    updateToken.mockResolvedValue(true);
  });

  it("adds the bearer token from the refreshed Keycloak session", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { ok: true }));
    const response = await authFetch("/api/activities");
    expect(response.status).toBe(200);
    expect(updateToken).toHaveBeenCalledWith(30);
    const [input, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(input).toBe("/api/activities");
    expect(String((init.headers as Headers).get("Authorization"))).toBe("Bearer expired-token");
  });

  it("retries with a forced token refresh when the first attempt returns 401", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(401, { message: "token expirado" }))
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }));

    const response = await authFetch("/api/activities");

    expect(response.status).toBe(200);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(updateToken).toHaveBeenNthCalledWith(1, 30);
    expect(updateToken).toHaveBeenNthCalledWith(2, -1);
  });

  it("throws AuthenticationRequiredError when refresh fails", async () => {
    keycloak.authenticated = true;
    updateToken.mockRejectedValue(new Error("refresh failed"));
    await expect(authFetch("/api/activities")).rejects.toBeInstanceOf(AuthenticationRequiredError);
  });

  it("throws AuthenticationRequiredError when there is no active session", async () => {
    keycloak.authenticated = false;
    await expect(authFetch("/api/activities")).rejects.toBeInstanceOf(AuthenticationRequiredError);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("throws AuthenticationRequiredError when no access token is available", async () => {
    keycloak.authenticated = true;
    keycloak.token = undefined as unknown as string;
    await expect(authFetch("/api/activities")).rejects.toBeInstanceOf(AuthenticationRequiredError);
  });
});
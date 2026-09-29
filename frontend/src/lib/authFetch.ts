import { getKeycloak } from "@/lib/keycloak";

export class AuthenticationRequiredError extends Error {
  constructor() {
    super("Tu sesión expiró. Iniciá sesión de nuevo.");
    this.name = "AuthenticationRequiredError";
  }
}

async function currentAccessToken(forceRefresh = false): Promise<string> {
  const keycloak = getKeycloak();
  if (!keycloak.authenticated) throw new AuthenticationRequiredError();

  try {
    await keycloak.updateToken(forceRefresh ? -1 : 30);
  } catch {
    throw new AuthenticationRequiredError();
  }

  if (!keycloak.token) throw new AuthenticationRequiredError();
  return keycloak.token;
}

/** Calls a same-origin API route with a fresh Keycloak access token and the
 * caller's IANA time zone, so the backend can judge date/time fields (e.g. "is
 * this activity's start in the future?") against the user's own clock instead
 * of the server's. */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const send = async (forceRefresh: boolean) => {
    const headers = new Headers(init?.headers);
    headers.set("Authorization", `Bearer ${await currentAccessToken(forceRefresh)}`);
    headers.set("X-Time-Zone", Intl.DateTimeFormat().resolvedOptions().timeZone);
    return fetch(input, { ...init, headers });
  };

  const response = await send(false);
  return response.status === 401 ? send(true) : response;
}

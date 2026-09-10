import { getKeycloak } from "@/lib/keycloak";

export class AuthenticationRequiredError extends Error {
  constructor() {
    super("A valid Keycloak session is required");
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

/** Calls a same-origin API route with a fresh Keycloak access token. */
export async function authFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  const send = async (forceRefresh: boolean) => {
    const headers = new Headers(init?.headers);
    headers.set("Authorization", `Bearer ${await currentAccessToken(forceRefresh)}`);
    return fetch(input, { ...init, headers });
  };

  const response = await send(false);
  return response.status === 401 ? send(true) : response;
}

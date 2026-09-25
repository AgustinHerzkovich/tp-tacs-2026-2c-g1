import {
  backendClient,
  keycloakClient,
  keycloakRealm,
  telegramApiToken,
  telegramBotClientId,
  telegramBotClientSecret,
} from "./consts";

const API_TOKEN_HEADER = "X-Api-Token";
const EXPIRY_SKEW_MS = 30_000;

let cachedToken: { value: string; expiresAt: number } | undefined;
let pendingToken: Promise<string> | undefined;

interface TokenResponse {
  access_token?: string;
  expires_in?: number;
  error?: string;
  error_description?: string;
}

/**
 * Devuelve el access token de la cuenta de servicio del bot, emitido por Keycloak con
 * client_credentials. Se pide con el client secret de solnotfoundTelegramBot y se cachea en el
 * contexto de la funcion hasta su vencimiento, para no pedir un token por cada comando.
 */
export async function getAccessToken(): Promise<string> {
  if (!telegramBotClientSecret) {
    throw new Error("Falta KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET para pedir el token de Keycloak");
  }

  if (cachedToken && cachedToken.expiresAt > Date.now()) {
    return cachedToken.value;
  }

  pendingToken = pendingToken ?? requestAccessToken();
  try {
    return await pendingToken;
  } finally {
    pendingToken = undefined;
  }
}

async function requestAccessToken(): Promise<string> {
  const url = `${keycloakClient}/realms/${keycloakRealm}/protocol/openid-connect/token`;
  const body = new URLSearchParams({
    grant_type: "client_credentials",
    client_id: telegramBotClientId,
    client_secret: telegramBotClientSecret,
  });

  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body,
  });

  const payload = (await response.json().catch(() => ({}))) as TokenResponse;
  if (!response.ok || !payload.access_token) {
    const reason = payload.error_description ?? payload.error ?? `HTTP ${response.status}`;
    throw new Error(`Keycloak no emitio un access token para ${telegramBotClientId}: ${reason}`);
  }

  // El margen de expiracion cubre el reloj de la funcion y el viaje hasta el backend.
  const expiresInMs = Math.max((payload.expires_in ?? 60) * 1000 - EXPIRY_SKEW_MS, 0);
  cachedToken = { value: payload.access_token, expiresAt: Date.now() + expiresInMs };
  console.log("[authToken] access token de Keycloak cacheado para el bot");

  return payload.access_token;
}

/**
 * Llama al backend con las dos medidas de seguridad: el JWT de la cuenta de servicio de Keycloak
 * (Bearer) y el API token del bot (X-Api-Token). El backend exige ambas, por lo que un token
 * filtrado no alcanza para llamar a la API. Si el backend rechaza el JWT cacheado, se pide uno
 * nuevo y se reintenta una vez.
 */
export async function backendFetch(path: string, init: RequestInit = {}): Promise<Response> {
  if (!telegramApiToken) {
    throw new Error("Falta TELEGRAM_API_TOKEN: el backend rechaza las llamadas del bot sin el API token");
  }

  const send = async (accessToken: string): Promise<Response> =>
    fetch(`${backendClient}${path}`, {
      ...init,
      headers: {
        ...init.headers,
        Authorization: `Bearer ${accessToken}`,
        [API_TOKEN_HEADER]: telegramApiToken,
      },
    });

  const response = await send(await getAccessToken());
  if (response.status === 401) {
    cachedToken = undefined;
    const retried = await send(await getAccessToken());
    console.log(`[authToken] backend respondio 401, se reintento con un token nuevo (HTTP ${retried.status})`);
    return retried;
  }

  return response;
}

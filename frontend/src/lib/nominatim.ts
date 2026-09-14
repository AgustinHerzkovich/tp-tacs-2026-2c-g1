const NOMINATIM_URL = "https://nominatim.openstreetmap.org";
const CACHE_TTL_MS = 10 * 60 * 1000;
const MIN_REQUEST_INTERVAL_MS = 1000;
const REQUEST_TIMEOUT_MS = 8000;

interface CacheEntry {
  expiresAt: number;
  value: unknown;
}

const cache = new Map<string, CacheEntry>();
let lastRequestAt = 0;
let requestQueue = Promise.resolve();

export class NominatimError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = "NominatimError";
  }
}

/** Serializes public Nominatim calls, caches responses, and bounds request time. */
export async function fetchNominatim<T>(path: string, params: URLSearchParams): Promise<T> {
  const key = `${path}?${params.toString()}`;
  const cached = cache.get(key);
  if (cached && cached.expiresAt > Date.now()) return cached.value as T;
  cache.delete(key);

  const result = requestQueue.then(async () => {
    const wait = Math.max(0, MIN_REQUEST_INTERVAL_MS - (Date.now() - lastRequestAt));
    if (wait > 0) await new Promise((resolve) => setTimeout(resolve, wait));

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
    lastRequestAt = Date.now();
    try {
      const response = await fetch(`${NOMINATIM_URL}${path}?${params.toString()}`, {
        headers: { "User-Agent": "Planazo-TACS/1.0 (educational project)" },
        signal: controller.signal,
      });
      if (response.status === 429) throw new NominatimError(429, "Demasiadas búsquedas. Intentá nuevamente en unos segundos.");
      if (!response.ok) throw new NominatimError(502, "No se pudo consultar OpenStreetMap.");
      const value = (await response.json()) as T;
      cache.set(key, { value, expiresAt: Date.now() + CACHE_TTL_MS });
      return value;
    } catch (error) {
      if (error instanceof NominatimError) throw error;
      if (error instanceof Error && error.name === "AbortError") {
        throw new NominatimError(504, "OpenStreetMap tardó demasiado en responder.");
      }
      throw new NominatimError(502, "No se pudo consultar OpenStreetMap.");
    } finally {
      clearTimeout(timeout);
    }
  });

  requestQueue = result.then(() => undefined, () => undefined);
  return result;
}

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

export interface NominatimAddress {
  city?: string;
  town?: string;
  village?: string;
  municipality?: string;
  city_district?: string;
  suburb?: string;
  county?: string;
  state_district?: string;
  state?: string;
}

/** Narrow-to-broad order used to build the two-level locality below. `suburb`
 * comes before `city` on purpose: in CABA, Nominatim tags the neighbourhood
 * as `suburb` ("San Nicolás") *and* the city as `city` ("Buenos Aires") —
 * `suburb` is the narrower one there. Outside the autonomous cities (most of
 * Buenos Aires province), there's no `city` at all, just `suburb` for the
 * town-like locality itself ("Munro") with `state_district` above it
 * ("Partido de Vicente López") — same two fields, same order, right result
 * either way. */
const LOCALITY_FIELDS = [
  "suburb",
  "city_district",
  "city",
  "town",
  "village",
  "municipality",
  "county",
  "state_district",
  "state",
] as const satisfies readonly (keyof NominatimAddress)[];

/** Best-effort "generic locality, one level of context" for a Nominatim
 * result's structured address — e.g. "Munro, Partido de Vicente López"
 * rather than just "Munro" (too generic to search by reliably) or the full
 * address down to postcode/country (too specific to match a differently
 * formatted stored address — see the `city` filter in `ActivityRepository`,
 * which does a literal contains match against the full address string).
 * Two levels, picked narrow-to-broad, is the middle ground. */
export function municipalityOf(address: NominatimAddress | undefined): string {
  if (!address) return "";
  const levels: string[] = [];
  for (const field of LOCALITY_FIELDS) {
    const value = address[field];
    if (value && !levels.includes(value)) levels.push(value);
    if (levels.length === 2) break;
  }
  return levels.join(", ");
}

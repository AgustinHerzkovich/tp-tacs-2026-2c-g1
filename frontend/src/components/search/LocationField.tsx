"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { AlertCircle, Compass, Loader2, LocateFixed, MapPin } from "lucide-react";
import { Input } from "@/components/ui/input";

/** Same shape the `/api/geocoding/*` routes return — same source the wizard's
 * `LocationMap` uses (`src/components/pages/wizard/LocationMap.tsx`), plus
 * `city` (the Nominatim-structured municipality, added to those routes for
 * this field so selecting a result sets a clean municipality name rather
 * than a full street address as the `city` filter). */
interface GeocodeResult {
  label: string;
  latitude: number;
  longitude: number;
  city: string;
}

/** Shown before any search (3, not a long static list) — plus "Tu ubicación
 * actual" via geolocation, always first. */
const SUGGESTED_CITIES = ["Buenos Aires", "Córdoba", "Rosario"];

interface LocationFieldContentProps {
  city: string;
  onCityChange: (value: string) => void;
  onDone?: () => void;
}

export function LocationFieldContent({ city, onCityChange, onDone }: LocationFieldContentProps) {
  const [results, setResults] = useState<GeocodeResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const debounceTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const search = useCallback(async (query: string) => {
    setLoading(true);
    setError(null);
    abortControllerRef.current?.abort();
    const controller = new AbortController();
    abortControllerRef.current = controller;
    try {
      const response = await fetch(`/api/geocoding/search?q=${encodeURIComponent(query)}`, { signal: controller.signal });
      if (!response.ok) {
        throw new Error(response.status === 429 ? "Demasiadas búsquedas. Esperá unos segundos." : "No se pudo buscar la ubicación.");
      }
      const data = (await response.json()) as GeocodeResult[];
      if (abortControllerRef.current === controller) setResults(data);
    } catch (err) {
      if (err instanceof Error && err.name === "AbortError") return;
      if (abortControllerRef.current === controller) {
        setError(err instanceof Error ? err.message : "Ocurrió un error al buscar la ubicación.");
        setResults([]);
      }
    } finally {
      if (abortControllerRef.current === controller) setLoading(false);
    }
  }, []);

  const searchable = city.trim().length >= 3;

  // Debounced live search, same 3-char/500ms rule as the wizard's LocationMap.
  // Below 3 chars there's simply nothing scheduled — `searchable` alone gates
  // what's rendered, so stale `results`/`error` from a longer query never
  // need a synchronous clear here (which upset the set-state-in-effect rule).
  useEffect(() => {
    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    if (!searchable) return;
    debounceTimerRef.current = setTimeout(() => void search(city), 500);
    return () => {
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- only the text should retrigger the search
  }, [city]);

  useEffect(() => () => abortControllerRef.current?.abort(), []);

  const visibleResults = searchable ? results : [];
  const visibleError = searchable ? error : null;

  const choose = (municipality: string) => {
    onCityChange(municipality);
    setResults([]);
    onDone?.();
  };

  const useCurrentLocation = () => {
    if (!navigator.geolocation) {
      setLocationError("Tu navegador no soporta geolocalización.");
      return;
    }
    setLocationLoading(true);
    setLocationError(null);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const response = await fetch(`/api/geocoding/reverse?lat=${position.coords.latitude}&lon=${position.coords.longitude}`);
          const body = response.ok ? ((await response.json()) as { city: string }) : null;
          if (body?.city) {
            choose(body.city);
          } else {
            setLocationError("No pudimos determinar tu municipio.");
          }
        } catch {
          setLocationError("No se pudo obtener tu ubicación actual.");
        } finally {
          setLocationLoading(false);
        }
      },
      (err) => {
        setLocationLoading(false);
        setLocationError(
          err.code === 1
            ? "Permiso de ubicación denegado. Habilitalo en tu navegador."
            : "No se pudo obtener tu ubicación actual.",
        );
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 },
    );
  };

  return (
    <div className="flex flex-col gap-3">
      <div className="relative">
        <MapPin className="absolute left-3.5 top-1/2 -translate-y-1/2 size-4" style={{ color: "var(--muted-foreground)" }} />
        <Input
          autoFocus
          value={city}
          onChange={(event) => onCityChange(event.target.value)}
          placeholder="Buscar por ciudad o dirección"
          className="pl-10 pr-9 rounded-xl bg-white"
        />
        {loading && (
          <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 size-4 animate-spin" style={{ color: "var(--muted-foreground)" }} />
        )}
      </div>

      {visibleError && (
        <p className="flex items-center gap-1.5 text-[12px] font-bold" style={{ color: "var(--destructive)" }}>
          <AlertCircle className="size-3.5 shrink-0" /> {visibleError}
        </p>
      )}
      {locationError && (
        <p className="flex items-center gap-1.5 text-[12px] font-bold" style={{ color: "var(--destructive)" }}>
          <AlertCircle className="size-3.5 shrink-0" /> {locationError}
        </p>
      )}

      {visibleResults.length > 0 ? (
        <ul className="flex flex-col max-h-64 overflow-y-auto" role="listbox" aria-label="Resultados de búsqueda">
          {visibleResults.map((result) => (
            <li key={`${result.latitude}-${result.longitude}`}>
              <button
                type="button"
                onClick={() => choose(result.city || result.label)}
                className="tap w-full flex items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-muted"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-full" style={{ background: "var(--secondary)" }}>
                  <MapPin className="size-4" style={{ color: "var(--secondary-foreground)" }} />
                </span>
                <span className="min-w-0 truncate text-[13.5px] font-semibold">{result.label}</span>
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <div>
          <p className="px-1 mb-1.5 text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>
            Sugeridos
          </p>
          <ul className="flex flex-col">
            <li>
              <button
                type="button"
                onClick={useCurrentLocation}
                disabled={locationLoading}
                className="tap w-full flex items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-muted disabled:opacity-60"
              >
                <span className="flex size-9 shrink-0 items-center justify-center rounded-full" style={{ background: "var(--sky)" }}>
                  {locationLoading ? (
                    <Loader2 className="size-4 animate-spin" style={{ color: "var(--sky-ink)" }} />
                  ) : (
                    <LocateFixed className="size-4" style={{ color: "var(--sky-ink)" }} />
                  )}
                </span>
                <span className="text-[13.5px] font-semibold">En tu zona</span>
              </button>
            </li>
            {SUGGESTED_CITIES.map((suggestion) => (
              <li key={suggestion}>
                <button
                  type="button"
                  onClick={() => choose(suggestion)}
                  className="tap w-full flex items-center gap-3 rounded-xl px-2 py-2 text-left hover:bg-muted"
                >
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-full" style={{ background: "var(--secondary)" }}>
                    <Compass className="size-4" style={{ color: "var(--secondary-foreground)" }} />
                  </span>
                  <span className="text-[13.5px] font-semibold">{suggestion}</span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

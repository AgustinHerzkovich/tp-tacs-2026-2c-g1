"use client";

import dynamic from "next/dynamic";
import { useState, useCallback, useRef, useEffect } from "react";
import { Search, AlertCircle, Loader2, Navigation } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export interface LocationOption {
  label: string;
  latitude: number;
  longitude: number;
}

interface LocationMapProps {
  place: string;
  latitude: number | null;
  longitude: number | null;
  onChange: (location: LocationOption) => void;
  onQueryChange: (query: string) => void;
  invalid: boolean;
}

const LeafletMap = dynamic(() => import("@/components/pages/wizard/LeafletMap"), {
  ssr: false,
  loading: () => <div className="h-64 mt-3 rounded-2xl bg-muted animate-pulse" />,
});

export function LocationMap({ place, latitude, longitude, onChange, onQueryChange, invalid }: LocationMapProps) {
  const [results, setResults] = useState<LocationOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showNoResults, setShowNoResults] = useState(false);
  const [locationLoading, setLocationLoading] = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const debounceTimerRef = useRef<NodeJS.Timeout | null>(null);

  const search = useCallback(async (signal?: AbortSignal) => {
    if (place.trim().length < 3) {
      setResults([]);
      setShowNoResults(false);
      setError(null);
      return;
    }

    setLoading(true);
    setError(null);
    setShowNoResults(false);

    try {
      const controller = new AbortController();
      abortControllerRef.current = controller;
      const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s timeout

      const response = await fetch(`/api/geocoding/search?q=${encodeURIComponent(place)}`, {
        signal: signal || controller.signal,
      });

      clearTimeout(timeoutId);

      if (!response.ok) {
        if (response.status === 429) {
          throw new Error("Demasiadas búsquedas. Esperá unos segundos antes de volver a intentar.");
        }
        throw new Error("No se pudo buscar la ubicación.");
      }

      const data = (await response.json()) as LocationOption[];
      setResults(data);
      setShowNoResults(data.length === 0);
    } catch (err) {
      if (err instanceof Error) {
        if (err.name === "AbortError") {
          // Request was cancelled, don't show error
          return;
        }
        setError(err.message);
      } else {
        setError("Ocurrió un error al buscar la ubicación.");
      }
      setResults([]);
      setShowNoResults(false);
    } finally {
      setLoading(false);
    }
  }, [place]);

  // Debounced search on input change
  useEffect(() => {
    if (debounceTimerRef.current) {
      clearTimeout(debounceTimerRef.current);
    }

    if (place.trim().length >= 3) {
      debounceTimerRef.current = setTimeout(() => {
        void search();
      }, 500); // 500ms debounce
    } else {
      setResults([]);
      setShowNoResults(false);
      setError(null);
    }

    return () => {
      if (debounceTimerRef.current) {
        clearTimeout(debounceTimerRef.current);
      }
    };
  }, [place, search]);

  // Cleanup abort controller on unmount
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }
    };
  }, []);

  const choose = (location: LocationOption) => {
    onChange(location);
    setResults([]);
    setShowNoResults(false);
    setError(null);
  };

  const handleManualSearch = () => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    void search();
  };

  const handleUseCurrentLocation = () => {
    if (!navigator.geolocation) {
      setLocationError("Tu navegador no soporta geolocalización.");
      return;
    }

    setLocationLoading(true);
    setLocationError(null);

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const { latitude: lat, longitude: lng } = position.coords;
        
        try {
          const response = await fetch(`/api/geocoding/reverse?lat=${lat}&lon=${lng}`);
          const body = response.ok ? ((await response.json()) as { label: string }) : null;
          onChange({ 
            label: body?.label ?? `${lat.toFixed(5)}, ${lng.toFixed(5)}`, 
            latitude: lat, 
            longitude: lng 
          });
          onQueryChange(body?.label ?? "");
          setLocationError(null);
        } catch (err) {
          setLocationError("No se pudo obtener la dirección de tu ubicación actual.");
        } finally {
          setLocationLoading(false);
        }
      },
      (err) => {
        setLocationLoading(false);
        if (err.code === 1) {
          setLocationError("Permiso de ubicación denegado. Habilitá la geolocalización en tu navegador.");
        } else if (err.code === 2) {
          setLocationError("No se pudo determinar tu ubicación. Verificá que el GPS esté activo.");
        } else if (err.code === 3) {
          setLocationError("La solicitud de ubicación expiró. Intentá nuevamente.");
        } else {
          setLocationError("Ocurrió un error al obtener tu ubicación.");
        }
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0,
      }
    );
  };

  return (
    <div>
      <div className="flex gap-2">
        <Input
          value={place}
          onChange={(event) => onQueryChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              void handleManualSearch();
            }
          }}
          aria-invalid={invalid}
          placeholder="Buscar dirección o lugar…"
          className="h-auto py-3.5 rounded-2xl border-2 text-[15px]"
        />
        <Button type="button" variant="outline" size="icon" className="size-12 rounded-2xl shrink-0" onClick={handleManualSearch} disabled={loading} aria-label="Buscar ubicación">
          {loading ? <Loader2 className="size-4 animate-spin" /> : <Search className="size-4" />}
        </Button>
        <Button 
          type="button" 
          variant="outline" 
          size="icon" 
          className="size-12 rounded-2xl shrink-0" 
          onClick={handleUseCurrentLocation} 
          disabled={locationLoading}
          aria-label="Usar mi ubicación actual"
          title="Usar mi ubicación actual"
        >
          {locationLoading ? <Loader2 className="size-4 animate-spin" /> : <Navigation className="size-4" />}
        </Button>
      </div>
      {error && (
        <div className="relative z-[1001] mt-2 rounded-2xl border-2 bg-destructive/10 overflow-hidden shadow-lg" role="alert" aria-live="polite">
          <div className="flex items-center gap-2 px-3 py-2.5 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
            <AlertCircle className="size-4" aria-hidden="true" />
            {error}
          </div>
        </div>
      )}
      {locationError && (
        <div className="relative z-[1001] mt-2 rounded-2xl border-2 bg-destructive/10 overflow-hidden shadow-lg" role="alert" aria-live="polite">
          <div className="flex items-center gap-2 px-3 py-2.5 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
            <AlertCircle className="size-4" aria-hidden="true" />
            {locationError}
          </div>
        </div>
      )}
      {showNoResults && !loading && !error && (
        <div className="relative z-[1001] mt-2 rounded-2xl border-2 bg-muted overflow-hidden shadow-lg">
          <div className="px-3 py-2.5 text-xs font-bold" style={{ color: "var(--muted-foreground)" }}>
            No encontramos resultados para esa búsqueda. Probá con otra dirección o lugar.
          </div>
        </div>
      )}
      {results.length > 0 && (
        <div className="relative z-[1001] mt-2 rounded-2xl border-2 bg-white overflow-hidden shadow-lg" role="listbox" aria-label="Resultados de búsqueda">
          {results.map((result) => (
            <button 
              key={`${result.latitude}-${result.longitude}`} 
              type="button" 
              onClick={() => choose(result)} 
              className="block w-full text-left px-3 py-2.5 text-xs font-bold border-b last:border-0 hover:bg-muted focus:bg-muted focus:outline-none focus:ring-2 focus:ring-inset focus:ring-[var(--primary)]"
              role="option"
              aria-label={`Seleccionar ${result.label}`}
            >
              {result.label}
            </button>
          ))}
        </div>
      )}
      {latitude !== null && longitude !== null && (
        <div className="mt-2 text-[11px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          📍 {place || "Ubicación seleccionada"} · {latitude.toFixed(5)}, {longitude.toFixed(5)}
        </div>
      )}
      <LeafletMap 
        latitude={latitude} 
        longitude={longitude} 
        onChange={choose} 
        onUseCurrentLocation={handleUseCurrentLocation}
      />
    </div>
  );
}

"use client";

import dynamic from "next/dynamic";
import { useState } from "react";
import { Search } from "lucide-react";
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

  const search = async () => {
    if (place.trim().length < 3) return;
    setLoading(true);
    try {
      const response = await fetch(`/api/geocoding/search?q=${encodeURIComponent(place)}`);
      setResults(response.ok ? ((await response.json()) as LocationOption[]) : []);
    } finally {
      setLoading(false);
    }
  };

  const choose = (location: LocationOption) => {
    onChange(location);
    setResults([]);
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
              void search();
            }
          }}
          aria-invalid={invalid}
          placeholder="Buscar dirección o lugar…"
          className="h-auto py-3.5 rounded-2xl border-2 text-[15px]"
        />
        <Button type="button" variant="outline" size="icon" className="size-12 rounded-2xl shrink-0" onClick={() => void search()} disabled={loading} aria-label="Buscar ubicación">
          <Search className="size-4" />
        </Button>
      </div>
      {results.length > 0 && (
        <div className="relative z-[1001] mt-2 rounded-2xl border-2 bg-white overflow-hidden shadow-lg">
          {results.map((result) => (
            <button key={`${result.latitude}-${result.longitude}`} type="button" onClick={() => choose(result)} className="block w-full text-left px-3 py-2.5 text-xs font-bold border-b last:border-0 hover:bg-muted">{result.label}</button>
          ))}
        </div>
      )}
      <LeafletMap latitude={latitude} longitude={longitude} onChange={choose} />
    </div>
  );
}

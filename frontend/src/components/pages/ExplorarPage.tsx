"use client";

import { useEffect, useMemo, useState } from "react";
import { Search } from "lucide-react";
import { useActivities } from "@/hooks/useActivities";
import { ExploreCard } from "@/components/activities/ExploreCard";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { ActivityFilterParams, ActivityType } from "@/types/backend";
import { ErrorState, LoadingState } from "@/components/common/AsyncState";
import { PageControls } from "@/components/common/PageControls";

const FILTERS = ["Todo", "Outdoor", "Indoor", "Mixto", "Hoy"] as const;
const TYPES: Record<(typeof FILTERS)[number], ActivityType | undefined> = {
  Todo: undefined,
  Outdoor: "OUTDOOR",
  Indoor: "INDOOR",
  Mixto: "MIXED",
  Hoy: undefined,
};

export function ExplorarPage() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("Todo");
  const [city, setCity] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [debouncedCity, setDebouncedCity] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedCity(city), 400);
    return () => window.clearTimeout(timer);
  }, [city]);

  const today = new Date();
  const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
  const effectiveFrom = filter === "Hoy" ? todayKey : dateFrom;
  const effectiveTo = filter === "Hoy" ? todayKey : dateTo;
  const invalidDates = Boolean(effectiveFrom && effectiveTo && effectiveFrom > effectiveTo);

  const applied = useMemo<ActivityFilterParams>(
    () => ({
      type: TYPES[filter],
      city: debouncedCity.trim() || undefined,
      dateFrom: invalidDates || !effectiveFrom ? undefined : `${effectiveFrom}T00:00:00`,
      dateTo: invalidDates || !effectiveTo ? undefined : `${effectiveTo}T23:59:59`,
      availability: onlyAvailable || undefined,
    }),
    [debouncedCity, effectiveFrom, effectiveTo, filter, invalidDates, onlyAvailable],
  );

  const [prevApplied, setPrevApplied] = useState(applied);
  const { exploreFeed, loading, error, refresh, explorePage, exploreTotalPages, setExplorePage } = useActivities(applied);
  if (prevApplied !== applied) {
    setPrevApplied(applied);
    setExplorePage(0);
  }

  const results = exploreFeed.filter((a) => {
    const matchesQuery = a.title.toLowerCase().includes(query.toLowerCase());
    return matchesQuery;
  });

  const clearFilters = () => {
    setFilter("Todo");
    setCity("");
    setDateFrom("");
    setDateTo("");
    setOnlyAvailable(false);
  };

  return (
    <div className="fade-in px-5 pt-2 pb-6 lg:px-10 lg:pt-8">
      <div className="lg:flex lg:items-end lg:justify-between lg:gap-8 lg:mb-8">
        <div className="hidden lg:block">
          <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Planes disponibles</p>
          <h2 className="font-display font-semibold text-4xl">Encontrá tu próximo plan</h2>
        </div>
      <div className="relative mb-3 lg:mb-0 lg:w-[420px]">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 size-4" style={{ color: "var(--muted-foreground)" }} />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar actividades…"
          className="w-full rounded-2xl border-2 pl-11 pr-4 py-3 font-body font-semibold text-[14px] outline-none bg-white"
          style={{ borderColor: "var(--border)" }}
        />
      </div>
      </div>
      <div className="flex gap-2 overflow-x-auto pb-1 -mx-5 px-5 mb-5 lg:mx-0 lg:px-0">
        {FILTERS.map((f) => {
          const active = filter === f;
          return (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className="tap shrink-0 px-4 py-2 rounded-full text-[12.5px] font-extrabold border-2 whitespace-nowrap"
              style={
                active
                  ? { background: "var(--primary)", color: "var(--primary-foreground)", borderColor: "var(--primary)" }
                  : { background: "#fff", color: "var(--muted-foreground)", borderColor: "var(--border)" }
              }
            >
              {f}
            </button>
          );
        })}
      </div>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
        <Input value={city} onChange={(event) => setCity(event.target.value)} placeholder="Ciudad" className="col-span-2 lg:col-span-1 rounded-xl bg-white" />
        <Input type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} disabled={filter === "Hoy"} aria-label="Fecha desde" className="rounded-xl bg-white" />
        <Input type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} disabled={filter === "Hoy"} aria-label="Fecha hasta" aria-invalid={invalidDates} className="rounded-xl bg-white" />
        <label className="flex items-center gap-2 text-xs font-extrabold px-3">
          <input type="checkbox" checked={onlyAvailable} onChange={(event) => setOnlyAvailable(event.target.checked)} className="size-4 accent-[var(--primary)]" /> Con cupo
        </label>
      </div>
      {invalidDates && (
        <p className="px-4 mt-2 mb-4 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
          La fecha desde no puede ser posterior a la fecha hasta.
        </p>
      )}
      <div className="flex gap-2 mb-6">
        <Button type="button" variant="outline" className="flex-1 rounded-xl" onClick={clearFilters}>Limpiar</Button>
      </div>
      {loading && <LoadingState label="Cargando actividades..." />}
      {error && (
        <ErrorState message={error} retry={refresh} />
      )}
      {!loading && !error && (
        <>
          <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-5">
            {results.map((a) => <ExploreCard key={a.id} activity={a} />)}
          </div>
          <PageControls page={explorePage} totalPages={exploreTotalPages} onPageChange={setExplorePage} />
        </>
      )}
      {!loading && !error && results.length === 0 && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          No encontramos actividades con esos filtros.
        </p>
      )}
    </div>
  );
}

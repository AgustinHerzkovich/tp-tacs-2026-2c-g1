"use client";

import { useEffect, useMemo, useState } from "react";
import { useActivities } from "@/hooks/useActivities";
import { ExploreCard } from "@/components/activities/ExploreCard";
import { Button } from "@/components/ui/button";
import type { ActivityFilterParams, ActivityType } from "@/types/backend";
import { ErrorState } from "@/components/common/AsyncState";
import { ExploreGridSkeleton } from "@/components/common/Skeletons";
import { PageControls } from "@/components/common/PageControls";
import { SearchBar } from "@/components/search/SearchBar";

const CATEGORIES = [
  { key: "Todo", emoji: "✨", type: undefined },
  { key: "Outdoor", emoji: "🏕️", type: "OUTDOOR" as ActivityType },
  { key: "Indoor", emoji: "🏠", type: "INDOOR" as ActivityType },
  { key: "Mixto", emoji: "🎉", type: "MIXED" as ActivityType },
] as const;

export function ExplorarPage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<(typeof CATEGORIES)[number]["key"]>("Todo");
  const [city, setCity] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [onlyAvailable, setOnlyAvailable] = useState(false);
  const [debouncedCity, setDebouncedCity] = useState("");

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedCity(city), 400);
    return () => window.clearTimeout(timer);
  }, [city]);

  const type = CATEGORIES.find((c) => c.key === category)?.type;
  const invalidDates = Boolean(dateFrom && dateTo && dateFrom > dateTo);

  const applied = useMemo<ActivityFilterParams>(
    () => ({
      type,
      city: debouncedCity.trim() || undefined,
      dateFrom: invalidDates || !dateFrom ? undefined : `${dateFrom}T00:00:00`,
      dateTo: invalidDates || !dateTo ? undefined : `${dateTo}T23:59:59`,
      availability: onlyAvailable || undefined,
    }),
    [debouncedCity, dateFrom, dateTo, invalidDates, onlyAvailable, type],
  );

  const { exploreFeed, loading, error, refresh, explorePage, exploreTotalPages, setExplorePage } = useActivities(applied);

  useEffect(() => {
    setExplorePage(0);
  }, [applied, setExplorePage]);

  const results = exploreFeed.filter((a) => a.title.toLowerCase().includes(query.toLowerCase()));

  const clearFilters = () => {
    setQuery("");
    setCategory("Todo");
    setCity("");
    setDateFrom("");
    setDateTo("");
    setOnlyAvailable(false);
  };

  return (
    <div className="fade-in px-5 pt-2 pb-6 lg:px-10 lg:pt-8">
      <div className="hidden lg:block lg:mb-6">
        <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Planes disponibles</p>
        <h2 className="font-display font-semibold text-4xl">Encontrá tu próximo plan</h2>
      </div>

      <div className="mb-4">
        <SearchBar
          query={query}
          onQueryChange={setQuery}
          city={city}
          onCityChange={setCity}
          dateFrom={dateFrom}
          dateTo={dateTo}
          onDateRangeChange={({ dateFrom: from, dateTo: to }) => { setDateFrom(from); setDateTo(to); }}
          onlyAvailable={onlyAvailable}
          onOnlyAvailableChange={setOnlyAvailable}
          onClear={clearFilters}
          invalidDates={invalidDates}
          typeLabel={category}
        />
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1 -mx-5 px-5 mb-5 lg:mx-0 lg:px-0">
        {CATEGORIES.map(({ key, emoji }) => {
          const active = category === key;
          return (
            <button
              key={key}
              onClick={() => setCategory(key)}
              aria-pressed={active}
              className="tap shrink-0 flex items-center gap-1.5 px-4 py-2 rounded-full text-[12.5px] font-extrabold border-2 whitespace-nowrap"
              style={
                active
                  ? { background: "var(--primary)", color: "var(--primary-foreground)", borderColor: "var(--primary)" }
                  : { background: "#fff", color: "var(--muted-foreground)", borderColor: "var(--border)" }
              }
            >
              <span className="emoji-3d" aria-hidden="true">{emoji}</span> {key}
            </button>
          );
        })}
      </div>

      {loading && <ExploreGridSkeleton />}
      {error && (
        <ErrorState message={error} retry={refresh} />
      )}
      {!loading && !error && (
        <>
          <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-5">
            {results.map((a) => <ExploreCard key={a.id} activity={a} onRefreshImages={refresh} />)}
          </div>
          <PageControls page={explorePage} totalPages={exploreTotalPages} onPageChange={setExplorePage} />
        </>
      )}
      {!loading && !error && results.length === 0 && (
        <div className="text-center py-10">
          <p className="text-[13px] font-bold" style={{ color: "var(--muted-foreground)" }}>
            No encontramos actividades con esos filtros.
          </p>
          <Button type="button" variant="outline" className="mt-3 rounded-xl" onClick={clearFilters}>
            Limpiar filtros
          </Button>
        </div>
      )}
    </div>
  );
}

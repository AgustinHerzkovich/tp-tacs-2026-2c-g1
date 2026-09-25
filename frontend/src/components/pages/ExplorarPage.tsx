"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useActivities } from "@/hooks/useActivities";
import { ExploreCard } from "@/components/activities/ExploreCard";
import { ActivityRail } from "@/components/activities/ActivityRail";
import { Button } from "@/components/ui/button";
import type { ActivityFilterParams, ActivityStatus, ActivityType } from "@/types/backend";
import { ErrorState } from "@/components/common/AsyncState";
import { ExploreGridSkeleton } from "@/components/common/Skeletons";
import { PageControls } from "@/components/common/PageControls";
import { SectionHeader } from "@/components/common/SectionHeader";
import { SearchBar } from "@/components/search/SearchBar";
import { getDatePresetRange } from "@/components/search/dateRangePresets";
import { Chip } from "@/components/common/Chip";
import type { Tone } from "@/lib/activityVisuals";

const CATEGORIES = [
  { key: "Todo", emoji: "✨", type: undefined },
  { key: "Outdoor", emoji: "🏕️", type: "OUTDOOR" as ActivityType },
  { key: "Indoor", emoji: "🏠", type: "INDOOR" as ActivityType },
  { key: "Mixto", emoji: "🎉", type: "MIXED" as ActivityType },
] as const;

const CATEGORY_TONE: Record<(typeof CATEGORIES)[number]["key"], Tone> = {
  Todo: "violet",
  Outdoor: "mint",
  Indoor: "lav",
  Mixto: "sun",
};

/** Statuses a user can still join (see ActivityStatus.java / Activity.
 * cannotChangeParticipants): everything except CANCELLED and FINISHED, which
 * never accept new participants. Explorar defaults to these so browsing
 * doesn't surface plans that are already over — "Ver todas" opts back in. */
const JOINABLE_STATUSES: ActivityStatus[] = ["CONFIRMED", "PROPOSED", "RESCHEDULED"];

/** How many cards each highlight rail loads — a handful more than fits on
 * screen so there's always one peeking at the edge to hint the scroll. */
const RAIL_SIZE = 8;

function isCategoryKey(value: string | null): value is (typeof CATEGORIES)[number]["key"] {
  return CATEGORIES.some((c) => c.key === value);
}

export function ExplorarPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // Every filter is seeded from the URL on first render, and an effect below
  // keeps the URL in sync as they change — so navigating into an activity
  // and back (or reloading, or sharing the link) restores this exact search
  // instead of dropping back to the unfiltered default.
  const [query, setQuery] = useState(() => searchParams?.get("q") ?? "");
  const [category, setCategory] = useState<(typeof CATEGORIES)[number]["key"]>(() => {
    const raw = searchParams?.get("type") ?? null;
    return isCategoryKey(raw) ? raw : "Todo";
  });
  const [city, setCity] = useState(() => searchParams?.get("city") ?? "");
  const [dateFrom, setDateFrom] = useState(() => searchParams?.get("from") ?? "");
  const [dateTo, setDateTo] = useState(() => searchParams?.get("to") ?? "");
  const [onlyAvailable, setOnlyAvailable] = useState(() => searchParams?.get("available") !== "0");
  const [includeAllStatuses, setIncludeAllStatuses] = useState(() => searchParams?.get("all") === "1");
  const [debouncedCity, setDebouncedCity] = useState(city);
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedCity(city), 400);
    return () => window.clearTimeout(timer);
  }, [city]);

  useEffect(() => {
    const timer = window.setTimeout(() => setDebouncedQuery(query), 400);
    return () => window.clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedQuery.trim()) params.set("q", debouncedQuery.trim());
    if (category !== "Todo") params.set("type", category);
    if (debouncedCity.trim()) params.set("city", debouncedCity.trim());
    if (dateFrom) params.set("from", dateFrom);
    if (dateTo) params.set("to", dateTo);
    if (!onlyAvailable) params.set("available", "0");
    if (includeAllStatuses) params.set("all", "1");
    const qs = params.toString();
    router.replace(qs ? `/explorar?${qs}` : "/explorar", { scroll: false });
  }, [category, dateFrom, dateTo, debouncedCity, debouncedQuery, includeAllStatuses, onlyAvailable, router]);

  const type = CATEGORIES.find((c) => c.key === category)?.type;
  const invalidDates = Boolean(dateFrom && dateTo && dateFrom > dateTo);

  const applied = useMemo<ActivityFilterParams>(
    () => ({
      type,
      title: debouncedQuery.trim() || undefined,
      city: debouncedCity.trim() || undefined,
      dateFrom: invalidDates || !dateFrom ? undefined : `${dateFrom}T00:00:00`,
      dateTo: invalidDates || !dateTo ? undefined : `${dateTo}T23:59:59`,
      availability: onlyAvailable || undefined,
      status: includeAllStatuses ? undefined : JOINABLE_STATUSES,
    }),
    [debouncedCity, debouncedQuery, dateFrom, dateTo, includeAllStatuses, invalidDates, onlyAvailable, type],
  );

  const { exploreFeed, loading, error, refresh, explorePage, exploreTotalPages, setExplorePage } = useActivities(applied);

  useEffect(() => {
    setExplorePage(0);
  }, [applied, setExplorePage]);

  // The highlight rails only make sense as a "browse from scratch" surface:
  // any active search narrows the grid below into exactly what the rails
  // would otherwise curate, so they'd be redundant (and confusing, showing
  // results outside the active filter). onlyAvailable/includeAllStatuses
  // stay in effect either way — those aren't a "search", just the grid's
  // baseline curation — see the mock's condition note.
  const showRails = category === "Todo" && !query.trim() && !city.trim() && !dateFrom && !dateTo;
  const railFilters = useMemo<ActivityFilterParams>(
    () => ({
      availability: onlyAvailable || undefined,
      status: includeAllStatuses ? undefined : JOINABLE_STATUSES,
      size: RAIL_SIZE,
    }),
    [includeAllStatuses, onlyAvailable],
  );
  // Stable for the component's lifetime — a session spanning midnight would
  // see a one-day-stale range, an acceptable tradeoff for not recomputing
  // this (and re-triggering the rail's fetch) on every render.
  const thisWeek = useMemo(() => getDatePresetRange("semana"), []);
  const weekRailFilters = useMemo<ActivityFilterParams>(
    () => ({ ...railFilters, dateFrom: `${thisWeek.from}T00:00:00`, dateTo: `${thisWeek.to}T23:59:59` }),
    [railFilters, thisWeek],
  );
  const outdoorRailFilters = useMemo<ActivityFilterParams>(
    () => ({ ...railFilters, type: "OUTDOOR" }),
    [railFilters],
  );
  const weekRail = useActivities(weekRailFilters, showRails);
  const outdoorRail = useActivities(outdoorRailFilters, showRails);

  const clearFilters = () => {
    setQuery("");
    setCategory("Todo");
    setCity("");
    setDateFrom("");
    setDateTo("");
    setOnlyAvailable(true);
    setIncludeAllStatuses(false);
  };

  return (
    <div className="fade-in px-5 pt-2 pb-6 lg:px-10 lg:pt-8">
      <div className="hidden lg:block lg:mb-6">
        <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Planes disponibles</p>
        <h2 className="font-brand text-4xl">Encontrá tu próximo plan</h2>
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
          includeAllStatuses={includeAllStatuses}
          onIncludeAllStatusesChange={setIncludeAllStatuses}
          onClear={clearFilters}
          invalidDates={invalidDates}
          typeLabel={category}
        />
      </div>

      <div className="flex gap-2 overflow-x-auto scrollbar-none pb-1 -mx-5 px-5 mb-5 lg:mx-0 lg:px-0">
        {CATEGORIES.map(({ key, emoji }) => {
          const active = category === key;
          const tone = CATEGORY_TONE[key];
          return (
            <Chip
              key={key}
              as="button"
              onClick={() => setCategory(key)}
              aria-pressed={active}
              {...(active ? { tone, sticker: true, rotate: -2 } : { active: false })}
              className="tap shrink-0 px-4 py-2"
            >
              <span className="emoji-3d" aria-hidden="true">{emoji}</span> {key}
            </Chip>
          );
        })}
      </div>

      {showRails && (
        <>
          <ActivityRail
            icon="🗓️"
            label="En esta semana"
            tone="sun"
            activities={weekRail.exploreFeed}
            loading={weekRail.loading}
            meta={(a) => `🗓️ ${a.when}`}
            onViewAll={() => { setDateFrom(thisWeek.from); setDateTo(thisWeek.to); }}
          />
          <ActivityRail
            icon="🏕️"
            label="Al aire libre"
            tone="mint"
            activities={outdoorRail.exploreFeed}
            loading={outdoorRail.loading}
            meta={(a) => `📍 ${a.where}`}
            onViewAll={() => setCategory("Outdoor")}
          />
        </>
      )}

      {showRails && !loading && !error && exploreFeed.length > 0 && <SectionHeader label="Todas las actividades" />}

      {loading && <ExploreGridSkeleton />}
      {error && (
        <ErrorState message={error} retry={refresh} />
      )}
      {!loading && !error && (
        <>
          <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-5">
            {exploreFeed.map((a) => <ExploreCard key={a.id} activity={a} onRefreshImages={refresh} />)}
          </div>
          <PageControls page={explorePage} totalPages={exploreTotalPages} onPageChange={setExplorePage} />
        </>
      )}
      {!loading && !error && exploreFeed.length === 0 && (
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

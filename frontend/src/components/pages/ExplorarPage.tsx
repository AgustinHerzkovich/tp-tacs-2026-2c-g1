"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { useActivities } from "@/hooks/useActivities";
import { ExploreCard } from "@/components/activities/ExploreCard";
import { TYPE_META } from "@/lib/activityVisuals";

const FILTERS = ["Todo", "Outdoor", "Indoor", "Mixto", "Hoy"] as const;

export function ExplorarPage() {
  const { exploreFeed, loading, error } = useActivities();
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("Todo");
  const today = new Date();
  const todayKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;

  const results = exploreFeed.filter((a) => {
    const matchesQuery = a.title.toLowerCase().includes(query.toLowerCase());
    const matchesFilter =
      filter === "Todo" ||
      (filter === "Hoy" ? a.dateTime.slice(0, 10) === todayKey : TYPE_META[a.type].label === filter);
    return matchesQuery && matchesFilter;
  });

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
      {loading && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          Cargando…
        </p>
      )}
      {error && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--destructive)" }}>
          {error}
        </p>
      )}
      {!loading && !error && (
        <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-5">
          {results.map((a) => <ExploreCard key={a.id} activity={a} />)}
        </div>
      )}
      {!loading && !error && results.length === 0 && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          No encontramos actividades con esos filtros.
        </p>
      )}
    </div>
  );
}

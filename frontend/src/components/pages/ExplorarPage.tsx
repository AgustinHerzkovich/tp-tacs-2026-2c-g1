"use client";

import { useState } from "react";
import { Search } from "lucide-react";
import { useActivities } from "@/hooks/useActivities";
import { ExploreCard } from "@/components/activities/ExploreCard";
import { TYPE_META } from "@/data/mockData";

const FILTERS = ["Todo", "Outdoor", "Indoor", "Mixto", "Hoy"] as const;

export function ExplorarPage() {
  const { exploreFeed } = useActivities();
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>("Todo");

  const results = exploreFeed.filter((a) => {
    const matchesQuery = a.title.toLowerCase().includes(query.toLowerCase());
    const matchesFilter = filter === "Todo" || filter === "Hoy" || TYPE_META[a.type].label === filter;
    return matchesQuery && matchesFilter;
  });

  return (
    <div className="fade-in px-5 pt-2 pb-6">
      <div className="relative mb-3">
        <Search className="absolute left-4 top-1/2 -translate-y-1/2 size-4" style={{ color: "var(--muted-foreground)" }} />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Buscar actividades…"
          className="w-full rounded-2xl border-2 pl-11 pr-4 py-3 font-body font-semibold text-[14px] outline-none bg-white"
          style={{ borderColor: "var(--border)" }}
        />
      </div>
      <div className="flex gap-2 overflow-x-auto pb-1 -mx-5 px-5 mb-5">
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
      {results.map((a) => (
        <ExploreCard key={a.id} activity={a} />
      ))}
      {results.length === 0 && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          No encontramos actividades con esos filtros.
        </p>
      )}
    </div>
  );
}

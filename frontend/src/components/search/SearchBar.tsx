"use client";

import { useEffect, useRef, useState, type Dispatch, type ReactNode, type SetStateAction } from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useHeaderCenterSlot } from "@/components/layout/HeaderSlot";
import { StickerTag } from "@/components/common/StickerTag";
import { LocationFieldContent } from "./LocationField";
import { DateRangeFieldContent } from "./DateRangeField";
import { FiltersContent } from "./FiltersContent";
import { formatSearchSummary } from "./searchSummary";

type Segment = "que" | "donde" | "cuando" | null;

interface SearchBarProps {
  query: string;
  onQueryChange: (value: string) => void;
  city: string;
  onCityChange: (value: string) => void;
  dateFrom: string;
  dateTo: string;
  onDateRangeChange: (range: { dateFrom: string; dateTo: string }) => void;
  onlyAvailable: boolean;
  onOnlyAvailableChange: (value: boolean) => void;
  includeAllStatuses: boolean;
  onIncludeAllStatusesChange: (value: boolean) => void;
  onClear: () => void;
  invalidDates?: boolean;
  /** Human label of the active type filter ("Outdoor"/"Indoor"/"Mixto"), only
   * used to build the summary text — the chip row itself lives in the page. */
  typeLabel: string;
}

/** Airbnb-style search entry point for Explorar: a 3-segment pill on desktop
 * (each segment opens its own popover), a single collapsed button on mobile
 * that opens a full-screen sheet with the same three fields stacked. Once the
 * pill scrolls out of view, a compact summary docks into the shared sticky
 * `Header` via `useHeaderCenterSlot` and reopens the same sheet when tapped.
 * Purely a controlled UI layer — all filter state/fetching stays owned by
 * `ExplorarPage`. */
export function SearchBar({
  query, onQueryChange,
  city, onCityChange,
  dateFrom, dateTo, onDateRangeChange,
  onlyAvailable, onOnlyAvailableChange,
  includeAllStatuses, onIncludeAllStatusesChange,
  onClear, invalidDates, typeLabel,
}: SearchBarProps) {
  const [openSegment, setOpenSegment] = useState<Segment>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [sheetOpen, setSheetOpen] = useState(false);
  const [sheetSection, setSheetSection] = useState<Segment>("donde");
  const [collapsed, setCollapsed] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = sentinelRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => entry && setCollapsed(!entry.isIntersecting),
      { rootMargin: "-84px 0px 0px 0px", threshold: 0 },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const summary = formatSearchSummary({ query, city, dateFrom, dateTo, type: typeLabel });
  const dateSummary = dateFrom || dateTo
    ? formatSearchSummary({ query: "", city: "", dateFrom, dateTo, type: "" })
    : "";

  useHeaderCenterSlot(
    collapsed ? (
      <button
        type="button"
        onClick={() => { setSheetSection("donde"); setSheetOpen(true); }}
        className="tap flex max-w-full items-center gap-2 rounded-full border-2 bg-white px-3.5 py-2"
        style={{ borderColor: "var(--border)" }}
      >
        <Search className="size-3.5 shrink-0" style={{ color: "var(--primary)" }} />
        <span className="truncate text-[12px] font-bold">{summary}</span>
      </button>
    ) : null,
  );

  return (
    <div className="flex flex-col gap-2">
      <div ref={sentinelRef} aria-hidden="true" />

      {/* One row shared by both breakpoints: the pill (desktop) or the
          collapsed trigger (mobile) swap via `hidden`/`flex`, but the
          Filters button renders exactly once — Radix portals popover
          content to <body> regardless of a hidden ancestor, so mounting it
          twice with shared state fought itself into never staying open. */}
      <div className="flex items-stretch gap-2">
        {/* Desktop: 3-segment pill, each opens its own popover */}
        <div className="hidden lg:flex flex-1 items-stretch rounded-full border-[3px] border-border bg-white shadow-[0_4px_0_var(--border)]">
          <Popover open={openSegment === "donde"} onOpenChange={(open) => setOpenSegment(open ? "donde" : null)}>
            <PopoverTrigger asChild>
              <button type="button" className="flex-1 min-w-0 flex flex-col justify-center rounded-l-full px-6 py-2 text-left hover:bg-muted/60">
                <span className="text-[10px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Dónde</span>
                <span className="truncate text-[13.5px] font-semibold">{city || "Explorá ubicaciones"}</span>
              </button>
            </PopoverTrigger>
            <PopoverContent align="start" className="w-80 rounded-3xl border-[3px] border-border p-5 shadow-[0_14px_30px_-14px_rgba(58,51,82,.3)]">
              <div className="flex items-center justify-between mb-4">
                <StickerTag tone="sky">📍 Dónde</StickerTag>
                <PopoverCloseButton onClose={() => setOpenSegment(null)} />
              </div>
              <LocationFieldContent city={city} onCityChange={onCityChange} onDone={() => setOpenSegment(null)} />
            </PopoverContent>
          </Popover>

          <div className="w-[3px] my-2.5 shrink-0 bg-border" />

          <Popover open={openSegment === "cuando"} onOpenChange={(open) => setOpenSegment(open ? "cuando" : null)}>
            <PopoverTrigger asChild>
              <button type="button" className="flex-1 min-w-0 flex flex-col justify-center px-6 py-2 text-left hover:bg-muted/60">
                <span className="text-[10px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Cuándo</span>
                <span className="truncate text-[13.5px] font-semibold">{dateSummary || "Agregá fechas"}</span>
              </button>
            </PopoverTrigger>
            <PopoverContent align="center" className="w-auto rounded-3xl border-[3px] border-border p-5 shadow-[0_14px_30px_-14px_rgba(58,51,82,.3)]">
              <div className="flex items-center justify-between mb-4">
                <StickerTag tone="sun">🗓️ Cuándo</StickerTag>
                <PopoverCloseButton onClose={() => setOpenSegment(null)} />
              </div>
              <DateRangeFieldContent dateFrom={dateFrom} dateTo={dateTo} onChange={onDateRangeChange} invalid={invalidDates} numberOfMonths={2} />
            </PopoverContent>
          </Popover>

          <div className="w-[3px] my-2.5 shrink-0 bg-border" />

          {/* "Qué" is a plain input, not a popup — there's nothing to pick
              from a panel for free-text search, so a popover would just be
              an empty click-to-focus step. The hover still matches the
              other two segments even though there's no button semantics to
              back it — it's the same control surface. */}
          <div className="flex-1 min-w-0 flex flex-col justify-center rounded-r-full px-6 py-2 hover:bg-muted/60">
            <label htmlFor="search-query" className="text-[10px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Qué</label>
            <input
              id="search-query"
              value={query}
              onChange={(event) => onQueryChange(event.target.value)}
              placeholder="Buscar actividades"
              className="truncate bg-transparent text-[13.5px] font-semibold outline-none placeholder:text-muted-foreground placeholder:font-semibold"
            />
          </div>

          <button
            type="button"
            aria-label="Buscar"
            onClick={() => setOpenSegment(null)}
            className="tap m-1.5 flex size-10 shrink-0 items-center justify-center rounded-full"
            style={{ background: "var(--primary)" }}
          >
            <Search className="size-4" style={{ color: "var(--primary-foreground)" }} />
          </button>
        </div>

        {/* Mobile: single collapsed trigger, opens the full sheet below */}
        <button
          type="button"
          onClick={() => { setSheetSection("que"); setSheetOpen(true); }}
          className="tap flex lg:hidden flex-1 min-w-0 items-center gap-2.5 rounded-full border-2 bg-white px-4 py-3"
          style={{ borderColor: "var(--border)" }}
        >
          <Search className="size-4 shrink-0" style={{ color: "var(--primary)" }} />
          <span className="truncate text-[13px] font-bold">{summary}</span>
        </button>

        <FiltersTrigger
          onlyAvailable={onlyAvailable}
          onOnlyAvailableChange={onOnlyAvailableChange}
          includeAllStatuses={includeAllStatuses}
          onIncludeAllStatusesChange={onIncludeAllStatusesChange}
          onClear={onClear}
          open={filtersOpen}
          onOpenChange={setFiltersOpen}
        />
      </div>

      <Sheet open={sheetOpen} onOpenChange={setSheetOpen}>
        <SheetContent side="bottom" className="h-[92dvh] rounded-t-3xl border-0 flex flex-col gap-0">
          <SheetHeader className="pb-2">
            <SheetTitle>Buscar actividades</SheetTitle>
          </SheetHeader>
          <div className="flex-1 overflow-y-auto px-4 flex flex-col gap-2.5">
            <SheetFieldRow
              label="Dónde"
              value={city || "Explorá ubicaciones"}
              expanded={sheetSection === "donde"}
              onToggle={() => setSheetSection((current) => (current === "donde" ? null : "donde"))}
            >
              <LocationFieldContent city={city} onCityChange={onCityChange} onDone={() => setSheetSection("cuando")} />
            </SheetFieldRow>
            <SheetFieldRow
              label="Cuándo"
              value={dateSummary || "Cuando sea"}
              expanded={sheetSection === "cuando"}
              onToggle={() => setSheetSection((current) => (current === "cuando" ? null : "cuando"))}
            >
              <DateRangeFieldContent dateFrom={dateFrom} dateTo={dateTo} onChange={onDateRangeChange} invalid={invalidDates} />
            </SheetFieldRow>

            {/* "Qué" is a plain input, not an accordion — nothing to expand
                for free-text search. */}
            <div className="rounded-2xl border-2 px-4 py-3" style={{ borderColor: "var(--border)" }}>
              <label htmlFor="sheet-search-query" className="block text-[11px] font-extrabold uppercase mb-1" style={{ color: "var(--muted-foreground)" }}>Qué</label>
              <Input
                id="sheet-search-query"
                value={query}
                onChange={(event) => onQueryChange(event.target.value)}
                placeholder="Buscar actividades"
                className="rounded-xl bg-white focus-visible:ring-1"
              />
            </div>
          </div>
          <SheetFooter className="flex-row gap-3 border-t-2 mt-2" style={{ borderColor: "var(--border)" }}>
            <Button type="button" variant="outline" size="xl" className="flex-1" onClick={onClear}>Limpiar</Button>
            <Button type="button" size="xl" className="flex-1" onClick={() => setSheetOpen(false)}>Buscar</Button>
          </SheetFooter>
        </SheetContent>
      </Sheet>
    </div>
  );
}

function FiltersTrigger({
  onlyAvailable, onOnlyAvailableChange, includeAllStatuses, onIncludeAllStatusesChange, onClear, open, onOpenChange,
}: {
  onlyAvailable: boolean;
  onOnlyAvailableChange: (value: boolean) => void;
  includeAllStatuses: boolean;
  onIncludeAllStatusesChange: (value: boolean) => void;
  onClear: () => void;
  open: boolean;
  onOpenChange: Dispatch<SetStateAction<boolean>>;
}) {
  // Both filters start at their "curated" default (only joinable activities,
  // only ones with room) — the dot flags when the user opted out of that.
  const nonDefault = !onlyAvailable || includeAllStatuses;
  return (
    <Popover open={open} onOpenChange={onOpenChange}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label="Filtros"
          className="tap relative shrink-0 flex size-12 items-center justify-center rounded-full border-[3px] border-border bg-white shadow-[0_4px_0_var(--border)]"
        >
          <SlidersHorizontal className="size-[18px]" />
          {nonDefault && (
            <span className="absolute -top-0.5 -right-0.5 size-3 rounded-full border-2 border-white" style={{ background: "var(--primary)" }} />
          )}
        </button>
      </PopoverTrigger>
      <PopoverContent align="end" className="w-96 rounded-3xl border-[3px] border-border p-5 shadow-[0_14px_30px_-14px_rgba(58,51,82,.3)]">
        <div className="flex items-center justify-between mb-4">
          <StickerTag tone="violet">
            <SlidersHorizontal className="size-3 mr-1" /> Filtros
          </StickerTag>
          <PopoverCloseButton onClose={() => onOpenChange(false)} />
        </div>
        <FiltersContent
          onlyAvailable={onlyAvailable}
          onOnlyAvailableChange={onOnlyAvailableChange}
          includeAllStatuses={includeAllStatuses}
          onIncludeAllStatusesChange={onIncludeAllStatusesChange}
          onClear={onClear}
        />
      </PopoverContent>
    </Popover>
  );
}

/** "✕" next to a segment's popover's tag label, on the same row so both
 * sit at the same height — a dismiss affordance in addition to the
 * click-outside/Escape Radix already gives for free. Just the mark at
 * rest; the circle only shows up on hover so it doesn't compete with the
 * tag label beside it. */
function PopoverCloseButton({ onClose }: { onClose: () => void }) {
  return (
    <button
      type="button"
      aria-label="Cerrar"
      onClick={onClose}
      className="tap flex size-7 shrink-0 items-center justify-center rounded-full text-muted-foreground hover:bg-muted hover:text-foreground"
    >
      <X className="size-3.5" />
    </button>
  );
}

function SheetFieldRow({
  label, value, expanded, onToggle, children,
}: {
  label: string;
  value: string;
  expanded: boolean;
  onToggle: () => void;
  children: ReactNode;
}) {
  return (
    <div className="rounded-2xl border-2 overflow-hidden" style={{ borderColor: "var(--border)" }}>
      <button
        type="button"
        onClick={onToggle}
        className="tap w-full flex items-center justify-between gap-3 px-4 py-3.5 text-left outline-none"
        style={expanded ? { background: "var(--muted)" } : undefined}
      >
        <span className="text-[11px] font-extrabold uppercase shrink-0" style={{ color: "var(--muted-foreground)" }}>{label}</span>
        {!expanded && <span className="truncate text-[13px] font-bold">{value}</span>}
      </button>
      {expanded && <div className="px-4 pb-4">{children}</div>}
    </div>
  );
}

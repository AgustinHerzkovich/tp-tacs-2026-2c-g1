"use client";

import { useMemo, useState } from "react";
import { CalendarGrid } from "@/components/activities/CalendarGrid";
import { CalendarLegend } from "@/components/activities/CalendarLegend";
import { MisCard } from "@/components/activities/MisCard";
import { ErrorState } from "@/components/common/AsyncState";
import { CalendarSkeleton } from "@/components/common/Skeletons";
import { useCalendarActivities } from "@/hooks/useCalendarActivities";
import { MONTH_NAMES, dateKey, formatFullDate, getMonthGrid } from "@/lib/calendar";

function startOfToday(): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate());
}

/** The calendar view of "Mis actividades": a month grid (see `CalendarGrid`)
 * plus the selected day's agenda, reusing the same `MisCard` the list view
 * uses. Owns its own month/selection state and fetches only that month's
 * activities via `useCalendarActivities`, independent of the list view's
 * paginated feeds. */
export function CalendarView() {
  const today = useMemo(() => startOfToday(), []);
  const todayKey = useMemo(() => dateKey(today), [today]);
  const [visibleMonth, setVisibleMonth] = useState({ year: today.getFullYear(), month0: today.getMonth() });
  const [selectedKey, setSelectedKey] = useState(todayKey);

  const { activitiesByDay, loading, error, refresh } = useCalendarActivities(
    visibleMonth.year,
    visibleMonth.month0,
  );

  const cells = useMemo(
    () => getMonthGrid(visibleMonth.year, visibleMonth.month0),
    [visibleMonth],
  );

  function goToMonth(delta: number) {
    setVisibleMonth((current) => {
      const date = new Date(current.year, current.month0 + delta, 1);
      return { year: date.getFullYear(), month0: date.getMonth() };
    });
  }

  const selectedDate = useMemo(() => {
    const [y, m, d] = selectedKey.split("-").map(Number);
    return new Date(y ?? today.getFullYear(), (m ?? 1) - 1, d ?? 1);
  }, [selectedKey, today]);

  const selectedActivities = activitiesByDay.get(selectedKey) ?? [];

  if (error) return <ErrorState message={error} retry={refresh} />;

  return (
    <div className="lg:flex lg:items-start lg:gap-6">
      <div className="lg:w-[43rem] lg:flex-[0_0_auto] lg:rounded-[24px] lg:bg-white lg:p-6 lg:shadow-[0_8px_20px_-12px_rgba(58,51,82,.25)]">
        {loading ? (
          <CalendarSkeleton />
        ) : (
          <>
            <div className="mb-3 flex items-center justify-center gap-3">
              <button
                type="button"
                onClick={() => goToMonth(-1)}
                aria-label="Mes anterior"
                className="flex h-7 w-7 items-center justify-center rounded-full text-[13px] font-extrabold"
                style={{ color: "var(--muted-foreground)" }}
              >
                ‹
              </button>
              <h3 className="font-brand-title min-w-[150px] text-center text-[15px] lg:min-w-[180px] lg:text-[17px]">
                {MONTH_NAMES[visibleMonth.month0]} {visibleMonth.year}
              </h3>
              <button
                type="button"
                onClick={() => goToMonth(1)}
                aria-label="Mes siguiente"
                className="flex h-7 w-7 items-center justify-center rounded-full text-[13px] font-extrabold"
              >
                ›
              </button>
            </div>

            <CalendarGrid
              cells={cells}
              activitiesByDay={activitiesByDay}
              selectedKey={selectedKey}
              todayKey={todayKey}
              onSelectDay={(key) => setSelectedKey(key)}
            />

            <div className="mt-3.5">
              <CalendarLegend />
            </div>
          </>
        )}
      </div>

      <div className="mt-5 lg:mt-0 lg:flex lg:min-w-0 lg:max-h-[640px] lg:flex-1 lg:flex-col lg:rounded-[24px] lg:bg-white lg:p-6 lg:shadow-[0_8px_20px_-12px_rgba(58,51,82,.25)]">
        <p
          className="hidden text-[11px] font-extrabold uppercase tracking-wide lg:mb-0.5 lg:block lg:shrink-0"
          style={{ color: "var(--primary)" }}
        >
          Tu selección
        </p>
        <div className="mb-2.5 flex items-center gap-2 lg:mb-4 lg:shrink-0">
          <h3 className="font-brand-title text-[15px] lg:text-[19px]">{formatFullDate(selectedDate)}</h3>
          {selectedKey === todayKey && (
            <span
              className="inline-flex items-center rounded-full px-2.5 py-0.5 text-[10px] font-black"
              style={{ background: "var(--secondary)", color: "var(--secondary-foreground)" }}
            >
              Hoy
            </span>
          )}
        </div>

        {!loading && selectedActivities.length === 0 && (
          <p className="text-[12.5px] font-bold lg:shrink-0" style={{ color: "var(--muted-foreground)" }}>
            No tenés planes este día.
          </p>
        )}

        <div className="flex flex-col gap-0 lg:flex-1 lg:gap-3 lg:overflow-y-auto lg:pr-1">
          {selectedActivities.map((activity) => (
            <MisCard key={activity.id} activity={activity} onRefreshImages={refresh} variant="outline" />
          ))}
        </div>
      </div>
    </div>
  );
}

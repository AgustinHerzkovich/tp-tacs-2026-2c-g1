"use client";

import { cn } from "cn";
import { STATUS_META, TONE_META } from "@/lib/activityVisuals";
import { WEEKDAY_LABELS, dateKey, type CalendarCell } from "@/lib/calendar";
import type { MisActivity } from "@/types/domain";

const MAX_DOTS = 3;
const MAX_PILLS = 2;

/** Fluid month grid: dots (always) plus, from `lg:` up, truncated title pills
 * — one responsive markup instead of separate mobile/desktop components (see
 * the approved plan). Columns are `fr`-based so this never forces a fixed
 * width, which matters for `responsive.spec.ts`'s no-horizontal-overflow
 * check down to 320px. */
export function CalendarGrid({
  cells,
  activitiesByDay,
  selectedKey,
  todayKey,
  onSelectDay,
}: {
  cells: CalendarCell[];
  activitiesByDay: Map<string, MisActivity[]>;
  selectedKey: string | null;
  todayKey: string;
  onSelectDay: (key: string, date: Date) => void;
}) {
  return (
    <div>
      <div className="grid grid-cols-7 gap-1 lg:gap-2">
        {WEEKDAY_LABELS.map((label) => (
          <span
            key={label}
            className="pb-1 text-center text-[10px] font-black uppercase tracking-wide"
            style={{ color: "var(--muted-foreground)" }}
          >
            {label.slice(0, 1)}
            <span className="hidden lg:inline">{label.slice(1)}</span>
          </span>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1 lg:gap-2">
        {cells.map((cell) => {
          const key = dateKey(cell.date);
          const dayActivities = activitiesByDay.get(key) ?? [];
          const isToday = key === todayKey;
          const isSelected = key === selectedKey;

          return (
            <button
              key={key}
              type="button"
              disabled={!cell.inMonth}
              onClick={() => onSelectDay(key, cell.date)}
              aria-pressed={isSelected}
              aria-label={`${cell.date.getDate()}${dayActivities.length > 0 ? `, ${dayActivities.length} actividad${dayActivities.length > 1 ? "es" : ""}` : ""}`}
              className={cn(
                "flex h-11 w-full flex-col items-center gap-1 rounded-[10px] pt-1 lg:h-[92px] lg:items-start lg:gap-1.5 lg:rounded-[12px] lg:p-1.5",
                cell.inMonth ? "cursor-pointer" : "cursor-default",
              )}
              style={
                isSelected
                  ? {
                      background: "var(--mint)",
                      border: "2px solid #fff",
                      boxShadow: "0 3px 6px rgba(58,51,82,.2)",
                      transform: "rotate(-1deg)",
                    }
                  : undefined
              }
            >
              <span
                className={cn(
                  "flex h-[22px] w-[22px] items-center justify-center rounded-full text-[12px] font-extrabold leading-none lg:h-5 lg:w-5 lg:text-[13px]",
                  !cell.inMonth && "opacity-35",
                )}
                style={{
                  color: !cell.inMonth ? "var(--muted-foreground)" : isToday ? "#fff" : "var(--foreground)",
                  background: isToday ? "var(--primary)" : undefined,
                }}
              >
                {cell.date.getDate()}
              </span>

              {dayActivities.length > 0 && (
                <span className="flex gap-0.5 lg:hidden">
                  {dayActivities.slice(0, MAX_DOTS).map((activity, i) => (
                    <span
                      key={i}
                      className="h-1.5 w-1.5 rounded-full"
                      style={{ background: TONE_META[STATUS_META[activity.status].tone].ink }}
                    />
                  ))}
                </span>
              )}

              {dayActivities.length > 0 && (
                <span className="hidden w-full flex-col gap-0.5 lg:flex">
                  {dayActivities.slice(0, MAX_PILLS).map((activity) => {
                    const tone = TONE_META[STATUS_META[activity.status].tone];
                    return (
                      <span
                        key={activity.id}
                        className="block w-full truncate rounded-[5px] px-1 py-0.5 text-left text-[8.5px] font-extrabold"
                        style={{ background: tone.bg, color: tone.ink }}
                      >
                        {activity.title}
                      </span>
                    );
                  })}
                </span>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
}

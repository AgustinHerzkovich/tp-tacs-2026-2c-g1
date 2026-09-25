"use client";

import type { DateRange } from "react-day-picker";
import { Calendar } from "@/components/ui/calendar";
import { DATE_PRESETS, dateKey, getDatePresetRange, parseDateKey } from "./dateRangePresets";

interface DateRangeFieldContentProps {
  dateFrom: string;
  dateTo: string;
  onChange: (range: { dateFrom: string; dateTo: string }) => void;
  invalid?: boolean;
  /** 2 on the desktop popover (room for a proper Airbnb-sized calendar), 1
   * on the mobile sheet where a second month would just force horizontal
   * scrolling. */
  numberOfMonths?: number;
}

export function DateRangeFieldContent({ dateFrom, dateTo, onChange, invalid, numberOfMonths = 1 }: DateRangeFieldContentProps) {
  const selected: DateRange | undefined = dateFrom || dateTo
    ? { from: parseDateKey(dateFrom), to: parseDateKey(dateTo) }
    : undefined;

  return (
    <div className="flex flex-col gap-4">
      <Calendar
        mode="range"
        numberOfMonths={numberOfMonths}
        selected={selected}
        onSelect={(range) => {
          onChange({
            dateFrom: range?.from ? dateKey(range.from) : "",
            dateTo: range?.to ? dateKey(range.to) : "",
          });
        }}
      />

      <div className="flex gap-2 overflow-x-auto scrollbar-none pb-1">
        {DATE_PRESETS.map((preset) => {
          const range = getDatePresetRange(preset.key);
          const active = dateFrom === range.from && dateTo === range.to;
          return (
            <button
              key={preset.key}
              type="button"
              onClick={() => onChange({ dateFrom: range.from, dateTo: range.to })}
              className="tap shrink-0 rounded-full px-3 py-1.5 text-[12px] font-extrabold whitespace-nowrap"
              style={
                active
                  ? { background: "var(--sun)", color: "var(--sun-ink)", border: "2px solid #fff", boxShadow: "0 2px 6px rgba(58,51,82,.18)" }
                  : { border: "2px solid var(--border)", background: "#fff", color: "var(--muted-foreground)" }
              }
            >
              {preset.label}
            </button>
          );
        })}
      </div>

      {(dateFrom || dateTo) && (
        <button
          type="button"
          onClick={() => onChange({ dateFrom: "", dateTo: "" })}
          className="self-start text-[12.5px] font-extrabold underline"
          style={{ color: "var(--muted-foreground)" }}
        >
          Borrar fechas
        </button>
      )}
      {invalid && (
        <p className="text-[12px] font-extrabold" style={{ color: "var(--destructive)" }}>
          La fecha desde no puede ser posterior a la fecha hasta.
        </p>
      )}
    </div>
  );
}

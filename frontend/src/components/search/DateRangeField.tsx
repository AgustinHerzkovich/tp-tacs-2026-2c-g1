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

      <div className="flex gap-2 overflow-x-auto pb-1">
        {DATE_PRESETS.map((preset) => (
          <button
            key={preset.key}
            type="button"
            onClick={() => {
              const range = getDatePresetRange(preset.key);
              onChange({ dateFrom: range.from, dateTo: range.to });
            }}
            className="tap shrink-0 rounded-full border-2 px-3 py-1.5 text-[12px] font-extrabold whitespace-nowrap"
            style={{ borderColor: "var(--border)", color: "var(--foreground)" }}
          >
            {preset.label}
          </button>
        ))}
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

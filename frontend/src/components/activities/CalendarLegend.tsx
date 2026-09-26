import { STATUS_META, TONE_META } from "@/lib/activityVisuals";
import type { MockStatusKey } from "@/types/domain";

/** Only the statuses `mapActivityStatus` can actually produce for a real
 * activity — `STATUS_META` also has a "votacion" entry that no mapped
 * `MisActivity` ever carries, so it's left out here to avoid a legend swatch
 * that could never appear on the calendar. */
const CALENDAR_STATUSES: MockStatusKey[] = ["confirmada", "propuesta", "reprogramada", "cancelada", "finalizada"];

export function CalendarLegend() {
  return (
    <div className="flex flex-wrap gap-x-4 gap-y-2">
      {CALENDAR_STATUSES.map((status) => {
        const meta = STATUS_META[status];
        return (
          <span
            key={status}
            className="inline-flex items-center gap-1.5 text-[10px] font-extrabold lg:text-[11px]"
            style={{ color: "var(--muted-foreground)" }}
          >
            <span className="h-2 w-2 shrink-0 rounded-full" style={{ background: TONE_META[meta.tone].ink }} />
            {meta.label}
          </span>
        );
      })}
    </div>
  );
}

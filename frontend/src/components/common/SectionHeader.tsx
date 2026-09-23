import type { Tone } from "@/lib/activityVisuals";
import { TONE_META } from "@/lib/activityVisuals";

interface SectionHeaderProps {
  icon: string;
  label: string;
  /** Omitted hides the count pill — used for sections that don't need a
   * running total (e.g. a highlight list). */
  count?: number;
  tone?: Tone;
  className?: string;
}

/** Small icon + uppercase label + optional count pill, used above a list of
 * cards ("Creadas por mí 2", "A las que te sumaste 2"...). Reused wherever a
 * feed needs a named section instead of a full page header. */
export function SectionHeader({ icon, label, count, tone = "violet", className }: SectionHeaderProps) {
  const m = TONE_META[tone];
  return (
    <div className={`flex items-center gap-1.5 mb-2.5 ${className ?? ""}`}>
      <span className="text-sm" aria-hidden="true">{icon}</span>
      <span className="text-[11px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
        {label}
      </span>
      {count !== undefined && (
        <span
          className="ml-auto inline-flex items-center justify-center min-w-[18px] h-[18px] rounded-full text-[10px] font-black px-1"
          style={{ background: m.bg, color: m.ink }}
        >
          {count}
        </span>
      )}
    </div>
  );
}

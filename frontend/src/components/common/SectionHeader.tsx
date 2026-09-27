import type { Tone } from "@/lib/activityVisuals";
import { TONE_META } from "@/lib/activityVisuals";

interface SectionHeaderProps {
  /** Omitted for sections with no natural icon (e.g. "Todas las
   * actividades"). */
  icon?: string;
  label: string;
  /** Omitted hides the count pill — used for sections that don't need a
   * running total (e.g. a highlight list). Sits right next to the label
   * instead of at the far end of the row, so it still reads as attached to
   * it on a wide (desktop) row. */
  count?: number;
  tone?: Tone;
  className?: string;
}

/** Icon + uppercase label + optional count pill, above a dashed divider
 * that spans the full row — used to head a list of cards ("Creadas por mí
 * 2", "A las que te sumaste 2", "Todas las actividades"...). Reused
 * wherever a feed needs a named section instead of a full page header. */
export function SectionHeader({ icon, label, count, tone = "violet", className }: SectionHeaderProps) {
  const m = TONE_META[tone];
  return (
    <div
      className={`flex items-center gap-1.5 mb-3 pb-2 border-b-2 border-dashed ${className ?? ""}`}
      style={{ borderColor: "var(--border)" }}
    >
      {icon && (
        <span className="text-sm" aria-hidden="true">
          {icon}
        </span>
      )}
      <span className="text-[13px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
        {label}
      </span>
      {count !== undefined && (
        <span
          className="inline-flex items-center justify-center min-w-[18px] h-[18px] rounded-full text-[10px] font-black px-1"
          style={{ background: m.bg, color: m.ink }}
        >
          {count}
        </span>
      )}
    </div>
  );
}

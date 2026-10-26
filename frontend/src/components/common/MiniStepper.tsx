import { useState } from "react";
import { Minus, Plus } from "lucide-react";

interface MiniStepperProps {
  value: number;
  onChange: (value: number) => void;
  min: number;
  max: number;
  unit?: string;
  /** Accessible name of the editable number. */
  label?: string;
}

/** A number adjusted with the − / + buttons or typed directly. While typing the
 * text is kept as a draft, so an intermediate value (empty, or below `min`)
 * is not rejected mid-keystroke; it is clamped to `min`..`max` on blur or Enter,
 * and an empty draft restores the previous value. */
export function MiniStepper({ value, onChange, min, max, unit = "", label }: MiniStepperProps) {
  const [draft, setDraft] = useState<string | null>(null);

  const commit = () => {
    if (draft === null) return;
    const typed = Number.parseInt(draft, 10);
    if (!Number.isNaN(typed)) onChange(Math.min(max, Math.max(min, typed)));
    setDraft(null);
  };

  return (
    <div className="flex items-center gap-2.5">
      <button
        type="button"
        className="tap w-8 h-8 rounded-full flex items-center justify-center"
        style={{ background: "#fff", boxShadow: "0 3px 0 var(--border)" }}
        onClick={() => onChange(Math.max(min, value - 1))}
      >
        <Minus className="size-3.5" />
      </button>
      <input
        type="text"
        inputMode="numeric"
        aria-label={label}
        className="w-12 rounded-lg bg-transparent text-center font-display font-semibold tabular-nums outline-none focus-visible:ring-2 focus-visible:ring-ring"
        value={draft ?? `${value}${unit}`}
        onFocus={(event) => {
          setDraft(String(value));
          event.target.select();
        }}
        onChange={(event) => setDraft(event.target.value.replace(/\D/g, "").slice(0, String(max).length))}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === "Enter") event.currentTarget.blur();
        }}
      />
      <button
        type="button"
        className="tap w-8 h-8 rounded-full flex items-center justify-center"
        style={{ background: "var(--primary)", color: "var(--primary-foreground)", boxShadow: "0 3px 0 var(--secondary-foreground)" }}
        onClick={() => onChange(Math.min(max, value + 1))}
      >
        <Plus className="size-3.5" />
      </button>
    </div>
  );
}

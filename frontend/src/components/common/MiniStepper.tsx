import { Minus, Plus } from "lucide-react";

interface MiniStepperProps {
  value: number;
  onChange: (value: number) => void;
  min: number;
  max: number;
  unit?: string;
}

export function MiniStepper({ value, onChange, min, max, unit = "" }: MiniStepperProps) {
  return (
    <div className="flex items-center gap-2.5">
      <button
        type="button"
        className="tap w-8 h-8 rounded-lg border-2 flex items-center justify-center"
        style={{ background: "var(--background)", borderColor: "var(--border)" }}
        onClick={() => onChange(Math.max(min, value - 1))}
      >
        <Minus className="size-3.5" />
      </button>
      <span className="w-12 text-center font-display font-semibold tabular-nums">
        {value}
        {unit}
      </span>
      <button
        type="button"
        className="tap w-8 h-8 rounded-lg flex items-center justify-center"
        style={{ background: "var(--primary)", color: "var(--primary-foreground)" }}
        onClick={() => onChange(Math.min(max, value + 1))}
      >
        <Plus className="size-3.5" />
      </button>
    </div>
  );
}

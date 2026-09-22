import type { CSSProperties } from "react";
import { Info } from "lucide-react";
import { Slider } from "@/components/ui/slider";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

const THUMB =
  "[&_[data-slot=slider-thumb]]:size-5 [&_[data-slot=slider-thumb]]:border-[3px] [&_[data-slot=slider-thumb]]:border-[var(--primary)] [&_[data-slot=slider-thumb]]:bg-white [&_[data-slot=slider-thumb]]:shadow-[0_2px_0_var(--primary)]";
const TRACK = "[&_[data-slot=slider-track]]:h-3.5";

/** Round-number tick marks within [min, max] at a fixed interval, e.g.
 * roundTicks(0, 100, 20) -> [0, 20, 40, 60, 80, 100]. */
function roundTicks(min: number, max: number, interval: number): number[] {
  const ticks: number[] = [];
  for (let v = Math.ceil(min / interval) * interval; v <= max; v += interval) ticks.push(v);
  return ticks;
}

interface GradientSliderProps {
  from: string;
  to: string;
  min: number;
  max: number;
  step: number;
  tickInterval: number;
  value: number[];
  onValueChange: (value: number[]) => void;
  minStepsBetweenThumbs?: number;
}

function GradientSlider({ from, to, min, max, tickInterval, ...props }: GradientSliderProps) {
  const ticks = roundTicks(min, max, tickInterval);
  return (
    <div className="relative py-1.5">
      <Slider
        {...props}
        min={min}
        max={max}
        className={`${TRACK} ${THUMB}`}
        style={{ "--range-from": from, "--range-to": to } as CSSProperties}
      />
      <div className="pointer-events-none absolute inset-x-0 top-1/2 z-10 -translate-y-1/2 h-3.5">
        {ticks.map((tick) => (
          <span
            key={tick}
            className="absolute top-1/2 size-1 rounded-full bg-white -translate-x-1/2 -translate-y-1/2"
            style={{ left: `${((tick - min) / (max - min)) * 100}%` }}
          />
        ))}
      </div>
    </div>
  );
}

export function StepClima({ form, set }: StepProps) {
  return (
    <div className="pt-1">
      <div className="flex gap-2 mb-6">
        <Info className="size-4 shrink-0 mt-0.5" style={{ color: "var(--muted-foreground)" }} aria-hidden="true" />
        <p className="text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          Si el clima real se sale de estos rangos, abrimos una votación automática.
        </p>
      </div>

      <div className="mb-7">
        <div className="flex justify-between mb-3">
          <span className="font-extrabold text-[13px]">☔ Probabilidad máxima de lluvia</span>
          <span className="font-display font-semibold text-[14px] tabular-nums" style={{ color: "var(--accent-foreground)" }}>
            {form.rain}%
          </span>
        </div>
        <GradientSlider
          from="var(--sky)"
          to="var(--sky-ink)"
          min={0}
          max={100}
          step={1}
          tickInterval={20}
          value={[form.rain]}
          onValueChange={([v]) => set("rain")(v ?? form.rain)}
        />
      </div>

      <div className="mb-7">
        <div className="flex justify-between mb-3">
          <span className="font-extrabold text-[13px]">💨 Velocidad máxima del viento</span>
          <span className="font-display font-semibold text-[14px] tabular-nums" style={{ color: "var(--accent-foreground)" }}>
            {form.wind} km/h
          </span>
        </div>
        <GradientSlider
          from="var(--mint)"
          to="var(--lav-ink)"
          min={0}
          max={100}
          step={1}
          tickInterval={20}
          value={[form.wind]}
          onValueChange={([v]) => set("wind")(v ?? form.wind)}
        />
      </div>

      <div className="mb-3">
        <div className="flex justify-between mb-3">
          <span className="font-extrabold text-[13px]">🌡️ Rango de temperatura</span>
          <span className="font-display font-semibold text-[14px] tabular-nums" style={{ color: "var(--accent-foreground)" }}>
            {form.tMin}° – {form.tMax}°C
          </span>
        </div>
        <GradientSlider
          from="#60A5FA"
          to="#FB923C"
          min={-5}
          max={45}
          step={1}
          tickInterval={10}
          minStepsBetweenThumbs={1}
          value={[form.tMin, form.tMax]}
          onValueChange={([lo, hi]) => {
            set("tMin")(lo ?? form.tMin);
            set("tMax")(hi ?? form.tMax);
          }}
        />
      </div>
    </div>
  );
}

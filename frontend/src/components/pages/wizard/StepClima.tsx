import type { CSSProperties } from "react";
import { Slider } from "@/components/ui/slider";
import { MiniStepper } from "@/components/common/MiniStepper";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

const THUMB =
  "[&_[data-slot=slider-thumb]]:size-6 [&_[data-slot=slider-thumb]]:border-4 [&_[data-slot=slider-thumb]]:border-[var(--primary)] [&_[data-slot=slider-thumb]]:bg-white [&_[data-slot=slider-thumb]]:shadow-[0_4px_10px_-2px_rgba(58,51,82,.4)]";
const TRACK = "[&_[data-slot=slider-track]]:h-2.5";

interface GradientSliderProps {
  from: string;
  to: string;
  min: number;
  max: number;
  step: number;
  value: number[];
  onValueChange: (value: number[]) => void;
  minStepsBetweenThumbs?: number;
}

function GradientSlider({ from, to, ...props }: GradientSliderProps) {
  return (
    <Slider
      {...props}
      className={`${TRACK} ${THUMB}`}
      style={{ "--range-from": from, "--range-to": to } as CSSProperties}
    />
  );
}

export function StepClima({ form, set }: StepProps) {
  return (
    <div className="pt-1">
      <div className="rounded-2xl p-3.5 mb-6 flex gap-2.5" style={{ background: "var(--sky)", opacity: 0.7 }}>
        <span>ℹ️</span>
        <p className="text-[11.5px] font-extrabold" style={{ color: "var(--sky-ink)" }}>
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
          from="#BAE6FD"
          to="#0369A1"
          min={0}
          max={100}
          step={1}
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
          from="#A7F3D0"
          to="#6D28D9"
          min={0}
          max={100}
          step={1}
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
          minStepsBetweenThumbs={1}
          value={[form.tMin, form.tMax]}
          onValueChange={([lo, hi]) => {
            set("tMin")(lo ?? form.tMin);
            set("tMax")(hi ?? form.tMax);
          }}
        />
        <div className="flex items-center gap-6 mt-4">
          <div>
            <p className="text-[10.5px] font-extrabold mb-1.5" style={{ color: "var(--muted-foreground)" }}>
              MÍNIMA
            </p>
            <MiniStepper value={form.tMin} min={-5} max={form.tMax - 1} unit="°C" onChange={set("tMin")} />
          </div>
          <div>
            <p className="text-[10.5px] font-extrabold mb-1.5" style={{ color: "var(--muted-foreground)" }}>
              MÁXIMA
            </p>
            <MiniStepper value={form.tMax} min={form.tMin + 1} max={45} unit="°C" onChange={set("tMax")} />
          </div>
        </div>
      </div>
    </div>
  );
}

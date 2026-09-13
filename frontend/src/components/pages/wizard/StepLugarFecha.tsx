import { type ReactNode } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { MiniStepper } from "@/components/common/MiniStepper";
import { LocationMap } from "@/components/pages/wizard/LocationMap";
import type { WizardErrors } from "@/lib/validation";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
  errors?: WizardErrors;
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="mb-5">
      <Label className="mb-2 font-extrabold text-[11.5px] uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
        {label}
      </Label>
      {children}
    </div>
  );
}

function FieldError({ message }: { message?: string }) {
  if (!message) return null;
  return (
    <p className="mt-1.5 text-[12px] font-extrabold" style={{ color: "var(--destructive)" }}>
      {message}
    </p>
  );
}

export function StepLugarFecha({ form, set, patch, errors }: StepProps) {
  if (!patch) return null;
  return (
    <div>
      <Field label="Ubicación">
        <LocationMap
          place={form.place}
          latitude={form.latitude}
          longitude={form.longitude}
          invalid={!!errors?.place}
          onQueryChange={(query) => patch({ place: query, latitude: null, longitude: null })}
          onChange={(location) => patch({ place: location.label, latitude: location.latitude, longitude: location.longitude })}
        />
        <FieldError message={errors?.place} />
      </Field>

      <div className="grid grid-cols-2 gap-4">
        <Field label="Fecha">
          <Input
            type="date"
            value={form.date}
            onChange={(e) => set("date")(e.target.value)}
            aria-invalid={!!errors?.date}
            className="h-auto py-3 rounded-2xl border-2"
          />
          <FieldError message={errors?.date} />
        </Field>
        <Field label="Hora">
          <Input
            type="time"
            value={form.time}
            onChange={(e) => set("time")(e.target.value)}
            aria-invalid={!!errors?.time}
            className="h-auto py-3 rounded-2xl border-2"
          />
          <FieldError message={errors?.time} />
        </Field>
        <Field label="Mín. participantes">
          <MiniStepper value={form.min} min={1} max={form.max} onChange={set("min")} />
          <FieldError message={errors?.min} />
        </Field>
        <Field label="Máx. participantes">
          <MiniStepper value={form.max} min={form.min} max={99} onChange={set("max")} />
          <FieldError message={errors?.max} />
        </Field>
      </div>
    </div>
  );
}

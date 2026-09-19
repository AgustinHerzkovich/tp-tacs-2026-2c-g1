import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { InfoHint } from "@/components/common/InfoHint";
import type { ReactNode } from "react";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <div className="mb-5">
      <Label className="mb-2 flex items-center gap-1.5 font-extrabold text-[11.5px] uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
        {label}
        {hint && <InfoHint>{hint}</InfoHint>}
      </Label>
      {children}
    </div>
  );
}

export function StepAlertas({ form, set }: StepProps) {
  return (
    <div className="pt-1">
      <Field label="Ventana de anticipación" hint="Cuánto antes de la actividad te avisamos si el pronóstico no cumple las condiciones que elegiste, para que todavía puedan reprogramar a tiempo.">
        <Select value={form.anticipation} onValueChange={set("anticipation")}>
          <SelectTrigger className="w-full h-auto py-3.5 rounded-2xl border-2 text-[14.5px] font-bold">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="12">Avisar 12 horas antes</SelectItem>
            <SelectItem value="24">Avisar 24 horas antes</SelectItem>
            <SelectItem value="48">Avisar 48 horas antes</SelectItem>
          </SelectContent>
        </Select>
      </Field>

      <div className="grid grid-cols-2 gap-3">
        <Field label="Reprogramar desde">
          <Input type="time" value={form.reprogramStart} onChange={(event) => set("reprogramStart")(event.target.value)} className="rounded-2xl border-2" />
        </Field>
        <Field label="Reprogramar hasta">
          <Input type="time" value={form.reprogramEnd} onChange={(event) => set("reprogramEnd")(event.target.value)} aria-invalid={form.reprogramEnd <= form.reprogramStart} className="rounded-2xl border-2" />
        </Field>
      </div>
      {form.reprogramEnd <= form.reprogramStart && <p className="-mt-3 mb-4 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>La hora final debe ser posterior a la inicial.</p>}
      <p className="-mt-3 mb-4 text-[11px] font-bold" style={{ color: "var(--muted-foreground)" }}>
        Franja horaria en la que se puede proponer una nueva fecha si hay que reprogramar.
      </p>

      <Field label="Rango de reprogramación" hint="Hasta cuántos días después de la fecha original se puede mover la actividad si el clima obliga a reprogramar.">
        <Select value={form.reschedule} onValueChange={set("reschedule")}>
          <SelectTrigger className="w-full h-auto py-3.5 rounded-2xl border-2 text-[14.5px] font-bold">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="1">Hasta 1 día después</SelectItem>
            <SelectItem value="3">Hasta 3 días después</SelectItem>
            <SelectItem value="7">Hasta 1 semana después</SelectItem>
          </SelectContent>
        </Select>
      </Field>
    </div>
  );
}

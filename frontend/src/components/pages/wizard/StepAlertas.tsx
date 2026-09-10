import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Card } from "@/components/ui/card";
import type { ReactNode } from "react";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
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

export function StepAlertas({ form, set }: StepProps) {
  return (
    <div className="pt-1">
      <Field label="Ventana de anticipación">
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

      <Field label="Rango de reprogramación">
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

      <Card className="p-4 mt-2 mb-4 rounded-2xl">
        <p className="font-display font-semibold text-[13.5px] mb-2 px-4">Resumen</p>
        <div className="space-y-1.5 text-[12.5px] font-extrabold px-4" style={{ color: "var(--muted-foreground)" }}>
          <p className="truncate">📍 {form.place || "Sin ubicación"}</p>
          <p>
            🌧️ Hasta {form.rain}% · 🌡️ {form.tMin}°–{form.tMax}°C · 💨 {form.wind} km/h
          </p>
          <p>
            👥 {form.min}–{form.max} participantes
          </p>
        </div>
      </Card>

      <div className="rounded-2xl p-3.5 flex gap-2.5" style={{ background: "var(--mint)" }}>
        <span>✅</span>
        <p className="text-[11.5px] font-extrabold" style={{ color: "var(--mint-ink)" }}>
          ¡Todo listo! Revisá los datos antes de publicar.
        </p>
      </div>
    </div>
  );
}

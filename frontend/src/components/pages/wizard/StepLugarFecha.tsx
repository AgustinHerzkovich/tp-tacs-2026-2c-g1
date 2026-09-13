import type { ReactNode } from "react";
import { MapPin } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { MiniStepper } from "@/components/common/MiniStepper";
import type { WizardErrors } from "@/lib/validation";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
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

export function StepLugarFecha({ form, set, errors }: StepProps) {
  return (
    <div>
      <Field label="Ubicación">
        <Input
          placeholder="Buscar dirección o lugar…"
          value={form.place}
          onChange={(e) => set("place")(e.target.value)}
          aria-invalid={!!errors?.place}
          className="h-auto py-3.5 rounded-2xl border-2 text-[15px]"
        />
        <FieldError message={errors?.place} />
      </Field>

      <div className="relative rounded-2xl overflow-hidden h-40 mb-5" style={{ background: "linear-gradient(135deg, #BAE6FD, #A7F3D0)" }}>
        <span className="emoji-3d floaty absolute text-4xl" style={{ left: "15%", top: "18%" }}>
          🗺️
        </span>
        <span className="emoji-3d floaty absolute text-3xl" style={{ right: "18%", top: "50%", animationDelay: ".4s" }}>
          🌳
        </span>
        <button
          type="button"
          className="tap absolute left-1/2 -translate-x-1/2 bottom-4 px-4 py-2.5 rounded-full bg-white shadow-[0_8px_18px_-8px_rgba(58,51,82,.5)] font-display font-semibold text-[12.5px] flex items-center gap-1.5"
        >
          <MapPin className="size-4" /> Tocá para colocar el pin
        </button>
      </div>

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

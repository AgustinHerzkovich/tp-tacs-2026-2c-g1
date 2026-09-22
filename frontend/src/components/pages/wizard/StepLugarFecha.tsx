import { useState, type ReactNode } from "react";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import { MiniStepper } from "@/components/common/MiniStepper";
import { LocationMap } from "@/components/pages/wizard/LocationMap";
import { dateKey, parseDateKey } from "@/components/search/dateRangePresets";
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

function DateField({ value, onChange, invalid }: { value: string; onChange: (value: string) => void; invalid?: boolean }) {
  const [open, setOpen] = useState(false);
  const selected = parseDateKey(value);
  const label = selected ? selected.toLocaleDateString("es-AR", { day: "2-digit", month: "short", year: "numeric" }) : "Elegí una fecha";

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label="Elegir fecha"
          className="w-full h-auto py-3 px-4 rounded-2xl border-2 text-left text-[14.5px] font-bold"
          style={{ borderColor: invalid ? "var(--destructive)" : "var(--border)", color: selected ? "var(--foreground)" : "var(--muted-foreground)" }}
        >
          {label}
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-auto p-4">
        <Calendar
          mode="single"
          selected={selected}
          onSelect={(date) => {
            onChange(date ? dateKey(date) : "");
            setOpen(false);
          }}
        />
      </PopoverContent>
    </Popover>
  );
}

const HOURS = Array.from({ length: 24 }, (_, i) => i);
const MINUTES = [0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55];

function pad(n: number): string {
  return String(n).padStart(2, "0");
}

function TimeField({ value, onChange, invalid }: { value: string; onChange: (value: string) => void; invalid?: boolean }) {
  const [open, setOpen] = useState(false);
  const [hh, mm] = value ? value.split(":") : [undefined, undefined];
  const hour = hh !== undefined ? Number(hh) : null;
  const minute = mm !== undefined ? Number(mm) : null;

  const pick = (h: number, m: number) => onChange(`${pad(h)}:${pad(m)}`);

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label="Elegir hora"
          className="w-full h-auto py-3 px-4 rounded-2xl border-2 text-left text-[14.5px] font-bold"
          style={{ borderColor: invalid ? "var(--destructive)" : "var(--border)", color: value ? "var(--foreground)" : "var(--muted-foreground)" }}
        >
          {value ? `${value} hs` : "Elegí una hora"}
        </button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-64 p-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <p className="mb-2 text-[10.5px] font-extrabold uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Hora</p>
            <div className="grid grid-cols-3 gap-1 max-h-40 overflow-y-auto pr-1">
              {HOURS.map((h) => (
                <button
                  key={h}
                  type="button"
                  onClick={() => pick(h, minute ?? 0)}
                  className="tap rounded-lg py-1.5 text-[12.5px] font-bold"
                  style={h === hour ? { background: "var(--primary)", color: "var(--primary-foreground)" } : { color: "var(--foreground)" }}
                >
                  {pad(h)}
                </button>
              ))}
            </div>
          </div>
          <div>
            <p className="mb-2 text-[10.5px] font-extrabold uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Min</p>
            <div className="grid grid-cols-2 gap-1 max-h-40 overflow-y-auto pr-1">
              {MINUTES.map((m) => (
                <button
                  key={m}
                  type="button"
                  onClick={() => pick(hour ?? 0, m)}
                  className="tap rounded-lg py-1.5 text-[12.5px] font-bold"
                  style={m === minute ? { background: "var(--primary)", color: "var(--primary-foreground)" } : { color: "var(--foreground)" }}
                >
                  {pad(m)}
                </button>
              ))}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="tap mt-3 w-full rounded-xl py-2 text-[12.5px] font-extrabold"
          style={{ background: "var(--secondary)", color: "var(--secondary-foreground)" }}
        >
          Listo
        </button>
      </PopoverContent>
    </Popover>
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
          <DateField value={form.date} onChange={set("date")} invalid={!!errors?.date} />
          <FieldError message={errors?.date} />
        </Field>
        <Field label="Hora">
          <TimeField value={form.time} onChange={set("time")} invalid={!!errors?.time} />
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

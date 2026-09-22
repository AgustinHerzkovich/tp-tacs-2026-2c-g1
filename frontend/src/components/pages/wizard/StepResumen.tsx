import type { ComponentType } from "react";
import { MapPin, CalendarDays, Users, CloudRain, Wind, Thermometer, ImageIcon } from "lucide-react";
import { TYPE_META } from "@/lib/activityVisuals";
import { formatActivityWhen } from "@/lib/formatDate";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
}

function SummaryRow({ icon: Icon, label, value }: { icon: ComponentType<{ className?: string; style?: React.CSSProperties }>; label: string; value: string }) {
  return (
    <div className="flex items-start gap-3 py-3 border-b last:border-0" style={{ borderColor: "var(--border)" }}>
      <span className="flex size-9 shrink-0 items-center justify-center rounded-full" style={{ background: "var(--secondary)" }}>
        <Icon className="size-4" style={{ color: "var(--secondary-foreground)" }} />
      </span>
      <div className="min-w-0">
        <p className="text-[10.5px] font-extrabold uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>{label}</p>
        <p className="text-[13.5px] font-bold">{value}</p>
      </div>
    </div>
  );
}

export function StepResumen({ form }: StepProps) {
  const type = TYPE_META[form.type];

  return (
    <div className="pt-1">
      <div className="mb-5">
        <h3 className="font-brand text-xl">{type.icon} {form.title || "Tu actividad"}</h3>
        <p className="mt-1 text-[13px] font-bold" style={{ color: "var(--muted-foreground)" }}>
          Revisá los datos antes de publicar.
        </p>
      </div>

      <div className="rounded-2xl border-2 px-4" style={{ borderColor: "var(--border)" }}>
        <SummaryRow icon={MapPin} label="Ubicación" value={form.place || "Sin ubicación"} />
        <SummaryRow icon={CalendarDays} label="Fecha y hora" value={form.date && form.time ? formatActivityWhen(`${form.date}T${form.time}:00`) : "Sin definir"} />
        <SummaryRow icon={Users} label="Participantes" value={`${form.min}–${form.max} personas`} />
        <SummaryRow icon={CloudRain} label="Lluvia máxima" value={`${form.rain}%`} />
        <SummaryRow icon={Wind} label="Viento máximo" value={`${form.wind} km/h`} />
        <SummaryRow icon={Thermometer} label="Rango de temperatura" value={`${form.tMin}° a ${form.tMax}°C`} />
        {form.images.length > 0 && (
          <SummaryRow icon={ImageIcon} label="Imágenes" value={`${form.images.length} ${form.images.length === 1 ? "imagen agregada" : "imágenes agregadas"}`} />
        )}
      </div>
    </div>
  );
}

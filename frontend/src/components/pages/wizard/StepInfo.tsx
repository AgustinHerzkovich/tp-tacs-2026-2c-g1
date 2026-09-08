import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { TYPE_META } from "@/data/mockData";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter } from "@/hooks/useWizardForm";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
}

export function StepInfo({ form, set }: StepProps) {
  return (
    <div>
      <div className="mb-5">
        <Label className="mb-2 font-extrabold text-[11.5px] uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
          Título
        </Label>
        <Input
          placeholder="Ej: Trekking Cerro Otto"
          value={form.title}
          onChange={(e) => set("title")(e.target.value)}
          className="h-auto py-3.5 rounded-2xl border-2 text-[15px]"
        />
      </div>
      <div className="mb-5">
        <Label className="mb-2 font-extrabold text-[11.5px] uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
          Descripción
        </Label>
        <Textarea
          rows={4}
          placeholder="Contales de qué se trata, qué llevar, nivel de dificultad…"
          value={form.desc}
          onChange={(e) => set("desc")(e.target.value)}
          className="rounded-2xl border-2 text-[15px] resize-none"
        />
      </div>
      <div className="mb-5">
        <Label className="mb-2 font-extrabold text-[11.5px] uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
          Tipo de actividad
        </Label>
        <div className="grid grid-cols-3 gap-3">
          {Object.entries(TYPE_META).map(([key, m]) => {
            const active = form.type === key;
            return (
              <button
                key={key}
                type="button"
                onClick={() => set("type")(key as WizardFormState["type"])}
                className="tap aspect-square rounded-2xl border-2 flex flex-col items-center justify-center gap-2 font-display font-semibold text-[12.5px]"
                style={active ? { background: m.bg, borderColor: m.bg, color: m.ink } : { background: "#fff", borderColor: "var(--border)", color: "var(--muted-foreground)" }}
              >
                <span className="text-3xl emoji-3d">{m.icon}</span>
                {m.label}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

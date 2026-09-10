import { WIZARD_STEPS } from "@/hooks/useWizardForm";

export function WizardProgress({ step }: { step: number }) {
  return (
    <div className="px-5 pt-2 pb-3">
      <div className="flex items-center gap-2">
        {WIZARD_STEPS.map((_, i) => (
          <div
            key={i}
            className="h-2.5 flex-1 rounded-full transition-colors duration-300"
            style={{ background: i <= step ? "var(--primary)" : "var(--border)" }}
          />
        ))}
      </div>
      <p className="font-display font-semibold text-[12.5px] mt-2.5" style={{ color: "var(--muted-foreground)" }}>
        Paso {step + 1} de 4 · <span style={{ color: "var(--accent-foreground)" }}>{WIZARD_STEPS[step]}</span>
      </p>
    </div>
  );
}

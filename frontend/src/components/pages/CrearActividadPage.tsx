"use client";

import { useState, type ComponentType } from "react";
import { useRouter } from "next/navigation";
import { X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useWizardForm } from "@/hooks/useWizardForm";
import { WizardProgress } from "@/components/pages/wizard/WizardProgress";
import { StepInfo } from "@/components/pages/wizard/StepInfo";
import { StepLugarFecha } from "@/components/pages/wizard/StepLugarFecha";
import { StepClima } from "@/components/pages/wizard/StepClima";
import { StepImagenes } from "@/components/pages/wizard/StepImagenes";
import { StepAlertas } from "@/components/pages/wizard/StepAlertas";
import { ConfirmModal } from "@/components/common/ConfirmModal";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { api, ApiError } from "@/lib/api";
import { toBackendActivityType } from "@/lib/activityMapping";
import { firstInvalidStep, validateAll, type WizardErrors } from "@/lib/validation";
import type { WizardFormState } from "@/types/domain";
import type { FieldSetter, FormPatchSetter } from "@/hooks/useWizardForm";
import type { CreateActivityRequest } from "@/types/backend";

interface StepProps {
  form: WizardFormState;
  set: FieldSetter;
  patch?: FormPatchSetter;
  errors?: WizardErrors;
}

const STEP_COMPONENTS: ComponentType<StepProps>[] = [StepInfo, StepLugarFecha, StepClima, StepImagenes, StepAlertas];

// No UI control exists yet for the reprogramming window's daily hour bounds
// (only its day count, via form.reschedule) — see frontend/TODO.md.
const DEFAULT_REPROGRAMATION_HOURS: [string, string] = ["09:00:00", "21:00:00"];

function buildCreateRequest(form: WizardFormState): CreateActivityRequest {
  return {
    title: form.title.trim(),
    description: form.desc.trim() || undefined,
    type: toBackendActivityType(form.type),
    location: { city: form.place.trim() || null, latitude: form.latitude, longitude: form.longitude },
    dateTime: `${form.date}T${form.time}:00`,
    minParticipants: form.min,
    maxParticipants: form.max,
    weatherConditions: {
      maxRainProbability: form.rain,
      minTemperature: form.tMin,
      maxTemperature: form.tMax,
      maxWindSpeed: form.wind,
    },
    anticipationWindow: Number(form.anticipation),
    reprogramationRange: {
      maxDays: Number(form.reschedule),
      initialHour: DEFAULT_REPROGRAMATION_HOURS[0],
      finalHour: DEFAULT_REPROGRAMATION_HOURS[1],
    },
  };
}

export function CrearActividadPage() {
  const router = useRouter();
  const wizard = useWizardForm();
  const user = useRequireAuth();
  const StepComponent = STEP_COMPONENTS[wizard.step];
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  if (!user) return null;

  const handlePublish = async () => {
    const validationErrors = validateAll(wizard.form);
    const invalidStep = firstInvalidStep(wizard.form);
    if (invalidStep >= 0) {
      wizard.showErrors(validationErrors, invalidStep);
      setSubmitError("Revisá los campos marcados en rojo para poder publicar.");
      return;
    }

    setSubmitError(null);
    setSubmitting(true);
    try {
      const activity = buildCreateRequest(wizard.form);
      const formData = new FormData();
      formData.append("activity", new Blob([JSON.stringify(activity)], { type: "application/json" }));
      wizard.form.images.forEach(({ file }) => formData.append("images", file));
      await api.activities.create(formData);
      wizard.publish();
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : "No pudimos publicar la actividad. Probá de nuevo.");
    } finally {
      setSubmitting(false);
    }
  };

  if (wizard.done) {
    return (
      <div className="fade-in flex-1 flex flex-col items-center justify-center px-8 text-center min-h-screen">
        <div className="text-6xl mb-5 emoji-3d floaty">🎉</div>
        <h2 className="font-display font-semibold text-2xl mb-2">¡Actividad publicada!</h2>
        <p className="text-[14px] font-bold mb-8" style={{ color: "var(--muted-foreground)" }}>
          Te avisamos si cambia el clima o se suma alguien nuevo.
        </p>
        <Button className="h-auto py-3.5 px-6 rounded-2xl font-display font-semibold" onClick={() => router.push("/mis-actividades")}>
          Ver mis actividades
        </Button>
      </div>
    );
  }

  return (
    <div className="fade-in flex flex-col min-h-screen lg:min-h-0 lg:max-w-3xl lg:w-full lg:mx-auto lg:my-10 lg:bg-white lg:rounded-3xl lg:shadow-xl lg:overflow-hidden">
      <div className="flex items-center justify-between px-5 pt-6 pb-1">
        <h2 className="font-display font-semibold text-lg">Nueva actividad</h2>
        <button
          onClick={wizard.requestDiscard}
          className="tap w-9 h-9 rounded-full border-2 flex items-center justify-center"
          style={{ background: "#fff", borderColor: "var(--border)" }}
          aria-label="Cerrar"
        >
          <X className="size-4" />
        </button>
      </div>

      <WizardProgress step={wizard.step} />

      <div key={wizard.step} className={`flex-1 px-5 pb-4 ${wizard.direction > 0 ? "anim-slide-right" : "anim-slide-left"}`}>
        {StepComponent && <StepComponent form={wizard.form} set={wizard.set} patch={wizard.patch} errors={wizard.errors} />}
        {submitError && Object.keys(wizard.errors).length > 0 && (
          <p className="mt-4 text-[12.5px] font-extrabold rounded-2xl p-3" style={{ background: "var(--rose)", color: "var(--rose-ink)" }}>
            {submitError}
          </p>
        )}
      </div>

      <div className="px-5 py-4 border-t-2 flex gap-3" style={{ borderColor: "var(--border)", background: "var(--background)" }}>
        {!wizard.isFirstStep && (
          <Button variant="outline" className="flex-1 h-auto py-3.5 rounded-2xl font-display font-semibold" onClick={wizard.back} disabled={submitting}>
            Atrás
          </Button>
        )}
        {wizard.isLastStep ? (
          <Button className="flex-1 h-auto py-3.5 rounded-2xl font-display font-semibold" onClick={() => void handlePublish()} disabled={submitting}>
            {submitting ? "Publicando…" : "Publicar Actividad"}
          </Button>
        ) : (
          <Button className="flex-1 h-auto py-3.5 rounded-2xl font-display font-semibold" onClick={() => { setSubmitError(null); wizard.next(); }}>
            Continuar
          </Button>
        )}
      </div>

      <ConfirmModal
        open={wizard.discardOpen}
        onOpenChange={(v) => !v && wizard.cancelDiscard()}
        title="¿Descartar este borrador?"
        description="Vas a perder la información que ya cargaste para esta actividad."
        confirmLabel="Descartar"
        destructive
        onConfirm={() => wizard.confirmDiscard(() => router.push("/mis-actividades"))}
      />
    </div>
  );
}

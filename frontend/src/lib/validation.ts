import type { WizardFormState } from "@/types/domain";

export type WizardErrors = Partial<Record<keyof WizardFormState, string>>;

function validateInfo(form: WizardFormState): WizardErrors {
  const errors: WizardErrors = {};
  const title = form.title.trim();
  if (!title) {
    errors.title = "Ingresá un título para la actividad.";
  } else if (title.length < 3) {
    errors.title = "El título tiene que tener al menos 3 caracteres.";
  }
  const desc = form.desc.trim();
  if (!desc) {
    errors.desc = "Poné una descripción de la actividad.";
  } else if (desc.length > 500) {
    errors.desc = "La descripción no puede superar los 500 caracteres.";
  }
  return errors;
}

function validateLugarFecha(form: WizardFormState): WizardErrors {
  const errors: WizardErrors = {};
  if (!form.place.trim()) {
    errors.place = "Indicá la ubicación de la actividad.";
  }
  if (form.min < 1) {
    errors.min = "Se necesita al menos 1 participante.";
  }
  if (form.max > 99) {
    errors.max = "El máximo no puede superar los 99 participantes.";
  }
  if (form.max < form.min) {
    errors.max = "El máximo no puede ser menor que el mínimo.";
  }
  if (!form.date) {
    errors.date = "Elegí una fecha.";
  } else if (!form.time) {
    errors.time = "Elegí una hora.";
  } else {
    const when = new Date(`${form.date}T${form.time}:00`);
    if (Number.isNaN(when.getTime())) {
      errors.date = "La fecha no es válida.";
    } else if (when.getTime() <= Date.now()) {
      errors.date = "La fecha y hora tienen que estar en el futuro.";
    }
  }
  return errors;
}

/** Returns the errors for a single wizard step (0..3). Slider/select steps
 * (clima, alertas) are constrained by their own controls, so they validate empty. */
export function validateStep(step: number, form: WizardFormState): WizardErrors {
  if (step === 0) return validateInfo(form);
  if (step === 1) return validateLugarFecha(form);
  return {};
}

/** Union of every step's errors, used right before publishing. */
export function validateAll(form: WizardFormState): WizardErrors {
  return { ...validateInfo(form), ...validateLugarFecha(form) };
}

/** Index of the first step whose current value is invalid, or -1. */
export function firstInvalidStep(form: WizardFormState): number {
  for (let step = 0; step < 4; step += 1) {
    if (Object.keys(validateStep(step, form)).length > 0) return step;
  }
  return -1;
}
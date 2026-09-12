"use client";

import { useRef, useState } from "react";
import type { WizardFormState } from "@/types/domain";
import { validateStep, type WizardErrors } from "@/lib/validation";

export const WIZARD_STEPS = ["Info básica", "Lugar y fecha", "Clima", "Alertas"] as const;

const INITIAL_FORM: WizardFormState = {
  title: "",
  desc: "",
  type: "outdoor",
  place: "",
  date: "",
  time: "",
  min: 4,
  max: 12,
  rain: 40,
  wind: 30,
  tMin: 12,
  tMax: 28,
  anticipation: "24",
  reschedule: "3",
};

export type FieldSetter = <K extends keyof WizardFormState>(key: K) => (value: WizardFormState[K]) => void;

export interface UseWizardForm {
  step: number;
  direction: 1 | -1;
  done: boolean;
  form: WizardFormState;
  errors: WizardErrors;
  set: FieldSetter;
  next: () => void;
  back: () => void;
  goTo: (nextStep: number) => void;
  showErrors: (allErrors: WizardErrors, targetStep?: number) => void;
  publish: () => void;
  reset: () => void;
  discardOpen: boolean;
  requestDiscard: () => void;
  cancelDiscard: () => void;
  confirmDiscard: (onDiscarded?: () => void) => void;
  isLastStep: boolean;
  isFirstStep: boolean;
}

/** Drives the 4-step "Crear actividad" wizard: step navigation with slide
 * direction, form field state, per-step validation, the discard-draft confirm
 * dialog, and the publish/done transition. */
export function useWizardForm(): UseWizardForm {
  const [step, setStep] = useState(0);
  const [direction, setDirection] = useState<1 | -1>(1);
  const [done, setDone] = useState(false);
  const [discardOpen, setDiscardOpen] = useState(false);
  const [form, setForm] = useState<WizardFormState>(INITIAL_FORM);
  const [errors, setErrors] = useState<WizardErrors>({});
  // Steps the user already tried to advance from; once attempted, their
  // errors stay visible and revalidate live as the user types.
  const attempted = useRef(new Set<number>());

  const validate = (targetStep: number, nextForm: WizardFormState): WizardErrors =>
    validateStep(targetStep, nextForm);

  const set: FieldSetter = (key) => (value) => {
    const nextForm = { ...form, [key]: value };
    setForm(nextForm);
    if (attempted.current.has(step)) setErrors(validate(step, nextForm));
  };

  const goTo = (nextStep: number) => {
    setDirection(nextStep > step ? 1 : -1);
    setStep(nextStep);
  };

  const next = () => {
    attempted.current.add(step);
    const stepErrors = validate(step, form);
    if (Object.keys(stepErrors).length > 0) {
      setErrors(stepErrors);
      return;
    }
    setErrors({});
    goTo(Math.min(step + 1, WIZARD_STEPS.length - 1));
  };

  const back = () => goTo(Math.max(step - 1, 0));

  const showErrors = (allErrors: WizardErrors, targetStep?: number) => {
    setErrors(allErrors);
    if (targetStep !== undefined && targetStep !== step) {
      attempted.current.add(targetStep);
      setDirection(targetStep > step ? 1 : -1);
      setStep(targetStep);
    }
  };

  const publish = () => setDone(true);

  const reset = () => {
    setStep(0);
    setDone(false);
    setForm(INITIAL_FORM);
    setErrors({});
    attempted.current.clear();
  };

  const requestDiscard = () => setDiscardOpen(true);
  const cancelDiscard = () => setDiscardOpen(false);
  const confirmDiscard = (onDiscarded?: () => void) => {
    setDiscardOpen(false);
    reset();
    onDiscarded?.();
  };

  return {
    step,
    direction,
    done,
    form,
    errors,
    set,
    next,
    back,
    goTo,
    showErrors,
    publish,
    reset,
    discardOpen,
    requestDiscard,
    cancelDiscard,
    confirmDiscard,
    isLastStep: step === WIZARD_STEPS.length - 1,
    isFirstStep: step === 0,
  };
}
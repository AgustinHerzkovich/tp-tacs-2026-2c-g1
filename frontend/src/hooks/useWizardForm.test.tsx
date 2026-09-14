import { act, renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useWizardForm } from "@/hooks/useWizardForm";

describe("useWizardForm", () => {
  it("starts on step 0 with the default draft and no errors", () => {
    const { result } = renderHook(() => useWizardForm());
    expect(result.current.step).toBe(0);
    expect(result.current.done).toBe(false);
    expect(result.current.isFirstStep).toBe(true);
    expect(result.current.isLastStep).toBe(false);
    expect(result.current.errors).toEqual({});
    expect(result.current.form).toMatchObject({
      title: "",
      type: "outdoor",
      min: 4,
      max: 12,
      reprogramStart: "09:00",
      reprogramEnd: "21:00",
    });
  });

  it("blocks advancing on an invalid step and then validates live as the user types", () => {
    const { result } = renderHook(() => useWizardForm());

    act(() => result.current.next());
    expect(result.current.step).toBe(0);
    expect(result.current.errors.title).toBeDefined();
    expect(result.current.errors.desc).toBeDefined();

    act(() => result.current.set("title")("Partido de vóley"));
    expect(result.current.errors.title).toBeUndefined();

    act(() => result.current.set("desc")("Jugamos en el parque."));
    expect(result.current.errors).toEqual({});

    act(() => result.current.next());
    expect(result.current.step).toBe(1);
  });

  it("navigates through the steps with validation and back/forward direction", () => {
    const { result } = renderHook(() => useWizardForm());

    act(() => result.current.next());
    act(() => result.current.back());
    expect(result.current.step).toBe(0);
    expect(result.current.direction).toBe(-1);

    act(() => result.current.set("title")("Título válido"));
    act(() => result.current.set("desc")("Descripción"));
    act(() => result.current.next());
    expect(result.current.step).toBe(1);

    act(() =>
      result.current.patch({
        place: "Parque Centenario",
        latitude: -34.6,
        longitude: -58.4,
        date: "2099-12-31",
        time: "18:00",
      }),
    );
    act(() => result.current.next());
    expect(result.current.step).toBe(2);
    expect(result.current.direction).toBe(1);

    act(() => result.current.goTo(4));
    expect(result.current.step).toBe(4);
    expect(result.current.isLastStep).toBe(true);
  });

  it("rejects a reprogramming range whose end is not later than the start", () => {
    const { result } = renderHook(() => useWizardForm());
    act(() => result.current.goTo(4));
    act(() => result.current.patch({ reprogramStart: "20:00", reprogramEnd: "09:00" }));
    act(() => result.current.next());
    expect(result.current.errors.reprogramEnd).toMatch(/posterior/i);
    expect(result.current.step).toBe(4);
  });

  it("publishes and discards the draft", () => {
    const { result } = renderHook(() => useWizardForm());
    const onDiscarded = vi.fn();

    act(() => result.current.publish());
    expect(result.current.done).toBe(true);

    act(() => result.current.requestDiscard());
    expect(result.current.discardOpen).toBe(true);
    act(() => result.current.cancelDiscard());
    expect(result.current.discardOpen).toBe(false);

    act(() => {
      result.current.requestDiscard();
      result.current.confirmDiscard(onDiscarded);
    });
    expect(result.current.discardOpen).toBe(false);
    expect(result.current.done).toBe(false);
    expect(result.current.step).toBe(0);
    expect(result.current.errors).toEqual({});
    expect(onDiscarded).toHaveBeenCalledTimes(1);
  });

  it("shows errors jumping to the target step when the backend reports them", () => {
    const { result } = renderHook(() => useWizardForm());
    act(() => result.current.patch({ reprogramStart: "21:00", reprogramEnd: "08:00" }));
    act(() => result.current.showErrors({ reprogramEnd: "Hora inválida" }, 4));
    expect(result.current.step).toBe(4);
    expect(result.current.errors.reprogramEnd).toBe("Hora inválida");
  });
});
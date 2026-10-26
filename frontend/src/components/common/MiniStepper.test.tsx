import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { MiniStepper } from "./MiniStepper";

function setup(value = 4) {
  const onChange = vi.fn();
  render(<MiniStepper label="Participantes" value={value} min={1} max={99} onChange={onChange} />);
  return { onChange, input: screen.getByLabelText("Participantes") };
}

describe("MiniStepper", () => {
  it("applies a typed number when the field loses focus", () => {
    const { onChange, input } = setup();

    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: "25" } });
    expect(onChange).not.toHaveBeenCalled();
    fireEvent.blur(input);

    expect(onChange).toHaveBeenCalledWith(25);
  });

  it("clamps a typed number to the allowed range", () => {
    const { onChange, input } = setup();

    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: "0" } });
    fireEvent.blur(input);

    expect(onChange).toHaveBeenCalledWith(1);
  });

  it("ignores non-digits and keeps the previous value when left empty", () => {
    const { onChange, input } = setup(7);

    fireEvent.focus(input);
    fireEvent.change(input, { target: { value: "abc" } });
    expect(input).toHaveValue("");
    fireEvent.blur(input);

    expect(onChange).not.toHaveBeenCalled();
    expect(input).toHaveValue("7");
  });

  it("still steps with the buttons", () => {
    const { onChange } = setup(4);
    const [minus, plus] = screen.getAllByRole("button");

    fireEvent.click(plus!);
    fireEvent.click(minus!);

    expect(onChange).toHaveBeenNthCalledWith(1, 5);
    expect(onChange).toHaveBeenNthCalledWith(2, 3);
  });
});

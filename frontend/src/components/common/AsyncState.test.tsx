import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ErrorState, LoadingState } from "@/components/common/AsyncState";

describe("LoadingState", () => {
  it("renders a polite loading message with the given label", () => {
    render(<LoadingState label="Cargando actividades" />);
    const element = screen.getByText("Cargando actividades");
    expect(element).toBeInTheDocument();
    expect(element).toHaveAttribute("aria-live", "polite");
  });

  it("defaults to Cargando...", () => {
    render(<LoadingState />);
    expect(screen.getByText("Cargando...")).toBeInTheDocument();
  });
});

describe("ErrorState", () => {
  it("shows the message as an alert and offers a retry action", async () => {
    const retry = vi.fn();
    render(<ErrorState message="No pudimos cargar la actividad." retry={retry} />);

    const alert = screen.getByRole("alert");
    expect(alert).toHaveTextContent("No pudimos cargar la actividad.");

    await userEvent.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(retry).toHaveBeenCalledTimes(1);
  });

  it("renders without a retry button when none is provided", () => {
    render(<ErrorState message="Error 500" />);
    expect(screen.getByRole("alert")).toHaveTextContent("Error 500");
    expect(screen.queryByRole("button", { name: "Reintentar" })).not.toBeInTheDocument();
  });
});
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LandingPage } from "@/components/pages/LandingPage";

const { login, register, replace, authState } = vi.hoisted(() => ({
  login: vi.fn(),
  register: vi.fn(),
  replace: vi.fn(),
  authState: { initialized: true, isAuthenticated: false },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
}));

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ ...authState, login, register }),
}));

beforeEach(() => {
  login.mockReset();
  register.mockReset();
  replace.mockReset();
  authState.initialized = true;
  authState.isAuthenticated = false;
});

describe("LandingPage", () => {
  it("shows the pitch and the call to action for a signed-out visitor", () => {
    render(<LandingPage />);
    expect(screen.getByText(/Nosotros vigilamos/)).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Unite a la comunidad" }).length).toBeGreaterThan(0);
    expect(replace).not.toHaveBeenCalled();
  });

  it("starts the Keycloak registration flow when the hero call-to-action is clicked", async () => {
    const user = userEvent.setup();
    render(<LandingPage />);

    const [, heroCta] = screen.getAllByRole("button", { name: "Unite a la comunidad" });
    await user.click(heroCta as HTMLElement);
    expect(register).toHaveBeenCalledTimes(1);
    expect(login).not.toHaveBeenCalled();
  });

  it("starts the Keycloak login flow when the Ingresar link is clicked", async () => {
    const user = userEvent.setup();
    render(<LandingPage />);

    await user.click(screen.getByRole("button", { name: "Ingresar" }));
    expect(login).toHaveBeenCalledTimes(1);
    expect(register).not.toHaveBeenCalled();
  });

  it("redirects an already signed-in visitor straight to Explorar", () => {
    authState.isAuthenticated = true;
    render(<LandingPage />);
    expect(replace).toHaveBeenCalledWith("/explorar");
  });

  it("disables every call-to-action while the SSO check is still running", () => {
    authState.initialized = false;
    render(<LandingPage />);
    expect(screen.getByRole("button", { name: /Comprobando sesión/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Ingresar" })).toBeDisabled();
  });
});

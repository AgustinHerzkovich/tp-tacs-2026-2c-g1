import { describe, expect, it, vi } from "vitest";
import { useAuth } from "@/hooks/useAuth";
import { createTestStore, renderHookWithStore } from "@/test/utils";

const login = vi.fn<() => Promise<void>>();
const logout = vi.fn<() => Promise<void>>();

vi.mock("@/lib/keycloak", () => ({
  getKeycloak: () => ({ login, logout }),
}));

const ADMIN = {
  id: "admin-1",
  name: "Ada Lovelace",
  roles: ["ADMIN", "USER"],
};

describe("useAuth", () => {
  it("starts without a session and exposes helper flags", () => {
    const { result } = renderHookWithStore(useAuth);
    expect(result.current.user).toBeNull();
    expect(result.current.initialized).toBe(true);
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.hasRole("ADMIN")).toBe(false);
  });

  it("is not authenticated until Keycloak has initialized", () => {
    const { result } = renderHookWithStore(useAuth, {
      preloadedSession: { user: ADMIN, initialized: false },
    });
    expect(result.current.isAuthenticated).toBe(false);
  });

  it("exposes the current user and role checks once authenticated", () => {
    const { result } = renderHookWithStore(useAuth, { preloadedSession: { user: ADMIN } });
    expect(result.current.user).toMatchObject({ id: "admin-1", name: "Ada Lovelace" });
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.hasRole("ADMIN")).toBe(true);
    expect(result.current.hasRole("USER")).toBe(true);
    expect(result.current.hasRole("FUTBOL")).toBe(false);
  });

  it("delegates login/logout to the Keycloak adapter", async () => {
    const { result } = renderHookWithStore(useAuth);
    await result.current.login();
    expect(login).toHaveBeenCalledWith({ redirectUri: expect.stringContaining("http") });

    await result.current.logout();
    expect(logout).toHaveBeenCalledWith({ redirectUri: expect.stringContaining("/login") });
  });
});

it("session slice transitions from anonymous to authenticated", () => {
  const store = createTestStore();
  const { dispatch } = store;
  dispatch({ type: "session/anonymous" });
  expect(store.getState().session).toEqual({ user: null, initialized: true });
  dispatch({ type: "session/authenticated", payload: ADMIN });
  expect(store.getState().session.user?.roles).toEqual(["ADMIN", "USER"]);
});
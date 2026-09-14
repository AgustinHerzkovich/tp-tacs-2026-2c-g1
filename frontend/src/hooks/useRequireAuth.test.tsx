import { describe, expect, it, vi } from "vitest";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { renderHookWithStore } from "@/test/utils";

const replace = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/explorar",
}));

const USER = { id: "u1", name: "Una", roles: ["USER"] };

describe("useRequireAuth", () => {
  it("redirects to the login page keeping the original route once Keycloak confirms no session", async () => {
    renderHookWithStore(useRequireAuth, {
      preloadedSession: { user: null, initialized: true },
    });
    expect(replace).toHaveBeenCalledWith("/login?returnTo=%2Fexplorar");
  });

  it("does not redirect while the SSO check is still running", () => {
    replace.mockClear();
    renderHookWithStore(useRequireAuth, { preloadedSession: { user: null, initialized: false } });
    expect(replace).not.toHaveBeenCalled();
  });

  it("does not redirect for an authenticated user and returns it", () => {
    replace.mockClear();
    const { result } = renderHookWithStore(useRequireAuth, { preloadedSession: { user: USER } });
    expect(replace).not.toHaveBeenCalled();
    expect(result.current).toMatchObject({ id: "u1" });
  });
});
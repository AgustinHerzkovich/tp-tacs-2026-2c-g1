import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { AuthBootstrap } from "@/auth/AuthBootstrap";
import { createTestStore } from "@/test/utils";
import { Provider } from "react-redux";
import type { KeycloakTokenParsed } from "keycloak-js";

interface FakeKeycloak {
  authenticated?: boolean;
  tokenParsed?: KeycloakTokenParsed | undefined;
  onAuthSuccess?: () => void;
  onAuthRefreshSuccess?: () => void;
  onAuthLogout?: () => void;
  onAuthError?: () => void;
  onAuthRefreshError?: () => void;
  onTokenExpired?: () => void;
  init: ReturnType<typeof vi.fn<() => Promise<boolean>>>;
  updateToken: ReturnType<typeof vi.fn<() => Promise<boolean>>>;
}

let adapter: FakeKeycloak;

vi.mock("@/lib/keycloak", () => ({
  getKeycloak: () => adapter,
}));

function makeAdapter(overrides: Partial<FakeKeycloak> = {}): FakeKeycloak {
  return {
    init: vi.fn().mockResolvedValue(true),
    updateToken: vi.fn().mockResolvedValue(true),
    ...overrides,
  };
}

function renderBootstrap() {
  const store = createTestStore();
  render(
    <Provider store={store}>
      <AuthBootstrap />
    </Provider>,
  );
  return store;
}

const TOKEN: KeycloakTokenParsed = {
  sub: "sub-123",
  name: "Vale Ríos",
  realm_access: { roles: ["USER"] },
  iss: "",
  aud: "",
  exp: 0,
  iat: 0,
};

describe("AuthBootstrap (check-sso)", () => {
  it("initializes the adapter with check-sso and pkce", async () => {
    adapter = makeAdapter();
    renderBootstrap();
    await waitFor(() => expect(adapter.init).toHaveBeenCalledTimes(1));
    const initArgs = adapter.init.mock.calls as unknown as [object | undefined][];
    const initArg = initArgs[0]?.[0] as unknown as Record<string, unknown>;
    expect(initArg.onLoad).toBe("check-sso");
    expect(initArg.pkceMethod).toBe("S256");
    expect(initArg.checkLoginIframe).toBe(false);
  });

  it("restores the session from the ID token after a successful init", async () => {
    adapter = makeAdapter({ tokenParsed: TOKEN as never });
    const store = renderBootstrap();
    await waitFor(() => expect(store.getState().session.user?.id).toBe("sub-123"));
    expect(store.getState().session.user?.name).toBe("Vale Ríos");
    expect(store.getState().session.user?.roles).toEqual(["USER"]);
    expect(store.getState().session.initialized).toBe(true);
  });

  it("marks the session anonymous when init reports no active SSO session", async () => {
    adapter = makeAdapter({ tokenParsed: undefined });
    const store = renderBootstrap();
    await waitFor(() => expect(store.getState().session.initialized).toBe(true));
    expect(store.getState().session.user).toBeNull();
  });

  it("marks the session anonymous when init fails (Keycloak unreachable)", async () => {
    adapter = makeAdapter({ init: vi.fn().mockRejectedValue(new Error("down")) });
    const store = renderBootstrap();
    await waitFor(() => expect(store.getState().session.initialized).toBe(true));
    expect(store.getState().session.user).toBeNull();
  });

  it("keeps the session in sync when a token refresh succeeds", async () => {
    adapter = makeAdapter();
    const store = renderBootstrap();
    await waitFor(() => expect(adapter.init).toHaveBeenCalled());
    adapter.tokenParsed = TOKEN as never;
    adapter.onAuthSuccess?.();
    expect(store.getState().session.user?.id).toBe("sub-123");
  });

  it("clears the session on logout / auth errors", async () => {
    adapter = makeAdapter({ tokenParsed: TOKEN as never });
    const store = renderBootstrap();
    await waitFor(() => expect(store.getState().session.user).not.toBeNull());
    adapter.onAuthLogout?.();
    expect(store.getState().session.user).toBeNull();
  });
});
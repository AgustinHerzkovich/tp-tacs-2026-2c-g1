import { describe, expect, it, vi } from "vitest";
import type Keycloak from "keycloak-js";
import { createRedirectAdapter } from "@/lib/keycloak";

function setup() {
  const keycloak = {
    createLoginUrl: vi.fn().mockResolvedValue("https://idp.test/auth"),
    createRegisterUrl: vi.fn().mockResolvedValue("https://idp.test/register"),
    createLogoutUrl: vi.fn().mockReturnValue("https://idp.test/logout"),
    createAccountUrl: vi.fn().mockReturnValue("https://idp.test/account"),
  } as unknown as Keycloak;
  const navigation = { assign: vi.fn(), replace: vi.fn() };
  return { adapter: createRedirectAdapter(keycloak, navigation), navigation };
}

describe("createRedirectAdapter", () => {
  it("replaces the history entry for the silent session check, so a reload never duplicates the page", async () => {
    const { adapter, navigation } = setup();

    void adapter.login({ prompt: "none" });

    await vi.waitFor(() => expect(navigation.replace).toHaveBeenCalledWith("https://idp.test/auth"));
    expect(navigation.assign).not.toHaveBeenCalled();
  });

  it("adds a history entry for an interactive login, so back returns from the login form", async () => {
    const { adapter, navigation } = setup();

    void adapter.login({ redirectUri: "https://app.test/explorar" });

    await vi.waitFor(() => expect(navigation.assign).toHaveBeenCalledWith("https://idp.test/auth"));
    expect(navigation.replace).not.toHaveBeenCalled();
  });

  it("adds a history entry for registration and replaces it on logout", async () => {
    const { adapter, navigation } = setup();

    void adapter.register();
    await vi.waitFor(() => expect(navigation.assign).toHaveBeenCalledWith("https://idp.test/register"));

    await adapter.logout();
    expect(navigation.replace).toHaveBeenCalledWith("https://idp.test/logout");
  });
});

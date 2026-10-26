import Keycloak, { type KeycloakAdapter } from "keycloak-js";

let instance: Keycloak | undefined;

function requiredEnvironmentVariable(name: string, value: string | undefined): string {
  if (!value) throw new Error(`Missing required environment variable ${name}`);
  return value;
}

/** Returns the single browser-side OIDC client. Tokens stay in this instance's memory. */
export function getKeycloak(): Keycloak {
  if (typeof window === "undefined") throw new Error("Keycloak is only available in the browser");

  instance ??= new Keycloak({
    url: requiredEnvironmentVariable("NEXT_PUBLIC_KEYCLOAK_URL", process.env.NEXT_PUBLIC_KEYCLOAK_URL),
    realm: requiredEnvironmentVariable("NEXT_PUBLIC_KEYCLOAK_REALM", process.env.NEXT_PUBLIC_KEYCLOAK_REALM),
    clientId: requiredEnvironmentVariable(
      "NEXT_PUBLIC_KEYCLOAK_CLIENT_ID",
      process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID,
    ),
  });

  return instance;
}

/** How the adapter leaves the page; injectable so it can be tested without a real navigation. */
export interface PageNavigation {
  assign: (url: string) => void;
  replace: (url: string) => void;
}

const BROWSER_NAVIGATION: PageNavigation = {
  assign: (url) => window.location.assign(url),
  replace: (url) => window.location.replace(url),
};

/** Same redirects as keycloak-js's default adapter, except that the silent
 * session check done on every page load (`prompt=none`) replaces the current
 * history entry instead of adding one.
 *
 * Tokens live only in memory, so each full load goes to Keycloak and comes back
 * to the same URL. With the default adapter that round trip left the page twice
 * in the history: after a reload, "back" landed on the same page again, which
 * ran the check again and trapped the user there. An interactive login or
 * registration still adds an entry, so "back" from the login form returns to
 * the app. */
export function createRedirectAdapter(
  keycloak: Keycloak,
  navigation: PageNavigation = BROWSER_NAVIGATION,
): KeycloakAdapter {
  const pending = () => new Promise<void>(() => {});
  return {
    login: async (options) => {
      const url = await keycloak.createLoginUrl(options);
      if (options?.prompt === "none") navigation.replace(url);
      else navigation.assign(url);
      return pending();
    },
    register: async (options) => {
      navigation.assign(await keycloak.createRegisterUrl(options));
      return pending();
    },
    logout: async (options) => {
      navigation.replace(keycloak.createLogoutUrl(options));
    },
    accountManagement: async () => {
      navigation.assign(keycloak.createAccountUrl());
      return pending();
    },
    redirectUri: (options) => options?.redirectUri || keycloak.redirectUri || window.location.href,
  };
}

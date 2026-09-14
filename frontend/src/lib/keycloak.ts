import Keycloak from "keycloak-js";

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

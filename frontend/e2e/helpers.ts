import type { Page } from "@playwright/test";

export interface Account {
  username: string;
  password: string;
}

/**
 * Credentials for the E2E run. The users must exist in the `solnotfound`
 * realm (see keycloak-import/realm-export.json and the README setup notes).
 */
export const USER: Account = {
  username: process.env.E2E_USER_USERNAME ?? "alumno",
  password: process.env.E2E_USER_PASSWORD ?? "alumno",
};

export const ADMIN: Account = {
  username: process.env.E2E_ADMIN_USERNAME ?? "admin",
  password: process.env.E2E_ADMIN_PASSWORD ?? "admin",
};

/**
 * Logs in against the realm through the Keycloak login page. The frontend
 * shows Planazo's own screen with the "Ingresar o registrarme" CTA, which
 * redirects to Keycloak where we fill the standard #username/#password form.
 */
export async function loginAs(page: Page, { username, password }: Account): Promise<void> {
  await page.goto("/login");
  await page.getByRole("button", { name: "Ingresar o registrarme" }).click();

  const nameInput = page.locator("#username");
  await nameInput.waitFor({ timeout: 30_000 });
  await nameInput.fill(username);
  await page.locator("#password").fill(password);
  await page
    .locator("#kc-login")
    .or(page.getByRole("button", { name: /ingres|entrar|sign in|sign-in/i }))
    .first()
    .click();

  await page.waitForURL(/\/mis-actividades/, { timeout: 30_000 });
}
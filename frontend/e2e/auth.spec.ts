import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

test.describe("login y logout", () => {
  test("inicia sesión con Keycloak y aterriza en Explorar", async ({ page }) => {
    await loginAs(page, USER);

    await expect(page).toHaveURL(/\/explorar/);
    await expect(page.getByPlaceholder("Buscar actividades")).toBeVisible();
  });

  test("cierra sesión desde el avatar y el token deja de valer", async ({ page }) => {
    await loginAs(page, USER);

    await page.getByRole("button", { name: "Cerrar sesión" }).click();
    await page.waitForURL(/\/login|\/keycloak|logout/, { timeout: 30_000 });

    await page.goto("/mis-actividades");
    await expect(page).toHaveURL(/\/login|\/keycloak/, { timeout: 30_000 });
  });

  test("redirige a /login cuando no hay sesión", async ({ page }) => {
    await page.goto("/mis-actividades");
    await expect(page).toHaveURL(/\/login/, { timeout: 30_000 });
    await expect(page.getByRole("button", { name: "Ingresar o registrarme" })).toBeVisible();
  });
});
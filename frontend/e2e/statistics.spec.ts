import { expect, test } from "@playwright/test";
import { ADMIN, loginAs, USER } from "./helpers";

test.describe("estadísticas admin", () => {
  test("el admin ve el panel con presets, métricas y open-meteo", async ({ page }) => {
    await loginAs(page, ADMIN);

    await page.goto("/estadisticas");
    await expect(page.getByText("Estadísticas")).toBeVisible();

    for (const preset of ["Últimos 7 días", "Últimos 30 días", "Últimos 90 días", "Este mes"]) {
      await expect(page.getByRole("button", { name: preset, exact: true })).toBeVisible();
    }

    for (const metric of ["Creadas", "Reprogramadas", "Canceladas", "Suspendidas por clima"]) {
      await expect(page.getByText(metric)).toBeVisible();
    }
    await expect(page.getByText("Open-Meteo")).toBeVisible();
  });

  test("un usuario sin ADMIN es redirigido desde /estadisticas", async ({ page }) => {
    await loginAs(page, USER);

    await page.goto("/estadisticas");
    await expect(page).toHaveURL(/\/mis-actividades/, { timeout: 15_000 });
  });
});
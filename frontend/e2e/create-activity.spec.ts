import { expect, test, type Page } from "@playwright/test";
import { loginAs, USER } from "./helpers";

/** 1x1 PNG para el paso de imágenes. */
const PNG_1X1 =
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";

function futureDayISO(daysFromNow: number): string {
  const date = new Date();
  date.setDate(date.getDate() + daysFromNow);
  return date.toISOString().slice(0, 10);
}

async function fillWizard(page: Page, { withImages }: { withImages: boolean }): Promise<void> {
  await page.goto("/crear");
  await expect(page.getByText("Nueva actividad")).toBeVisible();

  // Paso 1 — Info básica.
  await page.getByLabel("Título").fill("E2E Trekking costero");
  await page.getByLabel("Descripción").fill("Actividad creada por una prueba end-to-end.");
  await page.getByRole("button", { name: "Outdoor", exact: true }).click();
  await page.getByRole("button", { name: "Continuar" }).click();

  // Paso 2 — Lugar y fecha.
  const search = page.getByPlaceholder("Buscar dirección o lugar…");
  await search.fill("La Boca, Buenos Aires, Argentina");
  await page.getByRole("button", { name: "Buscar ubicación" }).click();
  const result = page.locator("button", { hasText: /La Boca/i }).first();
  await result.waitFor({ timeout: 20_000 });
  await result.click();
  await page.locator('input[type="date"]').fill(futureDayISO(3));
  await page.locator('input[type="time"]').fill("14:00");
  await page.getByRole("button", { name: "Continuar" }).click();

  // Paso 3 — Clima (rango por defecto).
  await page.getByRole("button", { name: "Continuar" }).click();

  // Paso 4 — Imágenes (opcionales).
  if (withImages) {
    await page.locator('input[type="file"]').setInputFiles({
      name: "foto.png",
      mimeType: "image/png",
      buffer: Buffer.from(PNG_1X1, "base64"),
    });
    await expect(page.getByText("Portada")).toBeVisible();
  }
  await page.getByRole("button", { name: "Continuar" }).click();

  // Paso 5 — Alertas: publicar directo.
  await page.getByRole("button", { name: "Publicar Actividad" }).click();
}

test.describe("creación de actividad", () => {
  test("crea una actividad sin imágenes", async ({ page }) => {
    await loginAs(page, USER);
    await fillWizard(page, { withImages: false });

    await expect(page.getByText("¡Actividad publicada!")).toBeVisible({ timeout: 30_000 });
    await page.getByRole("button", { name: "Ver mis actividades" }).click();
    await expect(page).toHaveURL(/\/mis-actividades/);
  });

  test("crea una actividad con imágenes y marca la portada", async ({ page }) => {
    await loginAs(page, USER);
    await fillWizard(page, { withImages: true });

    await expect(page.getByText("¡Actividad publicada!")).toBeVisible({ timeout: 30_000 });
  });
});
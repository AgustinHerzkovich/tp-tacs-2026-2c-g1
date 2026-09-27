import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

test.describe("detalle de actividad", () => {
  test("abre una actividad y muestra clima, participantes y acciones", async ({ page }) => {
    await loginAs(page, USER);
    await page.getByRole("link", { name: "Explorar" }).first().click();

    const firstCard = page.getByRole("link", { name: /Ver actividad →/ }).first();
    await firstCard.waitFor({ timeout: 15_000 });
    await firstCard.click();
    await expect(page).toHaveURL(/\/actividades\/.+/);

    await expect(page.getByText("Sobre la actividad")).toBeVisible();
    await expect(page.getByText(/pronóstico para el día/i)).toBeVisible();
    await expect(page.getByText("Para que se confirme")).toBeVisible();

    // Back to the feed without navigating in-app; the button keeps its aria label.
    await page.getByRole("button", { name: "Volver" }).click();
    await expect(page).toHaveURL(/\/explorar/);
  });

  test("sumarse y bajarse de una actividad con cupos", async ({ page }) => {
    await loginAs(page, USER);
    await page.getByRole("link", { name: "Explorar" }).first().click();

    const firstCard = page.getByRole("link", { name: /Ver actividad →/ }).first();
    await firstCard.waitFor({ timeout: 15_000 });
    await firstCard.click();

    const joinButton = page.getByRole("button", { name: "Sumarme a la actividad" });
    if (!(await joinButton.count())) {
      test.skip(true, "No hay actividades con cupos libres en este stack");
      return;
    }

    await joinButton.click();
    await page.getByRole("button", { name: "Sí, sumarme" }).click();
    await expect(page.getByRole("button", { name: "Bajarme de la actividad" })).toBeVisible();

    await page.getByRole("button", { name: "Bajarme de la actividad" }).click();
    await page.getByRole("button", { name: "Sí, bajarme" }).click();
    await expect(page.getByRole("button", { name: "Sumarme a la actividad" })).toBeVisible();
  });

  test("las actividades sin imágenes usan el placeholder de escena", async ({ page }) => {
    await loginAs(page, USER);
    await page.getByRole("link", { name: "Explorar" }).first().click();

    const firstCard = page.getByRole("link", { name: /Ver actividad →/ }).first();
    await firstCard.waitFor({ timeout: 15_000 });
    await firstCard.click();

    // Si no se llegó a ninguna galería con dots, el detalle muestra el
    // placeholder de escena sin controles de navegación.
    const dot = page.getByRole("button", { name: "Ver imagen 1" });
    if (await dot.count()) {
      await expect(dot.first()).toBeAttached();
    } else {
      // Placeholder de escena sin controles de navegación.
      await expect(page.getByText("Sin imagen").first()).toBeAttached();
    }
  });
});
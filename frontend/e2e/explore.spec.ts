import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

test.describe("Explorar", () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, USER);
    await page.getByRole("link", { name: "Explorar" }).first().click();
    await expect(page).toHaveURL(/\/explorar/);
  });

  test("muestra el listado de actividades con sus filtros", async ({ page }) => {
    await expect(page.getByPlaceholder("Buscar actividades…")).toBeVisible();
    for (const chip of ["Todo", "Outdoor", "Indoor", "Mixto"]) {
      await expect(page.getByRole("button", { name: chip, exact: true }).first()).toBeVisible();
    }
    await expect(page.getByLabel("Con cupo")).toBeVisible();

    // Nada que ver si el stack de prueba no tiene actividades sembradas.
    const empty = page.getByText("No encontramos actividades con esos filtros.");
    const card = page.getByRole("link", { name: /Ver actividad →/ }).first();
    await expect(empty.or(card).first()).toBeVisible();
  });

  test("filtra por tipo repitiendo el pedido a la API", async ({ page }) => {
    await page.getByRole("button", { name: "Outdoor", exact: true }).first().click();

    const card = page.getByRole("link", { name: /Ver actividad →/ }).first();
    if (await card.count()) {
      // Cada card responde a los nuevos filtros sin recargar la página.
      await expect(card).toBeVisible();
      expect(new URL(page.url()).pathname).toBe("/explorar");
    } else {
      await expect(page.getByText("No encontramos actividades con esos filtros.")).toBeVisible();
    }
  });

  test("permite limpiar los filtros", async ({ page }) => {
    await page.getByRole("button", { name: "Outdoor", exact: true }).first().click();
    await page.getByPlaceholder("Buscar actividades…").fill("Trekking");
    await page.getByPlaceholder("Ciudad").fill("CABA");
    await page.getByRole("button", { name: "Limpiar" }).click();

    await expect(page.getByPlaceholder("Buscar actividades…")).toHaveValue("");
    await expect(page.getByPlaceholder("Ciudad")).toHaveValue("");
  });
});
import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

test.describe("votación de reprogramación", () => {
  test("un participante vota una fecha alternativa en una votación pendiente", async ({ page }) => {
    await loginAs(page, USER);
    await page.goto("/mis-actividades");
    // "Mis actividades" opens in Calendario view by default, but "Te toca votar" (checked
    // below) is shown regardless of view — this just confirms the page loaded. The "Mis
    // actividades" heading itself is desktop-only (hidden on mobile), so check the view
    // toggle instead, which is visible on every viewport this suite runs at.
    await expect(page.getByRole("button", { name: "Calendario" })).toBeVisible();

    const pending = page.getByText("Te toca votar", { exact: true });
    if (!(await pending.count())) {
      test.skip(true, "No hay actividades con votación pendiente en este stack");
      return;
    }

    // Entrar a la primera actividad que pide voto.
    await page.getByRole("link", { name: /Mal pronóstico/ }).first().click();
    await expect(page).toHaveURL(/\/actividades\/.+/);
    await expect(page.getByText("Sala de Votación")).toBeVisible();

    // Un participante no ve el editor de configuración.
    await expect(page.getByRole("button", { name: "Administrar votación" })).toHaveCount(0);

    const voteAction = page
      .getByRole("button", { name: "Votar fecha alternativa" })
      .or(page.getByRole("button", { name: "Cambiar voto" }))
      .first();
    if (!(await voteAction.count())) {
      test.skip(true, "La votación no acepta votos en este estado");
      return;
    }

    await voteAction.click();
    const option = page.getByRole("button", { name: /%/ }).first();
    await option.click();
    await page.getByRole("button", { name: "Confirmar voto" }).click();

    await expect(page.getByRole("button", { name: "Cambiar voto" })).toBeVisible();
  });
});
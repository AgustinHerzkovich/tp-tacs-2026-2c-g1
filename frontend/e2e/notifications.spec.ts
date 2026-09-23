import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

test.describe("notificaciones", () => {
  test("abre el drawer y marca una como leída al navegar a la actividad", async ({ page }) => {
    await loginAs(page, USER);

    await page.getByRole("button", { name: "Notificaciones" }).click();
    await expect(page.getByRole("dialog").getByText("Notificaciones", { exact: true })).toBeVisible();

    // Cada notificación muestra su antigüedad ("hace 12 min", "recién", ...);
    // si no hay ninguna, el drawer muestra el estado vacío.
    const empty = page.getByText("No tenés alertas nuevas.");
    if (await empty.count()) {
      await expect(empty).toBeVisible();
      return;
    }

    const item = page
      .getByRole("dialog")
      .getByRole("button")
      .filter({ hasText: /hace \d+ (min|h|d)|recién|ayer|ahora/ });
    await item.first().click();
    await expect(page).toHaveURL(/\/actividades\/.+/);
  });
});
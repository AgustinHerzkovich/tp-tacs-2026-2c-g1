import { expect, test } from "@playwright/test";
import { loginAs, USER } from "./helpers";

const VIEWPORTS = [320, 375, 430, 768, 1024, 1440];

for (const width of VIEWPORTS) {
  test(`login no desborda a ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await page.goto("/login");
    await expect(page.locator("main")).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  });

  test(`feeds y chrome no desbordan a ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await loginAs(page, USER);
    for (const route of ["/explorar", "/mis-actividades"]) {
      await page.goto(route);
      await expect(page.locator("main, .fade-in").first()).toBeVisible();
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    }
  });
}

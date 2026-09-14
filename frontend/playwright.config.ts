import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests for Planazo.
 *
 * They exercise the real app against the real stack (Keycloak, backend,
 * Mongo, MinIO) brought up with `docker compose up --build` at the repo root,
 * plus this frontend. The tests log in through the Keycloak login page, so
 * the realm must have accounts matching E2E_USER_* / E2E_ADMIN_* credentials
 * below.
 */
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL,
    trace: "on-first-retry",
    screenshot: "only-on-failure",
    // The app is mobile-first; desktop Chromium additionally exercises the
    // lg: variants of the layout.
    viewport: { width: 390, height: 844 },
  },
  projects: [
    { name: "mobile-chromium", use: { ...devices["Pixel 7"] } },
    { name: "desktop-chromium", use: { ...devices["Desktop Chrome"] } },
  ],
  webServer: {
    command: "npm run dev",
    // Reuse a server already started by the developer instead of spawning a
    // second one; in CI a fresh `npm run dev` is started for the job.
    reuseExistingServer: !process.env.CI,
    url: baseURL,
    timeout: 120_000,
  },
});
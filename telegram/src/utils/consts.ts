export const TELEGRAM_API_URL = `https://api.telegram.org/bot${process.env.TELEGRAM_BOT_TOKEN}`;

export const backendClient = (process.env.BACKEND_URL ?? "http://localhost:8080").replace(/\/+$/, "");

export const keycloakClient = (process.env.KEYCLOAK_URL ?? "http://localhost:8090").replace(/\/+$/, "");

export const keycloakRealm = process.env.KEYCLOAK_REALM ?? "solnotfound";

export const telegramBotClientId = process.env.KEYCLOAK_TELEGRAM_BOT_CLIENT_ID ?? "solnotfoundTelegramBot";

export const telegramBotClientSecret = process.env.KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET ?? "";

export const telegramApiToken = process.env.TELEGRAM_API_TOKEN ?? "";

export const frontendClient = (process.env.FRONTEND_URL ?? "http://localhost:3000").replace(/\/+$/, "");

/** Secret registered with setWebhook (secret_token); Telegram echoes it on every update. */
export const telegramWebhookSecret = process.env.TELEGRAM_WEBHOOK_SECRET ?? "";

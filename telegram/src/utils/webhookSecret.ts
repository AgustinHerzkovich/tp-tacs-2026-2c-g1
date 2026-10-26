import { timingSafeEqual } from "node:crypto";

/** Header where Telegram sends the secret_token registered with setWebhook. */
export const WEBHOOK_SECRET_HEADER = "x-telegram-bot-api-secret-token";

/**
 * Checks that an update really comes from Telegram: the webhook URL is public, so without this
 * anyone who finds it could forge updates for any chat. Fails closed when no secret is configured.
 * The comparison runs in constant time so the secret cannot be guessed from response timings.
 */
export function isValidWebhookSecret(received: unknown, expected: string): boolean {
  if (!expected || typeof received !== "string") return false;
  const receivedBytes = Buffer.from(received);
  const expectedBytes = Buffer.from(expected);
  return receivedBytes.length === expectedBytes.length && timingSafeEqual(receivedBytes, expectedBytes);
}

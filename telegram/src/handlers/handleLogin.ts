import sendMessage, { escapeHtml } from "../utils/sendMessage";
import { frontendClient } from "../utils/consts";

/** Link al login del frontend; t=true indica que viene de Telegram y cid es el chat a vincular. */
export function loginUrl(chatId: number): string {
  return `${frontendClient}/login?t=true&cid=${chatId}`;
}

/** Telegram descarta en silencio los hipervínculos y botones a hosts locales. */
function isLocalUrl(url: string): boolean {
  const { hostname } = new URL(url);
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "0.0.0.0";
}

export default async function handleLogin(chatId: number): Promise<void> {
  const url = loginUrl(chatId);
  console.log(`[handleLogin] chat ${chatId} sin usuario vinculado, se envía link de login`);

  const link = `<a href="${escapeHtml(url)}">Inicia sesión aca</a>`;
  const visibleUrl = isLocalUrl(url) ? `\n${escapeHtml(url)}` : "";
  const replyMarkup = url.startsWith("https://")
    ? { inline_keyboard: [[{ text: "Iniciar sesión", url }]] }
    : { remove_keyboard: true };

  await sendMessage(
    chatId,
    `No encontramos una cuenta vinculada a este chat. ${link} para identificarte.${visibleUrl}\n\nDespués volvé y mandá /start.`,
    replyMarkup,
    { parseMode: "HTML" }
  );
}

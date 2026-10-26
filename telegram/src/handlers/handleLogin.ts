import sendMessage, { escapeHtml } from "../utils/sendMessage";
import { backendFetch } from "../utils/auth";
import { frontendClient } from "../utils/consts";

/** Link al login del frontend; t=true indica que viene de Telegram y link es el código de un solo
 * uso que emitió el backend para este chat. El chat nunca viaja en el link: si viajara, cualquiera
 * podría armar un link con su propio chat y mandárselo a otra persona para quedarse con su cuenta. */
export function loginUrl(code: string): string {
  return `${frontendClient}/login?t=true&link=${encodeURIComponent(code)}`;
}

/** Pide al backend un código de vinculación para el chat (vence a los pocos minutos y sirve una
 * sola vez). */
async function requestLinkCode(chatId: number): Promise<string> {
  const response = await backendFetch(`/users/telegram/${chatId}/link-code`, { method: "POST" });
  if (!response.ok) throw new Error(`Error HTTP ${response.status} pidiendo el código de vinculación del chat ${chatId}`);
  const { code } = (await response.json()) as { code?: unknown };
  if (typeof code !== "string" || !code) throw new Error(`El backend respondió sin código para el chat ${chatId}`);
  return code;
}

/** Telegram descarta en silencio los hipervínculos y botones a hosts locales. */
function isLocalUrl(url: string): boolean {
  const { hostname } = new URL(url);
  return hostname === "localhost" || hostname === "127.0.0.1" || hostname === "0.0.0.0";
}

export default async function handleLogin(chatId: number): Promise<void> {
  const url = loginUrl(await requestLinkCode(chatId));
  console.log(`[handleLogin] chat ${chatId} sin usuario vinculado, se envía link de login`);

  const link = `<a href="${escapeHtml(url)}">Inicia sesión aca</a>`;
  const visibleUrl = isLocalUrl(url) ? `\n${escapeHtml(url)}` : "";
  const replyMarkup = url.startsWith("https://")
    ? { inline_keyboard: [[{ text: "Iniciar sesión", url }]] }
    : { remove_keyboard: true };

  await sendMessage(
    chatId,
    `No encontramos una cuenta vinculada a este chat. ${link} para identificarte.${visibleUrl}\n\n` +
      "El link sirve una sola vez y vence en 10 minutos. No se lo compartas a nadie. " +
      "Después volvé y mandá /start.",
    replyMarkup,
    { parseMode: "HTML" }
  );
}

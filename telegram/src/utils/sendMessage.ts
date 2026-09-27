import { TELEGRAM_API_URL } from "./consts";

export interface SendMessageOptions {
  /** "HTML" permite usar <a href="...">, <b>, etc. El texto tiene que venir escapado (ver escapeHtml). */
  parseMode?: "HTML";
}

/** Escapa los caracteres que Telegram interpreta en parse_mode HTML. */
export function escapeHtml(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

export default async function sendMessage(
  chatId: number,
  text: string,
  replyMarkup?: unknown,
  options: SendMessageOptions = {}
): Promise<void> {
  const response = await fetch(`${TELEGRAM_API_URL}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text, reply_markup: replyMarkup, parse_mode: options.parseMode }),
  });

  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`[sendMessage] fallo enviando mensaje a chat ${chatId} (HTTP ${response.status}): ${errorBody}`);
  } else {
    console.log(`[sendMessage] mensaje enviado a chat ${chatId}`);
  }
}

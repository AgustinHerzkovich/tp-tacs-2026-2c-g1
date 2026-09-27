import { TELEGRAM_API_URL } from "./consts";

export interface SendMessageOptions {
  /** "HTML" permite usar <a href="...">, <b>, etc. El texto tiene que venir escapado (ver escapeHtml). */
  parseMode?: "HTML";
}

/** Límite duro de Telegram para el texto de un mensaje. */
const MAX_MESSAGE_LENGTH = 4096;

/** Margen para no chocar con el límite al agregar el encabezado de cada parte. */
const CHUNK_LENGTH = MAX_MESSAGE_LENGTH - 200;

/** Escapa los caracteres que Telegram interpreta en parse_mode HTML. */
export function escapeHtml(value: string): string {
  return value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

/**
 * Parte un texto largo en trozos de a lo sumo `limit` caracteres, cortando en los saltos de línea
 * para no partir una línea por la mitad. Si una sola línea ya excede el límite, se corta duro.
 */
export function chunkText(text: string, limit: number = CHUNK_LENGTH): string[] {
  const chunks: string[] = [];
  let current = "";

  for (const line of text.split("\n")) {
    const candidate = current.length === 0 ? line : `${current}\n${line}`;

    if (candidate.length <= limit) {
      current = candidate;
      continue;
    }

    if (current.length > 0) {
      chunks.push(current);
      current = "";
    }

    let rest = line;
    while (rest.length > limit) {
      chunks.push(rest.slice(0, limit));
      rest = rest.slice(limit);
    }
    current = rest;
  }

  if (current.length > 0) {
    chunks.push(current);
  }

  return chunks;
}

/** Envía un texto que puede no entrar en un solo mensaje, partiéndolo por saltos de línea. */
export async function sendLongMessage(
  chatId: number,
  text: string,
  options: SendMessageOptions = {}
): Promise<void> {
  const chunks = chunkText(text);
  for (const chunk of chunks) {
    await sendMessage(chatId, chunk, undefined, options);
  }
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

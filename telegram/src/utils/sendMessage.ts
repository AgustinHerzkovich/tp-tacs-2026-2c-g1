import { TELEGRAM_API_URL } from "./consts";


export default async function sendMessage(chatId: number, text: string, replyMarkup?: unknown): Promise<void> {
  const response = await fetch(`${TELEGRAM_API_URL}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text, reply_markup: replyMarkup }),
  });

  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`[sendMessage] fallo enviando mensaje a chat ${chatId} (HTTP ${response.status}): ${errorBody}`);
  } else {
    console.log(`[sendMessage] mensaje enviado a chat ${chatId}`);
  }
}

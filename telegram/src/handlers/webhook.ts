import type { HttpFunction } from "@google-cloud/functions-framework";

const TELEGRAM_API_URL = `https://api.telegram.org/bot${process.env.TELEGRAM_BOT_TOKEN}`;

interface TelegramMessage {
  chat: { id: number };
  text?: string;
}

interface TelegramUpdate {
  message?: TelegramMessage;
}

async function sendMessage(chatId: number, text: string, replyMarkup?: unknown): Promise<void> {
  const response = await fetch(`${TELEGRAM_API_URL}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text, reply_markup: replyMarkup }),
  });

  if (!response.ok) {
    console.error("sendMessage failed:", await response.text());
  }
}

async function handleStart(chatId: number): Promise<void> {
  await sendMessage(chatId, "Hola, ¿cómo estás? No tenemos información tuya, logueate para continuar.", {
    keyboard: [[{ text: "/perrea" }]],
    resize_keyboard: true,
    one_time_keyboard: true,
  });
}

async function handleLogin(chatId: number): Promise<void> {
  await sendMessage(chatId, "Iniciando sesión... (en construcción)");
}

const COMMANDS: Record<string, (chatId: number) => Promise<void>> = {
  "/start": handleStart,
  "/login": handleLogin,
  "/perrea": async (chatId: number) => {
    await sendMessage(chatId, "¡Perreando! (en construcción)");
  },
};

export const webhook: HttpFunction = async (req, res) => {
  const update = req.body as TelegramUpdate;
  const message = update.message;

  if (message?.text) {
    const command = message.text.trim();
    const handleCommand = COMMANDS[command];
    if (handleCommand) {
      await handleCommand(message.chat.id);
    }
  }

  res.status(200).json({ ok: true });
};

import type { HttpFunction } from "@google-cloud/functions-framework";
import { COMMANDS } from "../commands";
import { COMMAND_PARENT } from "../utils/command-tree";
import { getActiveMenu, getUserId, setActiveMenu } from "../utils/session";
import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";
import { telegramWebhookSecret } from "../utils/consts";
import { isValidWebhookSecret, WEBHOOK_SECRET_HEADER } from "../utils/webhookSecret";


interface TelegramMessage {
  chat: { id: number };
  text?: string;
}

interface TelegramUpdate {
  update_id?: number;
  message?: TelegramMessage;
}


const COMMANDS_WITHOUT_USER = new Set(["/start", "/login"]);

async function runCommand(chatId: number, command: string, handleCommand: CommandHandler): Promise<void> {
  const userId = getUserId(chatId);

  if (!COMMANDS_WITHOUT_USER.has(command) && userId === undefined) {
    await sendMessage(chatId, "Primero ejecutá /start para identificarte y obtener tu userId.");
    return;
  }

  const parent = COMMAND_PARENT[command];
  if (parent && getActiveMenu(chatId) !== parent) {
    await sendMessage(chatId, `Para ejecutar "${command}" primero tenés que ejecutar "${parent}".`);
    return;
  }

  console.log(`[webhook] ejecutando comando "${command}" en chat ${chatId}`);
  // Los comandos fuera de COMMANDS_WITHOUT_USER ya llegaron acá con userId definido.
  await handleCommand(chatId, userId ?? "");
  console.log(`[webhook] comando "${command}" finalizado`);

  setActiveMenu(chatId, parent ?? command);
  console.log(`[webhook] activeMenu de chat ${chatId} -> "${getActiveMenu(chatId)}"`);
}


export const webhook: HttpFunction = async (req: any, res: any) => {
  // Telegram always POSTs updates; a GET is the container healthcheck and processes nothing.
  if (req.method === "GET") {
    res.status(200).json({ ok: true });
    return;
  }

  if (!isValidWebhookSecret(req.headers?.[WEBHOOK_SECRET_HEADER], telegramWebhookSecret)) {
    console.warn("[webhook] update rechazado: falta el secret de Telegram o no coincide");
    res.status(401).json({ ok: false });
    return;
  }

  const update = req.body as TelegramUpdate | undefined;
  const message = update?.message;

  console.log(
    `[webhook] update recibido: update_id=${update?.update_id} chat_id=${message?.chat?.id} text=${message?.text ?? "(sin texto)"}`
  );

  if (message?.text) {
    const command = message.text.trim();
    const handleCommand = COMMANDS[command];
    if (handleCommand) {
      try {
        await runCommand(message.chat.id, command, handleCommand);
      } catch (err) {
        console.error(`[webhook] error ejecutando "${command}":`, err);
        await sendMessage(
          message.chat.id,
          "No pudimos obtener lo que pediste en este momento. Probá de nuevo en un rato."
        );
      }
    } else {
      console.log(`[webhook] comando desconocido: "${command}"`);
    }
  } else {
    console.log("[webhook] update sin message.text, ignorado");
  }
  
  res.status(200).json({ ok: true });
};
import type { HttpFunction } from "@google-cloud/functions-framework";
import { COMMANDS } from "../commands";


interface TelegramMessage {
  chat: { id: number };
  text?: string;
}

interface TelegramUpdate {
  update_id?: number;
  message?: TelegramMessage;
}




export const webhook: HttpFunction = async (req: any, res: any) => {
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
        console.log(`[webhook] ejecutando comando "${command}" en chat ${message.chat.id}`);
        await handleCommand(message.chat.id);
        console.log(`[webhook] comando "${command}" finalizado`);
      } catch (err) {
        console.error(`[webhook] error ejecutando "${command}":`, err);
      }
    } else {
      console.log(`[webhook] comando desconocido: "${command}"`);
    }
  } else {
    console.log("[webhook] update sin message.text, ignorado");
  }
  
  res.status(200).json({ ok: true });
};

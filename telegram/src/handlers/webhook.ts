import type { HttpFunction } from "@google-cloud/functions-framework";
import { COMMANDS } from "../commands";


interface TelegramMessage {
  chat: { id: number };
  text?: string;
}

interface TelegramUpdate {
  message?: TelegramMessage;
}




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

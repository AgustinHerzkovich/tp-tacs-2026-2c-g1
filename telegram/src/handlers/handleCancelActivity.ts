import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleCancelActivity: CommandHandler = async (chatId, userId) => {
  console.log(`[cancelActivity] chat=${chatId} userId=${userId}`);
  // TODO: request al backend: DELETE/cancelar ${backendClient}/activities/{id} usando userId=${userId}
  await sendMessage(chatId, `Vas a cancelar tu actividad. (userId=${userId})`);
};

export default handleCancelActivity;
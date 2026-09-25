import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleCancelActivity: CommandHandler = async (chatId, userId) => {
  console.log(`[cancelActivity] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): DELETE/cancelar /activities/{id} usando userId=${userId}
  await sendMessage(chatId, `Vas a cancelar tu actividad. (userId=${userId})`);
};

export default handleCancelActivity;
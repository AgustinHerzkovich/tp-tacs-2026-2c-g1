import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleCheckActivityClimate: CommandHandler = async (chatId, userId) => {
  console.log(`[checkActivityClimate] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): GET /activities/{id}/weather usando userId=${userId}
  await sendMessage(chatId, `Vas a chequear el clima de tu actividad. (userId=${userId})`);
};

export default handleCheckActivityClimate;
import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleConfigureClimate: CommandHandler = async (chatId, userId) => {
  console.log(`[configureClimate] chat=${chatId} userId=${userId}`);
  // TODO: request al backend: configurar clima de la actividad (body en CreateActivityRequest) usando userId=${userId}
  await sendMessage(chatId, `Vas a configurar el clima de tu actividad. (userId=${userId})`);
};

export default handleConfigureClimate;
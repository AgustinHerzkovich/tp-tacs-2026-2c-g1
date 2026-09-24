import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleConfigureReprogramation: CommandHandler = async (chatId, userId) => {
  console.log(`[configureReprogramation] chat=${chatId} userId=${userId}`);
  // TODO: request al backend: configurar reprogramación de la actividad (body en CreateActivityRequest) usando userId=${userId}
  await sendMessage(chatId, `Vas a configurar la reprogramación de tu actividad. (userId=${userId})`);
};

export default handleConfigureReprogramation;
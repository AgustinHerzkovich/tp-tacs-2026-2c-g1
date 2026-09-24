import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleConfigureAnticipation: CommandHandler = async (chatId, userId) => {
  console.log(`[configureAnticipation] chat=${chatId} userId=${userId}`);
  // TODO: request al backend: configurar anticipación de la actividad (body en CreateActivityRequest) usando userId=${userId}
  await sendMessage(chatId, `Vas a configurar la anticipación de tu actividad. (userId=${userId})`);
};

export default handleConfigureAnticipation;
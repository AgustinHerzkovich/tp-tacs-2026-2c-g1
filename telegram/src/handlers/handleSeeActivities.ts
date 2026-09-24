import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleSeeActivities: CommandHandler = async (chatId, userId) => {
  console.log(`[seeActivities] chat=${chatId} userId=${userId}`);
  // TODO: request al backend: GET ${backendClient}/activities (filtradas por disponibilidad) usando userId=${userId}
  await sendMessage(chatId, `Vas a ver las actividades disponibles. (userId=${userId})`);
};

export default handleSeeActivities;
import sendMessage from "../utils/sendMessage";
import { COMMAND_TREE } from "../utils/command-tree";
import type { CommandHandler } from "../utils/command.types";

const handleMyActivities: CommandHandler = async (chatId, userId) => {
  console.log(`[myActivities] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): GET /activities/participants/me y GET /activities/organizers/me usando userId=${userId}
  const subcommands = COMMAND_TREE["/myActivities"].map((cmd) => `- ${cmd}`).join("\n");
  await sendMessage(
    chatId,
    `Tus actividades. (userId=${userId})\n\nSubcomandos disponibles:\n${subcommands}`
  );
};

export default handleMyActivities;
import sendMessage from "../utils/sendMessage";
import { COMMAND_TREE } from "../utils/command-tree";
import type { CommandHandler } from "../utils/command.types";

const handleCreateActivity: CommandHandler = async (chatId, userId) => {
  console.log(`[createActivity] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): POST /activities (crear actividad) usando userId=${userId}
  const subcommands = COMMAND_TREE["/createActivity"].map((cmd) => `- ${cmd}`).join("\n");
  await sendMessage(
    chatId,
    `Vas a crear una actividad. \n\nSubcomandos disponibles:\n${subcommands}`
  );
};

export default handleCreateActivity;
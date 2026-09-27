import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleCheckVotingResults: CommandHandler = async (chatId, userId) => {
  console.log(`[checkVotingResults] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): GET /votations usando userId=${userId}
  await sendMessage(chatId, `Vas a ver los resultados de la votación. (userId=${userId})`);
};

export default handleCheckVotingResults;
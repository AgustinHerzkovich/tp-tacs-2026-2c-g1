import sendMessage from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

const handleVote: CommandHandler = async (chatId, userId) => {
  console.log(`[vote] chat=${chatId} userId=${userId}`);
  // TODO: request al backend con backendFetch (JWT + API token): PUT /votations/{votationId}/votes/me usando userId=${userId}
  await sendMessage(chatId, `Vas a votar en la votación de tu actividad. (userId=${userId})`);
};

export default handleVote;
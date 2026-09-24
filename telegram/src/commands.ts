import sendMessage from "./utils/sendMessage";
import handleStart from "./handlers/handleStart";
import handleLogin from "./handlers/handleLogin";

export  const COMMANDS: Record<string, (chatId: number) => Promise<void>> = {
  "/start": handleStart,
  "/login": handleLogin,
  "/perrea": async (chatId: number) => {
    await sendMessage(chatId, "¡Perreando! (en construcción)");
  },
};

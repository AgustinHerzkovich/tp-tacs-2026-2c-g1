import sendMessage from "../utils/sendMessage";

export default async function handleLogin(chatId: number): Promise<void> {
  console.log(`[handleLogin] enviando link de login a chat ${chatId}`);
  await sendMessage(chatId, "Loggeate Aca!");
}

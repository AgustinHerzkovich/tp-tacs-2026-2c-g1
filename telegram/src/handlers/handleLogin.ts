import sendMessage from "../utils/sendMessage";

export default async function handleLogin(chatId: number): Promise<void> {
    
  await sendMessage(chatId, "Loggeate Aca!");
}

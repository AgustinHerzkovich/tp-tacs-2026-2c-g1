import sendMessage from "../utils/sendMessage";
import { backendClient } from "../utils/consts";
import { User } from "../utils/user.type";


export default async function handleStart(chatId: number): Promise<void> {
  console.log(`[handleStart] enviando saludo a chat ${chatId}`);
  await sendMessage(chatId, "hola");
  /*const user = await getUser(chatId);
  if (user) {
    await sendMessage(chatId, `¡Hola ${user.name ?? ""}! Ya estás identificado.`);
  } else {
    await sendMessage(chatId, "Hola, ¿cómo estás? No tenemos información tuya. Usá /login para identificarte.");
  }*/
}

/*async function getUser(chatId: number): Promise<User | null> {
  const response = await fetch(`${backendClient}/users/telegram/${chatId}`);
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);
  return (await response.json()) as User;
}*/

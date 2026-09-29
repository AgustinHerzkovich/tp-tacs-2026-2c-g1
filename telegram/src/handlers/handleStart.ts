import sendMessage from "../utils/sendMessage";
import { backendFetch } from "../utils/auth";
import { MAIN_MENU } from "../utils/command-tree";
import { setUserId } from "../utils/session";
import type { User } from "../utils/user.type";
import handleLogin from "./handleLogin";


export default async function handleStart(chatId: number): Promise<void> {
  let user: User | null;
  try {
    user = await getUser(chatId);
  } catch (error) {
    console.error(`[handleStart] no se pudo buscar el usuario del chat ${chatId}:`, error);
    await sendMessage(chatId, "No pudimos verificar tu cuenta en este momento. Probá de nuevo en un rato.");
    return;
  }

  if (!user) {
    setUserId(chatId, undefined);
    await handleLogin(chatId);
    return;
  }

  setUserId(chatId, user.id);
  await sendMessage(chatId, `¡Hola${user.name ? ` ${user.name}` : ""}! ¿Qué querés hacer?`, {
    keyboard: MAIN_MENU.map((command) => [{ text: command }]),
    resize_keyboard: true,
  });
}


async function getUser(chatId: number): Promise<User | null> {
  const response = await backendFetch(`/users/telegram/${chatId}`);
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`Error HTTP ${response.status} buscando el usuario del chat ${chatId}`);
  const user = (await response.json()) as Partial<User>;
  if (typeof user.id !== "string" || !user.id) {
    throw new Error(`El backend respondió sin id de usuario para el chat ${chatId}`);
  }
  return { id: user.id, name: user.name ?? null };
}

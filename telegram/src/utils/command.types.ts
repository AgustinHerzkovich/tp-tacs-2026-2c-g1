/**
 * Handler de un comando. `userId` llega siempre definido para los comandos que no son
 * `/start` ni `/login`: `webhook.ts` corta antes si el chat todavía no se identificó, así que
 * un handler declarado con menos parámetros (como `handleStart`) también cumple este tipo.
 */
export type CommandHandler = (chatId: number, userId: string) => Promise<void>;

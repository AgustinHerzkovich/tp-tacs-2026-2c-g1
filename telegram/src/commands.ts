import handleStart from "./handlers/handleStart";
import handleLogin from "./handlers/handleLogin";
import handleSeeActivities from "./handlers/handleSeeActivities";
import handleMyActivities from "./handlers/handleMyActivities";
import type { CommandHandler } from "./utils/command.types";

export const COMMANDS: Record<string, CommandHandler> = {
  "/start": handleStart,
  "/login": handleLogin,
  "/seeActivities": handleSeeActivities,
  "/myActivities": handleMyActivities,
};

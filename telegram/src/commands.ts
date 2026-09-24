import handleStart from "./handlers/handleStart";
import handleLogin from "./handlers/handleLogin";
import handleSeeActivities from "./handlers/handleSeeActivities";
import handleMyActivities from "./handlers/handleMyActivities";
import handleCheckActivityClimate from "./handlers/handleCheckActivityClimate";
import handleCancelActivity from "./handlers/handleCancelActivity";
import handleVote from "./handlers/handleVote";
import handleCheckVotingResults from "./handlers/handleCheckVotingResults";
import handleCreateActivity from "./handlers/handleCreateActivity";
import handleConfigureClimate from "./handlers/handleConfigureClimate";
import handleConfigureAnticipation from "./handlers/handleConfigureAnticipation";
import handleConfigureReprogramation from "./handlers/handleConfigureReprogramation";
import type { CommandHandler } from "./utils/command.types";

export const COMMANDS: Record<string, CommandHandler> = {
  "/start": handleStart,
  "/login": handleLogin,
  "/seeActivities": handleSeeActivities,
  "/myActivities": handleMyActivities,
  "/checkActivityClimate": handleCheckActivityClimate,
  "/cancelActivity": handleCancelActivity,
  "/vote": handleVote,
  "/checkVotingResults": handleCheckVotingResults,
  "/createActivity": handleCreateActivity,
  "/configureClimate": handleConfigureClimate,
  "/configureAnticipation": handleConfigureAnticipation,
  "/configureReprogramation": handleConfigureReprogramation,
};
import { fetchAllActivities, isParticipant } from "../utils/activities";
import { renderActivityList } from "../utils/activityFormat";
import { sendLongMessage } from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

/** Lista las actividades en las que el usuario es participante, de la más próxima a la más lejana. */
const handleSeeActivities: CommandHandler = async (chatId, userId) => {
  console.log(`[seeActivities] chat=${chatId} userId=${userId}`);
  const activities = (await fetchAllActivities()).filter((activity) => isParticipant(activity, userId));

  await sendLongMessage(
    chatId,
    renderActivityList(activities, {
      header: "🥾 Tus actividades en las que participás",
      emptyMessage: "Todavía no participás de ninguna actividad. Sumate a alguna desde la app.",
    }),
    { parseMode: "HTML" }
  );
};

export default handleSeeActivities;

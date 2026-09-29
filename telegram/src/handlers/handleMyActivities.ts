import { fetchAllActivities, isOrganizer } from "../utils/activities";
import { renderActivityList, VOTATION_NOTE } from "../utils/activityFormat";
import { sendLongMessage } from "../utils/sendMessage";
import type { CommandHandler } from "../utils/command.types";

/**
 * Lista las actividades que organiza el usuario, de la más próxima a la más lejana, avisando
 * cuáles están en votación de reprogramación (ActivityStatus.PROPOSED).
 */
const handleMyActivities: CommandHandler = async (chatId, userId) => {
  console.log(`[myActivities] chat=${chatId} userId=${userId}`);
  const activities = (await fetchAllActivities()).filter((activity) => isOrganizer(activity, userId));

  await sendLongMessage(
    chatId,
    renderActivityList(activities, {
      header: "🗂️ Tus actividades",
      emptyMessage: "Todavía no organizás ninguna actividad.",
      noteFor: (activity) => (activity.status === "PROPOSED" ? VOTATION_NOTE : undefined),
    }),
    { parseMode: "HTML" }
  );
};

export default handleMyActivities;

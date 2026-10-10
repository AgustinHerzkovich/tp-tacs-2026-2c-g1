import type { CurrentUser } from "@/types/domain";

/** Shown for a participant the backend has no name for yet. The backend
 * stores each user's name the first time they use the app after signing in,
 * so this only appears for an account that has not come back since names
 * started being stored. */
export const PARTICIPANT_FALLBACK_NAME = "Participante";

/** Name to show for a participant whose `name` came back empty: the current
 * user's own name (known from their token) or the generic fallback. */
export function participantDisplayName(userId: string, currentUser: CurrentUser | null): string {
  if (currentUser && userId === currentUser.id) return currentUser.name;
  return PARTICIPANT_FALLBACK_NAME;
}

/** Derives display initials from a name (e.g. "Vale Ríos" -> "VR"). The
 * backend never stores initials — it only has `id`/`name` — so this is
 * computed client-side wherever a name needs an avatar. */
export function getInitials(name: string): string {
  return name
    .split(" ")
    .map((w) => w[0])
    .filter((c): c is string => Boolean(c))
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

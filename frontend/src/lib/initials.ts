import type { CurrentUser } from "@/types/domain";

/** The backend only ever exposes a participant as `{ userId }` (a Keycloak
 * subject id, e.g. a UUID) — there is no `/users/:id` endpoint and no name
 * embedded in ActivityResponse.participants, so there is no way to resolve a
 * real display name for anyone other than the current, logged-in user
 * (whose name we already have from their own Keycloak token, via
 * useAuth/session). Showing a fabricated name for other participants would
 * reintroduce fake data through the back door — this returns an honest,
 * generic placeholder instead. */
export function participantDisplayName(userId: string, currentUser: CurrentUser | null): string {
  if (currentUser && userId === currentUser.id) return currentUser.name;
  return "Invitade";
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

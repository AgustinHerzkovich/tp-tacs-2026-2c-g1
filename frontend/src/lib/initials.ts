/** Derives display initials from a name (e.g. "Vale Ríos" -> "VR"). The
 * backend never stores initials — it only has `id`/`name` — so this is
 * computed client-side wherever a name needs an avatar. */
/** The frontend has no way to resolve a display name for a raw user id (no
 * `/users/:id` endpoint exists) — this turns a hyphenated id like
 * "vale-rios" into "Vale Rios" so avatar initials still read sensibly. Real
 * mock/seeded users happen to use exactly this `first-last` id shape (see
 * backend's ActivitySeeder and the frontend's mockUsers.ts). */
export function userIdToDisplayName(userId: string): string {
  return userId
    .split("-")
    .map((part) => (part ? part[0]?.toUpperCase() + part.slice(1) : part))
    .join(" ");
}

export function getInitials(name: string): string {
  return name
    .split(" ")
    .map((w) => w[0])
    .filter((c): c is string => Boolean(c))
    .slice(0, 2)
    .join("")
    .toUpperCase();
}

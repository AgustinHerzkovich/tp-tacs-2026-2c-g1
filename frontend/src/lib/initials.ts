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

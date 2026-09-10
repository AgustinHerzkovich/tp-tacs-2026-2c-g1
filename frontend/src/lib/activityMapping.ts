// Maps the backend's real ActivityResponse (see src/types/backend.ts) into
// the UI's playful mock-shaped model (src/types/domain.ts). Card components
// (ExploreCard/MisCard) only need the lossy mapped shape; the detail page
// works with the raw ActivityResponse directly instead (see useActivity) —
// it needs richer fields (participants, weather conditions, image URLs) that
// don't survive this mapping.

import type { ActivityResponse, ActivityStatus, ActivityType } from "@/types/backend";
import type { ExploreActivity, MisActivity, MockActivityType, MockStatusKey, SceneKey } from "@/types/domain";
import { formatActivityWhen } from "@/lib/formatDate";

const SCENE_KEYS: SceneKey[] = ["trekking", "voley", "cine", "juegos", "asado", "cumple", "picnic"];

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

/** The seeded backend activities (asado/trekking/cumple/picnic/voley/juegos)
 * use the same ids as SceneKey on purpose (see backend's ActivitySeeder), so
 * they get their real illustration. Anything else (activities created
 * through the wizard get a backend-generated id) gets a deterministic —
 * but arbitrary — pick, so the same activity always renders the same scene. */
export function pickScene(activityId: string): SceneKey {
  const known = SCENE_KEYS.find((key) => key === activityId);
  if (known) return known;
  return SCENE_KEYS[hashString(activityId) % SCENE_KEYS.length] ?? "trekking";
}

export function mapActivityType(type: ActivityType): MockActivityType {
  switch (type) {
    case "OUTDOOR":
      return "outdoor";
    case "INDOOR":
      return "indoor";
    case "MIXED":
      return "mixed";
    default:
      return "outdoor";
  }
}

/** Inverse of mapActivityType — used when submitting the creation wizard's
 * form (lowercase UI value) back to the backend (uppercase enum). */
export function toBackendActivityType(type: MockActivityType): ActivityType {
  switch (type) {
    case "outdoor":
      return "OUTDOOR";
    case "indoor":
      return "INDOOR";
    case "mixed":
      return "MIXED";
    default:
      return "OUTDOOR";
  }
}

export function mapActivityStatus(status: ActivityStatus): MockStatusKey {
  switch (status) {
    case "CONFIRMED":
      return "confirmada";
    case "PROPOSED":
      return "propuesta";
    case "RESCHEDULED":
      return "reprogramada";
    case "CANCELLED":
      return "cancelada";
    case "FINISHED":
      return "finalizada";
    default:
      return "confirmada";
  }
}

function toBase(dto: ActivityResponse) {
  return {
    id: dto.id,
    scene: pickScene(dto.id),
    title: dto.title,
    type: mapActivityType(dto.type),
    when: formatActivityWhen(dto.dateTime),
    where: dto.location.city ?? "Ubicación a confirmar",
    participantIds: dto.participants.map((p) => p.userId),
  };
}

export function toExploreActivity(dto: ActivityResponse): ExploreActivity {
  return { ...toBase(dto), people: dto.participantCount };
}

export function toMisActivity(dto: ActivityResponse): MisActivity {
  return {
    ...toBase(dto),
    status: mapActivityStatus(dto.status),
    joined: dto.participantCount,
    cap: dto.maxParticipants,
  };
}

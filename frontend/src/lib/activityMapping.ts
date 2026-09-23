// Maps the backend's real ActivityResponse (see src/types/backend.ts) into
// the UI's playful mock-shaped model (src/types/domain.ts). Card components
// (ExploreCard/MisCard) only need the lossy mapped shape; the activity detail
// page works with the raw ActivityResponse directly instead (see
// useActivity) since it needs fields that don't survive this mapping.

import type { ActivityResponse, ActivityStatus, ActivityType } from "@/types/backend";
import type { ExploreActivity, MisActivity, MockActivityType, MockStatusKey, PatternKey, SceneKey } from "@/types/domain";
import { formatActivityWhen } from "@/lib/formatDate";

const SCENE_KEYS: SceneKey[] = ["skyMint", "sunRose", "lavSky", "roseViolet", "mintSun", "violetRose", "roseSky"];
const PATTERN_KEYS: PatternKey[] = ["plain", "dots", "diagonal", "grid"];

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) | 0;
  }
  return Math.abs(hash);
}

/** Picks a deterministic — but otherwise arbitrary — illustration for an
 * activity id, so the same activity always renders the same scene. */
export function pickScene(activityId: string): SceneKey {
  return SCENE_KEYS[hashString(activityId) % SCENE_KEYS.length] ?? "skyMint";
}

/** Picks a deterministic overlay pattern for an imageless activity's
 * gradient card. Divides the hash down first so this doesn't just track
 * `pickScene`'s `% SCENE_KEYS.length` remainder — two activities sharing a
 * scene/gradient can still land on different patterns. */
export function pickPattern(activityId: string): PatternKey {
  const shifted = Math.floor(hashString(activityId) / SCENE_KEYS.length);
  return PATTERN_KEYS[shifted % PATTERN_KEYS.length] ?? "plain";
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
    pattern: pickPattern(dto.id),
    imageUrl: dto.imageUrls[0] ?? null,
    title: dto.title,
    type: mapActivityType(dto.type),
    dateTime: dto.dateTime,
    when: formatActivityWhen(dto.dateTime),
    where: dto.location.city ?? "Ubicación a confirmar",
    participantIds: dto.participants.map((p) => p.userId),
    participantNames: dto.participants.map((p) => p.name ?? p.userId),
  };
}

export function toExploreActivity(dto: ActivityResponse): ExploreActivity {
  return { ...toBase(dto), people: dto.participantCount, status: mapActivityStatus(dto.status) };
}

export function toMisActivity(dto: ActivityResponse): MisActivity {
  return {
    ...toBase(dto),
    status: mapActivityStatus(dto.status),
    joined: dto.participantCount,
    cap: dto.maxParticipants,
  };
}

// UI-facing types. Deliberately separate from src/types/backend.ts: these
// use playful, lowercase keys (scenes, Spanish status labels) that don't
// match the backend's real enums. src/lib/activityMapping.ts maps real
// ActivityResponse data into ExploreActivity/MisActivity for list cards; the
// activity detail page works with the raw backend type directly instead
// (see useActivity) since it needs fields this shape doesn't carry.

import type { UserDTO } from "@/types/backend";

export type SceneKey = "trekking" | "voley" | "cine" | "juegos" | "asado" | "cumple" | "picnic";

export type MockActivityType = "outdoor" | "indoor" | "mixed";

export type MockStatusKey =
  | "confirmada"
  | "propuesta"
  | "reprogramada"
  | "cancelada"
  | "votacion"
  | "finalizada";

interface ActivityBase {
  id: string;
  scene: SceneKey;
  title: string;
  type: MockActivityType;
  when: string;
  where: string;
  /** Real participant user ids (see src/lib/initials.ts's userIdToDisplayName),
   * for the card's avatar stack — not necessarily every participant, see
   * src/lib/activityMapping.ts. */
  participantIds: string[];
}

/** Explorar feed card and the "votación pendiente" card share this shape. */
export interface ExploreActivity extends ActivityBase {
  people: number;
}

/** "Mis actividades" feed card. */
export interface MisActivity extends ActivityBase {
  status: MockStatusKey;
  joined: number;
  cap: number;
}

export type Activity = ExploreActivity | MisActivity;

export type NotificationKind = "warn" | "info" | "reprog" | "cancel";

export interface WizardFormState {
  title: string;
  desc: string;
  type: MockActivityType;
  place: string;
  date: string;
  time: string;
  min: number;
  max: number;
  rain: number;
  wind: number;
  tMin: number;
  tMax: number;
  anticipation: string;
  reschedule: string;
}

/** The logged-in client, kept in the Redux session slice. Same shape as the
 * backend's `User`/`UserDTO` (just `id` + `name` — see
 * backend/src/main/java/com/solnotfound/entity/user/User.java): the backend
 * creates a user record lazily from the JWT subject the first time it's
 * seen, and never stores more than a display name. */
export type CurrentUser = UserDTO;

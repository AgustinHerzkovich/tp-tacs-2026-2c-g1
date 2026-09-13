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
  imageUrl: string | null;
  title: string;
  type: MockActivityType;
  /** Backend LocalDateTime retained for exact filtering, without timezone conversion. */
  dateTime: string;
  when: string;
  where: string;
  /** Real participant user ids (Keycloak subjects) — see
   * src/lib/initials.ts's participantDisplayName for why these can't be
   * resolved to real names for anyone but the current user. */
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
  latitude: number | null;
  longitude: number | null;
  images: Array<{ file: File; previewUrl: string }>;
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

/** Non-sensitive identity claims used by the UI, read from the Keycloak ID
 * token (see auth/AuthBootstrap.tsx) — same shape as the backend's
 * `User`/`UserDTO` plus the realm roles carried in the token. */
export interface CurrentUser extends UserDTO {
  roles: string[];
}

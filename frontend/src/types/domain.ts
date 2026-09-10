// UI-facing types for the mock-data-driven screens. These are deliberately
// separate from src/types/backend.ts: the mock model uses playful, lowercase
// keys (scenes, Spanish status labels) that don't match the backend's real
// enums. Once useActivities/useNotifications fetch real data, this is where
// the backend -> UI mapping layer's output type should live.

import type { UserDTO } from "@/types/backend";

export type SceneKey = "trekking" | "voley" | "cine" | "juegos" | "asado" | "cumple" | "picnic";

export type MockActivityType = "outdoor" | "indoor" | "mixed";

export type MockStatusKey = "confirmada" | "propuesta" | "reprogramada" | "cancelada" | "votacion";

interface ActivityBase {
  id: string;
  scene: SceneKey;
  title: string;
  type: MockActivityType;
  when: string;
  where: string;
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

export function isMisActivity(activity: Activity): activity is MisActivity {
  return "status" in activity;
}

export interface VoteOption {
  id: string;
  label: string;
  votes: number;
}

export type NotificationKind = "warn" | "info" | "reprog" | "cancel";

export interface NotificationItem {
  id: number;
  kind: NotificationKind;
  icon: string;
  title: string;
  body: string;
  time: string;
}

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

/** Non-sensitive identity claims used by the UI. Access and refresh tokens
 * remain in keycloak-js memory and are never persisted in Redux or storage. */
export interface CurrentUser extends UserDTO {
  roles: string[];
}

// Design-system-ish lookup tables the UI still needs regardless of where the
// activity/notification data comes from (colors, icons, labels per type or
// status, illustrated "scenes"). The activity/notification arrays that used
// to live here are gone — see useActivities/useActivity/useNotifications,
// which fetch the real thing from the backend now.

import type { MockActivityType, MockStatusKey, NotificationKind, SceneKey } from "@/types/domain";

export const STATUS_META: Record<MockStatusKey, { label: string; bg: string; ink: string }> = {
  confirmada: { label: "Confirmada", bg: "var(--mint)", ink: "var(--mint-ink)" },
  propuesta: { label: "Propuesta", bg: "var(--lav)", ink: "var(--lav-ink)" },
  reprogramada: { label: "Reprogramada", bg: "var(--violet)", ink: "var(--violet-ink)" },
  cancelada: { label: "Cancelada", bg: "var(--rose)", ink: "var(--rose-ink)" },
  votacion: { label: "Votación abierta", bg: "var(--sun)", ink: "var(--sun-ink)" },
  finalizada: { label: "Finalizada", bg: "var(--sky)", ink: "var(--sky-ink)" },
};

export const TYPE_META: Record<MockActivityType, { label: string; icon: string; bg: string; ink: string }> = {
  outdoor: { label: "Outdoor", icon: "🏕️", bg: "var(--mint)", ink: "var(--mint-ink)" },
  indoor: { label: "Indoor", icon: "🏠", bg: "var(--lav)", ink: "var(--lav-ink)" },
  mixed: { label: "Mixto", icon: "🎉", bg: "var(--sun)", ink: "var(--sun-ink)" },
};

export const SCENES: Record<SceneKey, { grad: [string, string]; deco: [string, string, string] }> = {
  trekking: { grad: ["#BAE6FD", "#A7F3D0"], deco: ["🏔️", "🌲", "🥾"] },
  voley: { grad: ["#FEF08A", "#FECDD3"], deco: ["🏐", "☀️", "🏖️"] },
  cine: { grad: ["#DDD6FE", "#BAE6FD"], deco: ["🎬", "🍿", "🌙"] },
  juegos: { grad: ["#FECDD3", "#EAD9FF"], deco: ["🎲", "🎉", "🃏"] },
  asado: { grad: ["#A7F3D0", "#FEF08A"], deco: ["🍖", "🔥", "🎈"] },
  cumple: { grad: ["#EAD9FF", "#FECDD3"], deco: ["🎂", "🎈", "✨"] },
  picnic: { grad: ["#FECDD3", "#BAE6FD"], deco: ["🧺", "☔", "🍇"] },
};

export const NOTIF_META: Record<NotificationKind, { bg: string; ink: string; border: string }> = {
  warn: { bg: "var(--sun)", ink: "var(--sun-ink)", border: "#F7DE6B" },
  info: { bg: "var(--sky)", ink: "var(--sky-ink)", border: "#8FD4F8" },
  reprog: { bg: "var(--violet)", ink: "var(--violet-ink)", border: "#D8B6FF" },
  cancel: { bg: "var(--rose)", ink: "var(--rose-ink)", border: "#FAAFBB" },
};

/** Decorative filler for list-card avatar stacks (Explorar/Mis Actividades)
 * — those cards don't carry real per-activity participant data (see
 * src/lib/activityMapping.ts). The activity detail page shows real
 * participants instead, derived from their user id (see userIdToDisplayName). */
export const PEOPLE: string[] = ["Juan L.", "Vale R.", "Fede M.", "Cami P.", "Nico G.", "Sol A."];
export const AV_COLORS: string[] = ["var(--mint)", "var(--lav)", "var(--sky)", "var(--sun)", "var(--rose)"];

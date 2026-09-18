// Presentation-only lookup tables for the activity UI: labels, icons and
// colors keyed by the backend's real enums (via src/lib/activityMapping.ts).
// This is NOT mock content — there is no activity/user data here, only how
// to draw a given status/type/notification kind. Real activity data comes
// exclusively from the backend (see src/lib/api.ts).

import type { MockActivityType, MockStatusKey, NotificationKind, SceneKey } from "@/types/domain";

/** The six pastel tones used across the whole app (chips, badges,
 * notifications, avatar fallbacks) — one canonical table so a color only
 * ever gets defined once. `bg`/`ink` back the Tailwind `bg-<tone>`/
 * `text-<tone>-ink` utilities (see the `--color-*` tokens in globals.css);
 * `border` is only for surfaces that need a coordinated border (e.g.
 * notification rows), not for Chip's sticker outline (always white). */
export type Tone = "mint" | "lav" | "violet" | "sky" | "sun" | "rose";

export const TONE_META: Record<Tone, { bg: string; ink: string; border: string }> = {
  mint: { bg: "var(--mint)", ink: "var(--mint-ink)", border: "#7BE0B0" },
  lav: { bg: "var(--lav)", ink: "var(--lav-ink)", border: "#C3B8FD" },
  violet: { bg: "var(--violet)", ink: "var(--violet-ink)", border: "#D8B6FF" },
  sky: { bg: "var(--sky)", ink: "var(--sky-ink)", border: "#8FD4F8" },
  sun: { bg: "var(--sun)", ink: "var(--sun-ink)", border: "#F7DE6B" },
  rose: { bg: "var(--rose)", ink: "var(--rose-ink)", border: "#FAAFBB" },
};

export const STATUS_META: Record<MockStatusKey, { label: string; tone: Tone }> = {
  confirmada: { label: "Confirmada", tone: "mint" },
  propuesta: { label: "Propuesta", tone: "lav" },
  reprogramada: { label: "Reprogramada", tone: "violet" },
  cancelada: { label: "Cancelada", tone: "rose" },
  votacion: { label: "Votación abierta", tone: "sun" },
  finalizada: { label: "Finalizada", tone: "sky" },
};

export const TYPE_META: Record<MockActivityType, { label: string; icon: string; tone: Tone }> = {
  outdoor: { label: "Outdoor", icon: "🏕️", tone: "mint" },
  indoor: { label: "Indoor", icon: "🏠", tone: "lav" },
  mixed: { label: "Mixto", icon: "🎉", tone: "sun" },
};

/** Illustrated gradient "photo" placeholder per activity — picked
 * deterministically from the activity's real id (see pickScene in
 * activityMapping.ts), not tied to any specific activity's content. */
export const SCENES: Record<SceneKey, { grad: [string, string]; deco: [string, string, string] }> = {
  trekking: { grad: ["#BAE6FD", "#A7F3D0"], deco: ["🏔️", "🌲", "🥾"] },
  voley: { grad: ["#FEF08A", "#FECDD3"], deco: ["🏐", "☀️", "🏖️"] },
  cine: { grad: ["#DDD6FE", "#BAE6FD"], deco: ["🎬", "🍿", "🌙"] },
  juegos: { grad: ["#FECDD3", "#EAD9FF"], deco: ["🎲", "🎉", "🃏"] },
  asado: { grad: ["#A7F3D0", "#FEF08A"], deco: ["🍖", "🔥", "🎈"] },
  cumple: { grad: ["#EAD9FF", "#FECDD3"], deco: ["🎂", "🎈", "✨"] },
  picnic: { grad: ["#FECDD3", "#BAE6FD"], deco: ["🧺", "☔", "🍇"] },
};

export const NOTIF_META: Record<NotificationKind, { tone: Tone }> = {
  warn: { tone: "sun" },
  info: { tone: "sky" },
  reprog: { tone: "violet" },
  cancel: { tone: "rose" },
};

/** Cycled by position for avatar-stack fallback backgrounds — decorative
 * only, not tied to any particular person. */
export const AV_TONES: Tone[] = ["mint", "lav", "sky", "sun", "rose"];

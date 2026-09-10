// Mock data for the Planazo UI. Once the backend REST API is wired up, the
// hooks in `src/hooks` that currently return this data will fetch it instead
// — components should keep depending on the hooks, not on this module.

import type {
  Activity,
  ExploreActivity,
  MisActivity,
  MockActivityType,
  MockStatusKey,
  NotificationItem,
  NotificationKind,
  SceneKey,
  VoteOption,
} from "@/types/domain";

export const STATUS_META: Record<MockStatusKey, { label: string; bg: string; ink: string }> = {
  confirmada: { label: "Confirmada", bg: "var(--mint)", ink: "var(--mint-ink)" },
  propuesta: { label: "Propuesta", bg: "var(--lav)", ink: "var(--lav-ink)" },
  reprogramada: { label: "Reprogramada", bg: "var(--violet)", ink: "var(--violet-ink)" },
  cancelada: { label: "Cancelada", bg: "var(--rose)", ink: "var(--rose-ink)" },
  votacion: { label: "Votación abierta", bg: "var(--sun)", ink: "var(--sun-ink)" },
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

export const EXPLORE_FEED: ExploreActivity[] = [
  { id: "trekking", scene: "trekking", title: "Trekking Cerro Otto", type: "outdoor", when: "Sáb 5 sep · 9:00", where: "Bariloche, Río Negro", people: 8 },
  { id: "voley", scene: "voley", title: "Torneo de vóley playero", type: "outdoor", when: "Sáb 12 sep · 10:00", where: "Playa Bristol, Mar del Plata", people: 20 },
  { id: "cine", scene: "cine", title: "Maratón de cine al aire libre", type: "mixed", when: "Vie 11 sep · 20:30", where: "Parque Sarmiento, Córdoba", people: 30 },
  { id: "juegos", scene: "juegos", title: "Noche de juegos de mesa", type: "indoor", when: "Jue 10 sep · 20:00", where: "Palermo, CABA", people: 8 },
];

export const VOTING_PENDING: ExploreActivity = {
  id: "trekking",
  scene: "trekking",
  title: "Trekking Cerro Otto",
  type: "outdoor",
  when: "Sáb 5 sep · 9:00",
  where: "Bariloche, Río Negro",
  people: 8,
};

export const MIS_FEED: MisActivity[] = [
  { id: "asado", scene: "asado", title: "Asado en Parque Centenario", type: "outdoor", status: "confirmada", when: "Dom 6 sep · 14:00", where: "Parque Centenario, CABA", joined: 12, cap: 15 },
  { id: "cumple", scene: "cumple", title: "Cumpleaños de Vale", type: "indoor", status: "reprogramada", when: "Dom 6 sep · 21:00", where: "San Telmo, CABA", joined: 15, cap: 20 },
  { id: "juegos", scene: "juegos", title: "Noche de juegos de mesa", type: "indoor", status: "propuesta", when: "Jue 10 sep · 20:00", where: "Palermo, CABA", joined: 4, cap: 8 },
  { id: "picnic", scene: "picnic", title: "Picnic Costanera", type: "outdoor", status: "cancelada", when: "era Sáb 29 ago · 17:00", where: "Costanera Sur, CABA", joined: 6, cap: 10 },
];

export const VOTE_OPTIONS_INIT: VoteOption[] = [
  { id: "a", label: "Dom 6 sep · 9:00", votes: 9 },
  { id: "b", label: "Dom 6 sep · 15:00", votes: 4 },
  { id: "c", label: "Lun 7 sep · 9:00", votes: 2 },
];

export const NOTIFS: NotificationItem[] = [
  { id: 1, kind: "warn", icon: "🌧️", title: "Pronóstico desfavorable", body: "Trekking Cerro Otto: se abrió una votación para reprogramar por lluvia.", time: "hace 12 min" },
  { id: 2, kind: "info", icon: "⏰", title: "Tu actividad está por comenzar", body: "Asado en Parque Centenario empieza en 2 horas.", time: "hace 1 h" },
  { id: 3, kind: "reprog", icon: "🔁", title: "Actividad reprogramada", body: "Cumpleaños de Vale se movió al domingo 6 sep a las 21:00.", time: "hace 3 h" },
  { id: 4, kind: "cancel", icon: "🚫", title: "Actividad cancelada", body: "Picnic Costanera fue cancelada: lluvia confirmada para todo el día.", time: "ayer" },
];

export const NOTIF_META: Record<NotificationKind, { bg: string; ink: string; border: string }> = {
  warn: { bg: "var(--sun)", ink: "var(--sun-ink)", border: "#F7DE6B" },
  info: { bg: "var(--sky)", ink: "var(--sky-ink)", border: "#8FD4F8" },
  reprog: { bg: "var(--violet)", ink: "var(--violet-ink)", border: "#D8B6FF" },
  cancel: { bg: "var(--rose)", ink: "var(--rose-ink)", border: "#FAAFBB" },
};

export const PEOPLE: string[] = ["Juan L.", "Vale R.", "Fede M.", "Cami P.", "Nico G.", "Sol A."];
export const AV_COLORS: string[] = ["var(--mint)", "var(--lav)", "var(--sky)", "var(--sun)", "var(--rose)"];

export function findActivity(id: string): Activity | undefined {
  return [...EXPLORE_FEED, ...MIS_FEED, VOTING_PENDING].find((a) => a.id === id);
}

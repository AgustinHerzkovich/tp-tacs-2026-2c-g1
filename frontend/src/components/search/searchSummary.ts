import { parseDateKey } from "./dateRangePresets";

const TYPE_LABELS: Record<string, string> = {
  Outdoor: "Outdoor",
  Indoor: "Indoor",
  Mixto: "Mixto",
};

/** Builds the short label shown on the collapsed pill (mobile trigger, and
 * the compact one docked in the sticky header once the full bar scrolls out
 * of view) — purely a formatted readout of state that already exists in
 * `ExplorarPage`, no new filter data. */
export function formatSearchSummary(params: {
  query: string;
  city: string;
  dateFrom: string;
  dateTo: string;
  type: string;
}): string {
  const parts: string[] = [];
  if (params.city.trim()) parts.push(params.city.trim());
  const from = parseDateKey(params.dateFrom);
  const to = parseDateKey(params.dateTo);
  if (from && to) {
    const fmt = (d: Date) => d.toLocaleDateString("es-AR", { day: "numeric", month: "short" });
    parts.push(from.getTime() === to.getTime() ? fmt(from) : `${fmt(from)} - ${fmt(to)}`);
  }
  if (params.query.trim()) parts.push(`“${params.query.trim()}”`);
  const typeLabel = TYPE_LABELS[params.type];
  if (typeLabel) parts.push(typeLabel);
  return parts.length > 0 ? parts.join(" · ") : "¿Qué plan buscás?";
}

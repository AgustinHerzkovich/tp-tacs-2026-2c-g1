/** The small "go look at this" affordance at the bottom of an activity card:
 * a label plus a circular arrow bubble. Purely decorative —
 * the whole card is already the click target (wrapped in a `Link`). */
export function CardGoLink({ label = "Ver plan" }: { label?: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <span
        className="text-[12.5px] font-extrabold"
        style={{ color: "var(--primary)" }}
      >
        {label}
      </span>
      <span
        className="flex size-[26px] shrink-0 items-center justify-center rounded-full text-sm font-extrabold text-white"
        style={{ background: "var(--primary)", boxShadow: "0 3px 0 #a8330c" }}
        aria-hidden="true"
      >
        →
      </span>
    </span>
  );
}

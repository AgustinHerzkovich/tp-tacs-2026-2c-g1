import { formatActivityWhen } from "@/lib/formatDate";

interface ActivityAboutCardProps {
  description: string | null;
  dateTime: string;
}

/** The "Sobre la actividad" torn-note card: a pin in one corner, a date tag
 * pinned to the other, both overlapping the card edge like stickers. */
export function ActivityAboutCard({ description, dateTime }: ActivityAboutCardProps) {
  return (
    <div className="relative bg-white rounded-tl-[4px] rounded-tr-[16px] rounded-br-[16px] rounded-bl-[4px] shadow-[0_6px_16px_-8px_rgba(58,51,82,.25)] -rotate-1 px-5 py-4 mb-6">
      <span className="emoji-3d absolute -top-3.5 left-6 text-[22px]" style={{ transform: "rotate(12deg)" }}>
        📌
      </span>
      <span
        className="absolute -top-2.5 right-4 whitespace-nowrap uppercase rounded-[10px] border-2 border-white px-2.5 py-1 text-[10px] font-black"
        style={{ background: "var(--sun)", color: "var(--sun-ink)", boxShadow: "0 3px 0 var(--sun-ink)", transform: "rotate(4deg)" }}
      >
        {formatActivityWhen(dateTime)}
      </span>
      <h3 className="font-display font-extrabold text-[13.5px] mb-1.5">Sobre la actividad</h3>
      <p className="text-[12.5px] font-semibold leading-relaxed" style={{ color: "var(--muted-foreground)" }}>
        {description ?? "Sin descripción."}
      </p>
    </div>
  );
}

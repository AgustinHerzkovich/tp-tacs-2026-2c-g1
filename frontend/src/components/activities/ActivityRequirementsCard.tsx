import { InfoHint } from "@/components/common/InfoHint";
import type { Tone } from "@/lib/activityVisuals";

interface ActivityRequirementsCardProps {
  minParticipants: number;
  anticipationWindow: number;
  reprogramationMaxDays: number;
}

interface Tile {
  emoji: string;
  tone: Tone;
  value: string;
  label: string;
  rotate: number;
}

/** The non-weather requirements for an activity to be confirmed (quórum,
 * aviso previo, ventana de reprogramación). Weather-specific limits live in
 * `WeatherWidget`'s own "dentro de lo permitido" chip instead — keeping them
 * here too would reintroduce the duplicate-info problem this redesign fixes. */
export function ActivityRequirementsCard({ minParticipants, anticipationWindow, reprogramationMaxDays }: ActivityRequirementsCardProps) {
  const tiles: Tile[] = [
    { emoji: "👥", tone: "mint", value: `${minParticipants} mín.`, label: "quórum", rotate: -3 },
    { emoji: "🔔", tone: "violet", value: `${anticipationWindow}h antes`, label: "aviso", rotate: 4 },
    { emoji: "🔄", tone: "sun", value: `${reprogramationMaxDays} días`, label: "reprogram.", rotate: -4 },
  ];

  return (
    <div className="mb-6">
      <div className="flex items-center gap-2 mb-3">
        <span
          className="inline-flex items-center rounded-tl-[10px] rounded-tr-[10px] rounded-br-[10px] rounded-bl-[2px] border-2 border-white px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wide"
          style={{ background: "var(--rose)", color: "var(--rose-ink)", boxShadow: "0 3px 0 var(--rose-ink)" }}
        >
          Para que se confirme
        </span>
        <InfoHint>
          Además de que el clima acompañe, la actividad necesita: llegar al quórum mínimo, respetar el aviso previo antes del
          inicio y —si el clima no da— tener margen dentro de la ventana de reprogramación.
        </InfoHint>
      </div>
      <div className="flex flex-wrap justify-center gap-3">
        {tiles.map((tile) => (
          <div
            key={tile.label}
            className="flex min-w-[96px] flex-1 flex-col items-center justify-center gap-1 rounded-[18px] border-[3px] border-white px-2 py-3.5 text-center shadow-[0_5px_12px_-6px_rgba(58,51,82,.28)]"
            style={{ background: `var(--${tile.tone})`, transform: `rotate(${tile.rotate}deg)` }}
          >
            <span className="emoji-3d text-[19px]">{tile.emoji}</span>
            <p className="font-black text-[14.5px]" style={{ color: `var(--${tile.tone}-ink)` }}>
              {tile.value}
            </p>
            <p className="text-[9.5px] font-extrabold" style={{ color: `var(--${tile.tone}-ink)` }}>
              {tile.label}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}

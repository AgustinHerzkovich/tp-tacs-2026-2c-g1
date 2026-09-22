import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { AV_TONES, TONE_META } from "@/lib/activityVisuals";
import { getInitials } from "@/lib/initials";

interface AvatarStackProps {
  names: string[];
  extra?: number;
  size?: "sm" | "default" | "lg";
}

/** Overlapping participant avatars, with a "+N" overflow bubble. */
export function AvatarStack({ names, extra = 0, size = "default" }: AvatarStackProps) {
  return (
    <div className="flex -space-x-2.5">
      {names.map((name, i) => {
        const tone = TONE_META[AV_TONES[i % AV_TONES.length]!];
        return (
          <Avatar key={`${name}-${i}`} size={size} className="ring-2 ring-white" title={name} aria-label={name}>
            <AvatarFallback
              className="font-brand"
              style={{ background: tone.bg, color: tone.ink }}
            >
              {getInitials(name)}
            </AvatarFallback>
          </Avatar>
        );
      })}
      {extra > 0 && (
        <Avatar size={size} className="ring-2 ring-white">
          <AvatarFallback className="font-brand bg-[var(--foreground)] text-white">
            +{extra}
          </AvatarFallback>
        </Avatar>
      )}
    </div>
  );
}

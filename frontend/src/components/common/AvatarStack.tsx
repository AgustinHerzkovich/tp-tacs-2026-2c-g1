import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { AV_COLORS } from "@/lib/activityVisuals";
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
      {names.map((name, i) => (
        <Avatar key={i} size={size} className="ring-2 ring-white">
          <AvatarFallback
            className="font-display font-bold text-[var(--foreground)]"
            style={{ background: AV_COLORS[i % AV_COLORS.length] }}
          >
            {getInitials(name)}
          </AvatarFallback>
        </Avatar>
      ))}
      {extra > 0 && (
        <Avatar size={size} className="ring-2 ring-white">
          <AvatarFallback className="font-display font-bold bg-[var(--foreground)] text-white">
            +{extra}
          </AvatarFallback>
        </Avatar>
      )}
    </div>
  );
}

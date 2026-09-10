import type { CSSProperties } from "react";
import { SCENES } from "@/lib/activityVisuals";
import type { SceneKey } from "@/types/domain";
import { cn } from "cn";

interface SceneProps {
  scene: SceneKey;
  height?: number;
  className?: string;
}

/** Illustrated gradient "photo" placeholder for an activity — a sticker-style
 * scene instead of a stock image, built entirely from CSS + emoji. */
export function Scene({ scene, height = 150, className }: SceneProps) {
  const s = SCENES[scene];
  return (
    <div
      className={cn("relative overflow-hidden", className)}
      style={{ height, background: `linear-gradient(135deg, ${s.grad[0]}, ${s.grad[1]})` }}
    >
      {s.deco.map((emoji, i) => (
        <span
          key={i}
          className="emoji-3d floaty absolute"
          style={
            {
              fontSize: 44 - i * 8,
              left: `${14 + i * 27}%`,
              top: `${18 + (i % 2 === 0 ? 8 : 40)}%`,
              "--rot": `${(i - 1) * 9}deg`,
              animationDelay: `${i * 0.3}s`,
            } as CSSProperties
          }
        >
          {emoji}
        </span>
      ))}
    </div>
  );
}

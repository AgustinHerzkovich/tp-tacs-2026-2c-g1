import type { SceneKey } from "@/types/domain";
import { cn } from "cn";

interface SceneProps {
  scene: SceneKey;
  imageUrl?: string | null;
  alt?: string;
  height?: number;
  className?: string;
}

/** Illustrated gradient "photo" placeholder for an activity — a sticker-style
 * scene instead of a stock image, built entirely from CSS + emoji. */
export function Scene({ scene, imageUrl, alt = "", height = 150, className }: SceneProps) {
  return (
    <div
      className={cn("relative overflow-hidden", className)}
      style={{ height, background: "var(--muted)" }}
      data-scene={scene}
    >
      {imageUrl && (
        // Presigned image hosts are configured at runtime, so next/image cannot whitelist them statically.
        // eslint-disable-next-line @next/next/no-img-element
        <img src={imageUrl} alt={alt} className="absolute inset-0 h-full w-full object-cover" />
      )}
      {!imageUrl && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2" style={{ color: "var(--muted-foreground)" }}>
          <div className="size-12 rounded-2xl border-2 border-dashed opacity-50" aria-hidden="true" />
          <span className="text-[11px] font-extrabold uppercase tracking-wide">Sin imagen</span>
        </div>
      )}
    </div>
  );
}

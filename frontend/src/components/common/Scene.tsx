"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import type { PatternKey, SceneKey } from "@/types/domain";
import { PATTERN_OVERLAYS, SCENES } from "@/lib/activityVisuals";
import { cn } from "cn";

interface SceneProps {
  scene: SceneKey;
  pattern?: PatternKey;
  imageUrl?: string | null;
  alt?: string;
  height?: number;
  className?: string;
  onRefresh?: () => void;
}

/** Gradient "photo" placeholder for an activity with no real image — the
 * gradient is picked deterministically per activity (see `pickScene`), and
 * a decorative overlay (dots, diagonal stripes, grid...) picked the same way
 * (see `pickPattern`) gives two activities sharing a gradient a different
 * look instead of an identical flat card. */
export function Scene({ scene, pattern = "plain", imageUrl, alt = "", height = 150, className, onRefresh }: SceneProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const [loadedUrl, setLoadedUrl] = useState<string | null>(null);
  const status = !imageUrl || loadedUrl === imageUrl ? "loaded" : failedUrl === imageUrl ? "error" : "loading";
  const { grad } = SCENES[scene];
  const overlay = PATTERN_OVERLAYS[pattern];
  const gradient = `linear-gradient(135deg, ${grad[0]}, ${grad[1]})`;

  return (
    <div
      className={cn("relative overflow-hidden", className)}
      style={
        imageUrl
          ? { height, background: "var(--muted)" }
          : {
              height,
              backgroundImage: overlay ? `${overlay.image}, ${gradient}` : gradient,
              backgroundSize: overlay?.size ? `${overlay.size}, auto` : undefined,
            }
      }
      data-scene={scene}
      data-pattern={imageUrl ? undefined : pattern}
    >
      {imageUrl && status !== "error" && (
        // Presigned image hosts are configured at runtime, so next/image cannot whitelist them statically.
        // eslint-disable-next-line @next/next/no-img-element
        <img src={imageUrl} alt={alt} className="absolute inset-0 h-full w-full object-cover" onLoad={() => setLoadedUrl(imageUrl)} onError={() => setFailedUrl(imageUrl)} />
      )}
      {imageUrl && status === "loading" && <div className="absolute inset-0 animate-pulse bg-muted" aria-label="Cargando imagen" />}
      {imageUrl && status === "error" && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2" style={{ color: "var(--muted-foreground)", background: "var(--muted)" }}>
          <span className="text-[11px] font-extrabold uppercase tracking-wide">Imagen no disponible</span>
          {onRefresh && (
            <button type="button" className="flex items-center gap-1 rounded-full bg-white px-2 py-1 text-[10px] font-extrabold" onClick={(event) => { event.preventDefault(); event.stopPropagation(); setFailedUrl(null); onRefresh(); }}>
              <RefreshCw className="size-3" /> Reintentar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

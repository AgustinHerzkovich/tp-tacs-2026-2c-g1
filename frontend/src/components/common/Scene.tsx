"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import type { SceneKey } from "@/types/domain";
import { cn } from "cn";

interface SceneProps {
  scene: SceneKey;
  imageUrl?: string | null;
  alt?: string;
  height?: number;
  className?: string;
  onRefresh?: () => void;
}

/** Illustrated gradient "photo" placeholder for an activity — a sticker-style
 * scene instead of a stock image, built entirely from CSS + emoji. */
export function Scene({ scene, imageUrl, alt = "", height = 150, className, onRefresh }: SceneProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const [loadedUrl, setLoadedUrl] = useState<string | null>(null);
  const status = !imageUrl || loadedUrl === imageUrl ? "loaded" : failedUrl === imageUrl ? "error" : "loading";

  return (
    <div
      className={cn("relative overflow-hidden", className)}
      style={{ height, background: "var(--muted)" }}
      data-scene={scene}
    >
      {imageUrl && status !== "error" && (
        // Presigned image hosts are configured at runtime, so next/image cannot whitelist them statically.
        // eslint-disable-next-line @next/next/no-img-element
        <img src={imageUrl} alt={alt} className="absolute inset-0 h-full w-full object-cover" onLoad={() => setLoadedUrl(imageUrl)} onError={() => setFailedUrl(imageUrl)} />
      )}
      {imageUrl && status === "loading" && <div className="absolute inset-0 animate-pulse bg-muted" aria-label="Cargando imagen" />}
      {(!imageUrl || status === "error") && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2" style={{ color: "var(--muted-foreground)" }}>
          <div className="size-12 rounded-2xl border-2 border-dashed opacity-50" aria-hidden="true" />
          <span className="text-[11px] font-extrabold uppercase tracking-wide">{status === "error" ? "Imagen no disponible" : "Sin imagen"}</span>
          {status === "error" && onRefresh && (
            <button type="button" className="flex items-center gap-1 rounded-full bg-white px-2 py-1 text-[10px] font-extrabold" onClick={(event) => { event.preventDefault(); event.stopPropagation(); setFailedUrl(null); onRefresh(); }}>
              <RefreshCw className="size-3" /> Reintentar
            </button>
          )}
        </div>
      )}
    </div>
  );
}

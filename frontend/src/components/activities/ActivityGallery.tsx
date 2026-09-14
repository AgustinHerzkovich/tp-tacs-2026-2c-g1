"use client";

import { useRef, useState } from "react";
import { ChevronLeft, ChevronRight, AlertCircle, RefreshCw } from "lucide-react";
import { Scene } from "@/components/common/Scene";
import type { SceneKey } from "@/types/domain";

interface ActivityGalleryProps {
  images: string[];
  scene: SceneKey;
  title: string;
  onRefresh?: () => void;
}

interface ImageState {
  loaded: boolean;
  error: boolean;
}

export function ActivityGallery({ images, scene, title, onRefresh }: ActivityGalleryProps) {
  const scroller = useRef<HTMLDivElement>(null);
  const [activeIndex, setActiveIndex] = useState(0);
  const [imageStates, setImageStates] = useState<Record<number, ImageState>>({});

  if (images.length === 0) {
    return <Scene scene={scene} alt={title} height={300} className="lg:h-[420px]!" />;
  }

  const goTo = (index: number) => {
    const nextIndex = Math.min(Math.max(index, 0), images.length - 1);
    scroller.current?.children[nextIndex]?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "start" });
    setActiveIndex(nextIndex);
  };

  const handleImageLoad = (index: number) => {
    setImageStates((prev) => ({ ...prev, [index]: { loaded: true, error: false } }));
  };

  const handleImageError = (index: number) => {
    setImageStates((prev) => ({ ...prev, [index]: { loaded: false, error: true } }));
  };

  const handleRetry = () => {
    setImageStates({});
    onRefresh?.();
  };

  const hasErrors = Object.values(imageStates).some((state) => state.error);

  return (
    <div className="relative h-[300px] lg:h-[420px] bg-black/5">
      {hasErrors && (
        <div className="absolute top-3 right-3 z-10 rounded-xl bg-destructive/90 px-3 py-2 flex items-center gap-2 shadow-lg" role="alert" aria-live="polite">
          <AlertCircle className="size-4 text-white" aria-hidden="true" />
          <span className="text-[11px] font-extrabold text-white">Algunas imágenes no cargaron</span>
          <button 
            type="button" 
            onClick={handleRetry} 
            className="size-6 rounded-full bg-white/20 hover:bg-white/30 flex items-center justify-center transition-colors"
            aria-label="Reintentar cargar imágenes"
          >
            <RefreshCw className="size-3 text-white" />
          </button>
        </div>
      )}
      <div
        ref={scroller}
        className="flex h-full overflow-x-auto snap-x snap-mandatory scrollbar-none"
        onScroll={(event) => {
          const width = event.currentTarget.clientWidth;
          if (width > 0) setActiveIndex(Math.round(event.currentTarget.scrollLeft / width));
        }}
      >
        {images.map((image, index) => {
          const state = imageStates[index];
          const hasError = state?.error;
          const isLoading = !state;

          return (
            <div key={image} className="h-full min-w-full snap-start relative">
              {hasError ? (
                <div className="size-full flex flex-col items-center justify-center bg-muted">
                  <AlertCircle className="size-8 mb-2" style={{ color: "var(--muted-foreground)" }} aria-hidden="true" />
                  <p className="text-xs font-bold text-center px-4" style={{ color: "var(--muted-foreground)" }}>
                    La imagen no está disponible
                  </p>
                  <button 
                    type="button" 
                    onClick={handleRetry} 
                    className="mt-2 px-3 py-1.5 rounded-xl bg-white border-2 text-[11px] font-extrabold hover:bg-muted transition-colors"
                  >
                    Reintentar
                  </button>
                </div>
              ) : (
                <>
                  {isLoading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-muted animate-pulse">
                      <div className="size-8 rounded-full border-2 border-t-[var(--primary)] border-transparent animate-spin" />
                    </div>
                  )}
                  {/* Presigned image hosts are configured at runtime, so next/image cannot whitelist them statically. */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img 
                    src={image} 
                    alt={`${title}, imagen ${index + 1}`} 
                    className="h-full min-w-full snap-start object-cover" 
                    onLoad={() => handleImageLoad(index)}
                    onError={() => handleImageError(index)}
                  />
                </>
              )}
            </div>
          );
        })}
      </div>

      {images.length > 1 && (
        <>
          <button 
            type="button" 
            onClick={() => goTo(activeIndex - 1)} 
            disabled={activeIndex === 0} 
            className="hidden lg:flex tap absolute left-4 top-1/2 -translate-y-1/2 size-10 rounded-full bg-white/90 items-center justify-center disabled:opacity-30" 
            aria-label="Imagen anterior"
          >
            <ChevronLeft className="size-5" />
          </button>
          <button 
            type="button" 
            onClick={() => goTo(activeIndex + 1)} 
            disabled={activeIndex === images.length - 1} 
            className="hidden lg:flex tap absolute right-4 top-1/2 -translate-y-1/2 size-10 rounded-full bg-white/90 items-center justify-center disabled:opacity-30" 
            aria-label="Imagen siguiente"
          >
            <ChevronRight className="size-5" />
          </button>
          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5 rounded-full bg-black/35 px-3 py-2" aria-label={`Imagen ${activeIndex + 1} de ${images.length}`}>
            {images.map((image, index) => (
              <button 
                key={image} 
                type="button" 
                onClick={() => goTo(index)} 
                className={`size-2 rounded-full ${index === activeIndex ? "bg-white" : "bg-white/45"}`} 
                aria-label={`Ver imagen ${index + 1}`} 
              />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

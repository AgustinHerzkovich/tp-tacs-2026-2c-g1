"use client";

import { useRef, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Scene } from "@/components/common/Scene";
import type { SceneKey } from "@/types/domain";

interface ActivityGalleryProps {
  images: string[];
  scene: SceneKey;
  title: string;
}

export function ActivityGallery({ images, scene, title }: ActivityGalleryProps) {
  const scroller = useRef<HTMLDivElement>(null);
  const [activeIndex, setActiveIndex] = useState(0);

  if (images.length === 0) {
    return <Scene scene={scene} alt={title} height={300} className="lg:h-[420px]!" />;
  }

  const goTo = (index: number) => {
    const nextIndex = Math.min(Math.max(index, 0), images.length - 1);
    scroller.current?.children[nextIndex]?.scrollIntoView({ behavior: "smooth", block: "nearest", inline: "start" });
    setActiveIndex(nextIndex);
  };

  return (
    <div className="relative h-[300px] lg:h-[420px] bg-black/5">
      <div
        ref={scroller}
        className="flex h-full overflow-x-auto snap-x snap-mandatory scrollbar-none"
        onScroll={(event) => {
          const width = event.currentTarget.clientWidth;
          if (width > 0) setActiveIndex(Math.round(event.currentTarget.scrollLeft / width));
        }}
      >
        {images.map((image, index) => (
          // Presigned image hosts are configured at runtime, so next/image cannot whitelist them statically.
          // eslint-disable-next-line @next/next/no-img-element
          <img key={image} src={image} alt={`${title}, imagen ${index + 1}`} className="h-full min-w-full snap-start object-cover" />
        ))}
      </div>

      {images.length > 1 && (
        <>
          <button type="button" onClick={() => goTo(activeIndex - 1)} disabled={activeIndex === 0} className="hidden lg:flex tap absolute left-4 top-1/2 -translate-y-1/2 size-10 rounded-full bg-white/90 items-center justify-center disabled:opacity-30" aria-label="Imagen anterior">
            <ChevronLeft className="size-5" />
          </button>
          <button type="button" onClick={() => goTo(activeIndex + 1)} disabled={activeIndex === images.length - 1} className="hidden lg:flex tap absolute right-4 top-1/2 -translate-y-1/2 size-10 rounded-full bg-white/90 items-center justify-center disabled:opacity-30" aria-label="Imagen siguiente">
            <ChevronRight className="size-5" />
          </button>
          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5 rounded-full bg-black/35 px-3 py-2" aria-label={`Imagen ${activeIndex + 1} de ${images.length}`}>
            {images.map((image, index) => (
              <button key={image} type="button" onClick={() => goTo(index)} className={`size-2 rounded-full ${index === activeIndex ? "bg-white" : "bg-white/45"}`} aria-label={`Ver imagen ${index + 1}`} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

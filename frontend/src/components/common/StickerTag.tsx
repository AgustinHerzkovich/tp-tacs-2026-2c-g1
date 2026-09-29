import type { ReactNode } from "react";
import type { Tone } from "@/lib/activityVisuals";

interface StickerTagProps {
  tone: Tone;
  children: ReactNode;
  className?: string;
}

/** Small uppercase badge with an asymmetric corner and a hard offset
 * shadow — the "torn sticker" look used for ActivityDetailPage's "Para que
 * se confirme" tag. Reused wherever a compact section needs a colored label
 * instead of a plain heading. */
export function StickerTag({ tone, children, className }: StickerTagProps) {
  return (
    <span
      className={`inline-flex items-center rounded-tl-[10px] rounded-tr-[10px] rounded-br-[10px] rounded-bl-[2px] border-2 border-white px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wide ${className ?? ""}`}
      style={{ background: `var(--${tone})`, color: `var(--${tone}-ink)`, boxShadow: `0 3px 0 var(--${tone}-ink)` }}
    >
      {children}
    </span>
  );
}

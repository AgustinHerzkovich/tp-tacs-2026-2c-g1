import type { ComponentProps, ElementType } from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "cn";

const chipVariants = cva(
  "inline-flex items-center justify-center gap-1.5 rounded-full px-3.5 py-1.5 text-[12.5px] font-extrabold whitespace-nowrap",
  {
    variants: {
      /** Static pastel tone for data-driven chips (status, type, notification
       * kind). Pass this OR `active` — not both. */
      tone: {
        mint: "bg-mint text-mint-ink",
        lav: "bg-lav text-lav-ink",
        violet: "bg-violet text-violet-ink",
        sky: "bg-sky text-sky-ink",
        sun: "bg-sun text-sun-ink",
        rose: "bg-rose text-rose-ink",
      },
      /** Selected/unselected look for a toggleable filter chip. Pass this OR
       * `tone` — not both. */
      active: {
        true: "bg-primary text-primary-foreground shadow-[0_3px_0_#a8330c]",
        false: "border-2 border-border bg-white text-muted-foreground",
      },
      /** Thick white border + soft shadow, for a chip sitting on top of a
       * photo (activity type/status tags) so it reads as a sticker. */
      sticker: {
        true: "border-2 border-white shadow-[0_2px_6px_rgba(58,51,82,.18)]",
        false: "",
      },
    },
    defaultVariants: {
      sticker: false,
    },
  },
);

interface ChipProps extends Omit<ComponentProps<"span">, "color">, VariantProps<typeof chipVariants> {
  as?: ElementType;
  /** Slight sticker wobble, in degrees (e.g. -3, 2). */
  rotate?: number;
}

export function Chip({ as, className, tone, active, sticker, rotate, style, ...props }: ChipProps) {
  const Comp = as ?? "span";
  return (
    <Comp
      className={cn(chipVariants({ tone, active, sticker }), className)}
      style={rotate ? { transform: `rotate(${rotate}deg)`, ...style } : style}
      {...props}
    />
  );
}

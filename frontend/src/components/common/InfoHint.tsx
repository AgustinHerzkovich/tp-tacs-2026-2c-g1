"use client";

import { useState } from "react";
import { Info } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

/** A small "(i)" affordance that explains a field on hover/focus, for
 * settings whose purpose isn't obvious from the label alone. Also toggles on
 * click/tap — Radix's tooltip is hover-only by default, which never opens on
 * touch devices, so this keeps it controlled to support both. */
export function InfoHint({ children }: { children: string }) {
  const [open, setOpen] = useState(false);

  return (
    <Tooltip open={open} onOpenChange={setOpen}>
      <TooltipTrigger asChild>
        <button
          type="button"
          className="tap inline-flex size-4 items-center justify-center rounded-full"
          style={{ color: "var(--muted-foreground)" }}
          aria-label="Más información"
          onClick={() => setOpen((value) => !value)}
        >
          <Info className="size-4" />
        </button>
      </TooltipTrigger>
      <TooltipContent>{children}</TooltipContent>
    </Tooltip>
  );
}

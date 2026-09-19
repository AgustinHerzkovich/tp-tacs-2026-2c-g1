import { Info } from "lucide-react";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

/** A small "(i)" affordance that explains a field on hover/focus, for
 * settings whose purpose isn't obvious from the label alone. */
export function InfoHint({ children }: { children: string }) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          type="button"
          className="tap inline-flex size-4 items-center justify-center rounded-full"
          style={{ color: "var(--muted-foreground)" }}
          aria-label="Más información"
        >
          <Info className="size-4" />
        </button>
      </TooltipTrigger>
      <TooltipContent>{children}</TooltipContent>
    </Tooltip>
  );
}

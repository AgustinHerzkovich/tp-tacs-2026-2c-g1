import type * as React from "react";
import { cn } from "cn";

/** Base pulsing placeholder block — compose into content-shaped skeletons
 * (see components/common/Skeletons.tsx) rather than using bare. */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("animate-pulse rounded-md", className)}
      style={{ background: "var(--muted)" }}
      {...props}
    />
  );
}

export { Skeleton };

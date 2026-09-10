import type { ReactNode } from "react";
import { Badge } from "@/components/ui/badge";
import { STATUS_META, TYPE_META } from "@/data/mockData";
import type { MockActivityType, MockStatusKey } from "@/types/domain";

interface PillBadgeProps {
  bg: string;
  ink: string;
  className?: string;
  children: ReactNode;
}

/** A Badge pre-styled with an inline background/foreground pair — used for
 * anything whose color comes from data (status, activity type) rather than
 * from the shadcn semantic variants. */
export function PillBadge({ bg, ink, className, children }: PillBadgeProps) {
  return (
    <Badge className={className} style={{ background: bg, color: ink }}>
      {children}
    </Badge>
  );
}

export function StatusBadge({ status, className }: { status: MockStatusKey; className?: string }) {
  const m = STATUS_META[status];
  return (
    <PillBadge bg={m.bg} ink={m.ink} className={className}>
      {m.label}
    </PillBadge>
  );
}

export function TypeBadge({ type, className }: { type: MockActivityType; className?: string }) {
  const m = TYPE_META[type];
  return (
    <PillBadge bg={m.bg} ink={m.ink} className={className}>
      {m.icon} {m.label}
    </PillBadge>
  );
}

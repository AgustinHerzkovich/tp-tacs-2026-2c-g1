import { Chip } from "@/components/common/Chip";
import { STATUS_META, TYPE_META } from "@/lib/activityVisuals";
import type { MockActivityType, MockStatusKey } from "@/types/domain";

export function StatusBadge({ status, className }: { status: MockStatusKey; className?: string }) {
  const m = STATUS_META[status];
  return (
    <Chip tone={m.tone} sticker className={className}>
      {m.label}
    </Chip>
  );
}

export function TypeBadge({ type, className }: { type: MockActivityType; className?: string }) {
  const m = TYPE_META[type];
  return (
    <Chip tone={m.tone} sticker className={className}>
      {m.icon} {m.label}
    </Chip>
  );
}

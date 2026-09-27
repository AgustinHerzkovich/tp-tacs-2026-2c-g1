"use client";

import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";

interface FiltersContentProps {
  onlyAvailable: boolean;
  onOnlyAvailableChange: (value: boolean) => void;
  includeAllStatuses: boolean;
  onIncludeAllStatusesChange: (value: boolean) => void;
  onClear: () => void;
}

export function FiltersContent({
  onlyAvailable, onOnlyAvailableChange,
  includeAllStatuses, onIncludeAllStatusesChange,
  onClear,
}: FiltersContentProps) {
  return (
    <div className="flex flex-col gap-4">
      <label className="flex items-center justify-between gap-3">
        <span className="text-[13.5px] font-bold">Solo con cupo disponible</span>
        <Switch checked={onlyAvailable} onCheckedChange={onOnlyAvailableChange} />
      </label>
      <label className="flex items-center justify-between gap-3">
        <span className="text-[13.5px] font-bold">Ver todas (incluso finalizadas)</span>
        <Switch checked={includeAllStatuses} onCheckedChange={onIncludeAllStatusesChange} />
      </label>
      <Button type="button" variant="outline" className="rounded-xl" onClick={onClear}>
        Limpiar todo
      </Button>
    </div>
  );
}

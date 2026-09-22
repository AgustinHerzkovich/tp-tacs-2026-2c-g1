"use client";

import { Button } from "@/components/ui/button";

interface FiltersContentProps {
  onlyAvailable: boolean;
  onOnlyAvailableChange: (value: boolean) => void;
  onClear: () => void;
}

export function FiltersContent({ onlyAvailable, onOnlyAvailableChange, onClear }: FiltersContentProps) {
  return (
    <div className="flex flex-col gap-4">
      <label className="flex items-center justify-between gap-3">
        <span className="text-[13.5px] font-bold">Solo con cupo disponible</span>
        <input
          type="checkbox"
          checked={onlyAvailable}
          onChange={(event) => onOnlyAvailableChange(event.target.checked)}
          className="size-5 accent-[var(--primary)]"
        />
      </label>
      <Button type="button" variant="outline" className="rounded-xl" onClick={onClear}>
        Limpiar todo
      </Button>
    </div>
  );
}

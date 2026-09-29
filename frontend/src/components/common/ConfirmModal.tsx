"use client";

import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription, SheetFooter } from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";

interface ConfirmModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  destructive?: boolean;
  onConfirm: () => void;
  pending?: boolean;
}

/** Bottom-sheet confirmation dialog for important or destructive actions
 * (discarding a draft, casting a vote, joining an activity). */
export function ConfirmModal({
  open,
  onOpenChange,
  title,
  description,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  destructive = false,
  onConfirm,
  pending = false,
}: ConfirmModalProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="bottom" showCloseButton={false} className="rounded-t-3xl border-0 px-5 pb-6">
        <div className="w-10 h-1.5 rounded-full bg-[var(--border)] mx-auto mt-1 mb-2" />
        <SheetHeader className="p-0 gap-2">
          <SheetTitle className="font-brand text-xl">{title}</SheetTitle>
          <SheetDescription className="text-[13.5px] font-bold leading-snug">{description}</SheetDescription>
        </SheetHeader>
        <SheetFooter className="p-0 flex-row gap-3 mt-4">
          <Button variant="outline" size="xl" className="flex-1" onClick={() => onOpenChange(false)} disabled={pending}>
            {cancelLabel}
          </Button>
          <Button
            size="xl"
            className="flex-1"
            variant={destructive ? "destructive" : "default"}
            onClick={onConfirm}
            disabled={pending}
          >
            {pending ? "Procesando..." : confirmLabel}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}

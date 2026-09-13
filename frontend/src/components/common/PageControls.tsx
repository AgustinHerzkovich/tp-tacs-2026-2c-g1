"use client";

import { Button } from "@/components/ui/button";

interface PageControlsProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function PageControls({ page, totalPages, onPageChange }: PageControlsProps) {
  if (totalPages <= 1) return null;

  return (
    <nav className="mt-6 flex items-center justify-center gap-3" aria-label="Paginación">
      <Button type="button" variant="outline" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
        Anterior
      </Button>
      <span className="text-xs font-extrabold" style={{ color: "var(--muted-foreground)" }}>
        Página {page + 1} de {totalPages}
      </span>
      <Button type="button" variant="outline" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
        Siguiente
      </Button>
    </nav>
  );
}

"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Chip } from "@/components/common/Chip";

interface PageControlsProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function PageControls({ page, totalPages, onPageChange }: PageControlsProps) {
  if (totalPages <= 1) return null;

  const atStart = page === 0;
  const atEnd = page + 1 >= totalPages;

  return (
    <nav className="mt-6 flex items-center justify-center gap-3" aria-label="Paginación">
      <button
        type="button"
        disabled={atStart}
        onClick={() => onPageChange(page - 1)}
        aria-label="Página anterior"
        className="tap size-9 rounded-full bg-white flex items-center justify-center disabled:opacity-30 disabled:shadow-none"
        style={atStart ? undefined : { boxShadow: "0 3px 0 var(--border)" }}
      >
        <ChevronLeft className="size-4" />
      </button>
      <Chip as="span" active={false}>
        Página {page + 1} de {totalPages}
      </Chip>
      <button
        type="button"
        disabled={atEnd}
        onClick={() => onPageChange(page + 1)}
        aria-label="Página siguiente"
        className="tap size-9 rounded-full bg-white flex items-center justify-center disabled:opacity-30 disabled:shadow-none"
        style={atEnd ? undefined : { boxShadow: "0 3px 0 var(--border)" }}
      >
        <ChevronRight className="size-4" />
      </button>
    </nav>
  );
}

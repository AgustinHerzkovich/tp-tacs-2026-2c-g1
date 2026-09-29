"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Chip } from "@/components/common/Chip";

interface PageControlsProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  /** `"default"` (the app-wide pill-and-shadow look) or `"quiet"` — plain
   * muted text and borderless arrows, for a control that sits under a list
   * inside an already-busy screen instead of a page's main pagination. */
  variant?: "default" | "quiet";
}

export function PageControls({ page, totalPages, onPageChange, variant = "default" }: PageControlsProps) {
  if (totalPages <= 1) return null;

  const atStart = page === 0;
  const atEnd = page + 1 >= totalPages;

  if (variant === "quiet") {
    return (
      <nav className="mt-1.5 flex items-center justify-center gap-1.5" aria-label="Paginación">
        <button
          type="button"
          disabled={atStart}
          onClick={() => onPageChange(page - 1)}
          aria-label="Página anterior"
          className="tap size-7 rounded-full flex items-center justify-center disabled:opacity-30"
          style={{ color: "var(--muted-foreground)" }}
        >
          <ChevronLeft className="size-3.5" />
        </button>
        <span className="text-[11.5px] font-bold px-1.5" style={{ color: "var(--muted-foreground)" }}>
          Página {page + 1} de {totalPages}
        </span>
        <button
          type="button"
          disabled={atEnd}
          onClick={() => onPageChange(page + 1)}
          aria-label="Página siguiente"
          className="tap size-7 rounded-full flex items-center justify-center disabled:opacity-30"
          style={{ color: "var(--muted-foreground)" }}
        >
          <ChevronRight className="size-3.5" />
        </button>
      </nav>
    );
  }

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

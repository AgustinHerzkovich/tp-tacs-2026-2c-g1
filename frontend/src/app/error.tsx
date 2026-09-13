"use client";

import { ErrorState } from "@/components/common/AsyncState";

export default function GlobalError({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main className="min-h-screen flex items-center justify-center p-6"><ErrorState message="Ocurrió un error inesperado al mostrar esta pantalla." retry={reset} /></main>;
}

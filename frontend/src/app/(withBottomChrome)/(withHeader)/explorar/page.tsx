import { Suspense } from "react";
import type { Metadata } from "next";
import { ExplorarPage } from "@/components/pages/ExplorarPage";

export const metadata: Metadata = { title: "Explorar actividades" };

export default function Page() {
  return (
    <Suspense
      fallback={
        <main className="min-h-screen px-5 py-6">
          <p className="text-center font-bold py-12" style={{ color: "var(--muted-foreground)" }}>
            Cargando…
          </p>
        </main>
      }
    >
      <ExplorarPage />
    </Suspense>
  );
}

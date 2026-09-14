import { Suspense } from "react";
import { StatisticsPage } from "@/components/pages/StatisticsPage";

export default function StatisticsRoute() {
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
      <StatisticsPage />
    </Suspense>
  );
}
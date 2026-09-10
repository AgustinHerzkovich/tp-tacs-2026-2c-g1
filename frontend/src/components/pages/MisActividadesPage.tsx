"use client";

import { useActivities } from "@/hooks/useActivities";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";

export function MisActividadesPage() {
  const { misFeed, votingPending, loading, error } = useActivities();

  return (
    <div className="fade-in px-5 pt-2 pb-4">
      {votingPending.length > 0 && (
        <>
          <p className="font-display font-semibold text-[13px] uppercase tracking-wide mb-2" style={{ color: "var(--muted-foreground)" }}>
            Votaciones pendientes
          </p>
          {votingPending.map((a) => (
            <VotingPendingCard key={a.id} activity={a} />
          ))}
        </>
      )}

      <p className="font-display font-semibold text-[13px] uppercase tracking-wide mt-6 mb-2" style={{ color: "var(--muted-foreground)" }}>
        Tus actividades
      </p>
      {loading && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          Cargando…
        </p>
      )}
      {error && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--destructive)" }}>
          {error}
        </p>
      )}
      {!loading && !error && misFeed.length === 0 && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          Todavía no tenés actividades.
        </p>
      )}
      {misFeed.map((a) => (
        <MisCard key={a.id} activity={a} />
      ))}
    </div>
  );
}

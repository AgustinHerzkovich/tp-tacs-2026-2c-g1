"use client";

import { useActivities } from "@/hooks/useActivities";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";

export function MisActividadesPage() {
  const { misFeed, votingPending, loading, error } = useActivities();
  const pendingIds = new Set(votingPending.map((a) => a.id));
  const rest = misFeed.filter((a) => !pendingIds.has(a.id));

  if (loading) {
    return (
      <p className="fade-in text-center text-[13px] font-bold py-10 px-5" style={{ color: "var(--muted-foreground)" }}>
        Cargando tus actividades…
      </p>
    );
  }

  if (error) {
    return (
      <p className="fade-in text-center text-[13px] font-bold py-10 px-5" style={{ color: "var(--rose-ink)" }}>
        {error}
      </p>
    );
  }

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
      {rest.map((a) => (
        <MisCard key={a.id} activity={a} />
      ))}
      {rest.length === 0 && votingPending.length === 0 && (
        <p className="text-center text-[13px] font-bold py-10" style={{ color: "var(--muted-foreground)" }}>
          Todavía no organizaste ni te sumaste a ninguna actividad.
        </p>
      )}
    </div>
  );
}

"use client";

import { useActivities } from "@/hooks/useActivities";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";

export function MisActividadesPage() {
  const { misFeed, votingPending, loading, error } = useActivities();
  const pendingIds = new Set(votingPending.map((activity) => activity.id));
  const remainingActivities = misFeed.filter((activity) => !pendingIds.has(activity.id));

  return (
    <div className="fade-in px-5 pt-2 pb-4 lg:px-10 lg:pt-8">
      <div className="hidden lg:block mb-7">
        <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Tu agenda</p>
        <h2 className="font-display font-semibold text-4xl">Mis actividades</h2>
      </div>
      {votingPending.length > 0 && (
        <>
          <p className="font-display font-semibold text-[13px] uppercase tracking-wide mb-2" style={{ color: "var(--muted-foreground)" }}>
            Votaciones pendientes
          </p>
          <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-4">
            {votingPending.map((a) => (
              <VotingPendingCard key={a.id} activity={a} />
            ))}
          </div>
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
          Todavía no organizaste ni te sumaste a ninguna actividad.
        </p>
      )}
      <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-4">
        {remainingActivities.map((a) => (
          <MisCard key={a.id} activity={a} />
        ))}
      </div>
    </div>
  );
}

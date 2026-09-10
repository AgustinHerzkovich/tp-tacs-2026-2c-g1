"use client";

import { useActivities } from "@/hooks/useActivities";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";

export function MisActividadesPage() {
  const { misFeed, votingPending } = useActivities();

  return (
    <div className="fade-in px-5 pt-2 pb-4">
      <p className="font-display font-semibold text-[13px] uppercase tracking-wide mb-2" style={{ color: "var(--muted-foreground)" }}>
        Votaciones pendientes
      </p>
      <VotingPendingCard activity={votingPending} />

      <p className="font-display font-semibold text-[13px] uppercase tracking-wide mt-6 mb-2" style={{ color: "var(--muted-foreground)" }}>
        Tus actividades
      </p>
      {misFeed.map((a) => (
        <MisCard key={a.id} activity={a} />
      ))}
    </div>
  );
}

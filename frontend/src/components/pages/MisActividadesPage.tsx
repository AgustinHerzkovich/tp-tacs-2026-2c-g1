"use client";

import { useMisActividades } from "@/hooks/useMisActividades";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";
import { SectionHeader } from "@/components/common/SectionHeader";
import { StickerTag } from "@/components/common/StickerTag";
import { ErrorState } from "@/components/common/AsyncState";
import { MisGridSkeleton } from "@/components/common/Skeletons";
import { PageControls } from "@/components/common/PageControls";
import type { MisActivity } from "@/types/domain";

function MisSection({
  icon,
  label,
  tone,
  total,
  activities,
  emptyText,
  page,
  totalPages,
  onPageChange,
  onRefreshImages,
}: {
  icon: string;
  label: string;
  tone: "violet" | "mint";
  total: number;
  activities: MisActivity[];
  emptyText: string;
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  onRefreshImages: () => void;
}) {
  return (
    <div className="mt-6">
      <SectionHeader icon={icon} label={label} count={total} tone={tone} />
      {activities.length === 0 ? (
        <p className="text-[12.5px] font-bold py-3" style={{ color: "var(--muted-foreground)" }}>
          {emptyText}
        </p>
      ) : (
        <>
          <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-4">
            {activities.map((a) => (
              <MisCard key={a.id} activity={a} onRefreshImages={onRefreshImages} />
            ))}
          </div>
          <PageControls page={page} totalPages={totalPages} onPageChange={onPageChange} variant="quiet" />
        </>
      )}
    </div>
  );
}

export function MisActividadesPage() {
  const {
    organizedFeed,
    organizedTotal,
    organizedPage,
    organizedTotalPages,
    setOrganizedPage,
    joinedFeed,
    joinedTotal,
    joinedPage,
    joinedTotalPages,
    setJoinedPage,
    votingPending,
    loading,
    error,
    refresh,
  } = useMisActividades();

  return (
    <div className="fade-in px-5 pt-2 pb-4 lg:mx-auto lg:w-full lg:max-w-7xl lg:px-10 lg:pt-8">
      <div className="hidden lg:block mb-7">
        <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Tu agenda</p>
        <h2 className="font-brand text-4xl" style={{ letterSpacing: "0.02em" }}>Mis actividades</h2>
      </div>

      {loading && <MisGridSkeleton />}
      {error && <ErrorState message={error} retry={refresh} />}

      {!loading && !error && (
        <>
          {votingPending.length > 0 && (
            <div className="mb-2">
              <StickerTag tone="mint" className="mb-3">
                🔥 Te toca votar
              </StickerTag>
              <div className="lg:grid lg:grid-cols-2 xl:grid-cols-3 lg:gap-4">
                {votingPending.map((a, i) => (
                  <VotingPendingCard key={a.id} activity={a} rotate={i % 2 === 0 ? -1 : 1} />
                ))}
              </div>
            </div>
          )}

          <MisSection
            icon="👑"
            label="Creadas por mí"
            tone="violet"
            total={organizedTotal}
            activities={organizedFeed}
            emptyText="Todavía no creaste ninguna actividad."
            page={organizedPage}
            totalPages={organizedTotalPages}
            onPageChange={setOrganizedPage}
            onRefreshImages={refresh}
          />

          <MisSection
            icon="✋"
            label="A las que te sumaste"
            tone="mint"
            total={joinedTotal}
            activities={joinedFeed}
            emptyText="Todavía no te sumaste a ninguna actividad."
            page={joinedPage}
            totalPages={joinedTotalPages}
            onPageChange={setJoinedPage}
            onRefreshImages={refresh}
          />
        </>
      )}
    </div>
  );
}

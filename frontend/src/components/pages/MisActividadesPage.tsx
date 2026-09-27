"use client";

import { useState, type ReactNode } from "react";
import { useMisActividades } from "@/hooks/useMisActividades";
import { VotingPendingCard } from "@/components/activities/VotingPendingCard";
import { MisCard } from "@/components/activities/MisCard";
import { CalendarView } from "@/components/activities/CalendarView";
import { SectionHeader } from "@/components/common/SectionHeader";
import { StickerTag } from "@/components/common/StickerTag";
import { ErrorState } from "@/components/common/AsyncState";
import { MisGridSkeleton } from "@/components/common/Skeletons";
import { PageControls } from "@/components/common/PageControls";
import type { MisActivity } from "@/types/domain";

type View = "calendar" | "list";

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
  const [view, setView] = useState<View>("calendar");
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
  } = useMisActividades({ skipFeeds: view === "calendar" });

  return (
    <div className="fade-in px-5 pt-2 pb-4 lg:mx-auto lg:w-full lg:max-w-7xl lg:px-10 lg:pt-8">
      <div className="hidden lg:flex lg:items-end lg:justify-between mb-7">
        <div>
          <p className="text-xs font-extrabold uppercase" style={{ color: "var(--primary)" }}>Tu agenda</p>
          <h2 className="font-brand text-4xl" style={{ letterSpacing: "0.02em" }}>Mis actividades</h2>
        </div>
        <ViewToggle view={view} onChange={setView} className="inline-flex" />
      </div>
      <ViewToggle view={view} onChange={setView} className="mb-5 flex w-full lg:hidden" />

      {!loading && !error && votingPending.length > 0 && (
        <div className="mb-5">
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

      {/* CalendarView owns its own fetch/loading/error (see useCalendarActivities), so it
          renders independently of useMisActividades — the loading/error states below only
          gate the list view and the "Te toca votar" banner above. */}
      {view === "calendar" && <CalendarView />}

      {view === "list" && loading && <MisGridSkeleton />}
      {view === "list" && error && <ErrorState message={error} retry={refresh} />}
      {view === "list" && !loading && !error && (
        <>
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

function ViewToggle({ view, onChange, className }: { view: View; onChange: (view: View) => void; className?: string }) {
  return (
    <div
      className={`gap-1 rounded-full border-2 bg-white p-1 ${className ?? ""}`}
      style={{ borderColor: "var(--border)" }}
    >
      <ViewToggleButton active={view === "list"} onClick={() => onChange("list")}>
        Lista
      </ViewToggleButton>
      <ViewToggleButton active={view === "calendar"} onClick={() => onChange("calendar")}>
        Calendario
      </ViewToggleButton>
    </div>
  );
}

function ViewToggleButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className="flex-1 rounded-full px-0 py-2 text-[13px] font-extrabold lg:flex-none lg:px-[22px] lg:py-2.5 lg:text-[13.5px]"
      style={
        active
          ? { background: "var(--primary)", color: "#fff" }
          : { background: "transparent", color: "var(--muted-foreground)" }
      }
    >
      {children}
    </button>
  );
}

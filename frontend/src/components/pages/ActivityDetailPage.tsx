"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, MapPin, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ActivityGallery } from "@/components/activities/ActivityGallery";
import { StatusBadge, TypeBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { ConfirmModal } from "@/components/common/ConfirmModal";
import { WeatherWidget } from "@/components/activities/WeatherWidget";
import { ActivityAboutCard } from "@/components/activities/ActivityAboutCard";
import { ActivityRequirementsCard } from "@/components/activities/ActivityRequirementsCard";
import { VotingRoom } from "@/components/activities/VotingRoom";
import { useActivity } from "@/hooks/useActivity";
import { useActivityWeather } from "@/hooks/useActivityWeather";
import { useVoting } from "@/hooks/useVoting";
import { useJoinActivity } from "@/hooks/useJoinActivity";
import { useAuth } from "@/hooks/useAuth";
import { mapActivityStatus, mapActivityType, pickScene } from "@/lib/activityMapping";
import { participantDisplayName } from "@/lib/initials";
import { api } from "@/lib/api";
import { ErrorState } from "@/components/common/AsyncState";
import { ActivityDetailSkeleton } from "@/components/common/Skeletons";
import { useToast } from "@/components/common/ToastProvider";

export function ActivityDetailPage({ id }: { id: string }) {
  const router = useRouter();
  const { user } = useAuth();
  const toast = useToast();
  const { activity, loading, notFound, error, refresh } = useActivity(id);
  const weather = useActivityWeather(id);
  const voting = useVoting(id, user);
  const [isOrganizer, setIsOrganizer] = useState(false);
  const infoColumnRef = useRef<HTMLDivElement>(null);
  const [votingMaxHeight, setVotingMaxHeight] = useState<number>();

  const initialJoined = activity?.participants.some((p) => p.userId === user?.id) ?? false;
  const join = useJoinActivity(id, initialJoined, refresh);

  const handleRefreshImages = () => {
    refresh();
  };

  useEffect(() => {
    let cancelled = false;
    api.activities.organized().then((activities) => {
      if (!cancelled) setIsOrganizer(activities.content.some((item) => item.id === id));
    }).catch(() => undefined);
    return () => { cancelled = true; };
  }, [id]);

  // Keeps the voting sidebar's max-height in sync with the info column next
  // to it (desktop only — see VotingRoom's `lg:max-h-[var(--voting-max-h)]`)
  // so its header and vote button stay visible while only the options list
  // scrolls, instead of the sidebar growing taller than its sibling column.
  useEffect(() => {
    const el = infoColumnRef.current;
    if (!el || typeof ResizeObserver === "undefined") return;
    const observer = new ResizeObserver(([entry]) => {
      if (entry) setVotingMaxHeight(entry.contentRect.height);
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  if (loading) return <ActivityDetailSkeleton />;

  if (notFound || !activity) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20">
        <p className="font-display font-semibold text-lg">No encontramos esa actividad.</p>
        <Button variant="outline" onClick={() => router.push("/explorar")}>
          Volver a explorar
        </Button>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20">
        <p className="font-display font-semibold text-lg" style={{ color: "var(--destructive)" }}>
          {error}
        </p>
        <Button variant="outline" onClick={() => router.push("/explorar")}>
          Volver a explorar
        </Button>
      </div>
    );
  }

  const scene = pickScene(activity.id);
  const type = mapActivityType(activity.type);
  const status = mapActivityStatus(activity.status);
  const hasVoting = voting.votation !== null;
  const participantNames = activity.participants.map((p) => p.name ?? participantDisplayName(p.userId, user));
  const maxRain = activity.weatherConditions.maxRainProbability;

  return (
    <div className="fade-in lg:max-w-5xl lg:mx-auto lg:py-8 lg:px-8 lg:pb-24">
      <div>
        <div className="relative lg:rounded-3xl lg:overflow-hidden">
          <ActivityGallery images={activity.imageUrls} scene={scene} title={activity.title} onRefresh={handleRefreshImages} />
          <div className="absolute inset-x-0 bottom-0 h-28" style={{ background: "linear-gradient(to top, rgba(58,51,82,.75), transparent)" }} />
          <button
            onClick={() => router.back()}
            className="tap absolute top-6 left-5 w-10 h-10 rounded-full bg-white/90 backdrop-blur shadow-[0_3px_0_var(--lav)] flex items-center justify-center"
            aria-label="Volver"
          >
            <ArrowLeft className="size-[18px]" />
          </button>
          <div
            className="absolute top-6 right-5 flex w-[58px] h-[58px] flex-col items-center justify-center rounded-full border-[3px] border-white shadow-[0_4px_10px_-4px_rgba(58,51,82,.35)]"
            style={{ background: "var(--lav)", transform: "rotate(6deg)" }}
          >
            <span className="text-[13.5px] font-black leading-none" style={{ color: "var(--lav-ink)" }}>
              {activity.participantCount}/{activity.maxParticipants}
            </span>
            <span className="mt-0.5 text-[6.5px] font-extrabold uppercase leading-none" style={{ color: "var(--lav-ink)" }}>
              anotados
            </span>
          </div>
          <div className="absolute left-5 right-5 bottom-4">
            <div className="flex gap-2 mb-2">
              <TypeBadge type={type} />
              <StatusBadge status={status} />
            </div>
            <h1 className="font-brand text-[24px] text-white leading-tight drop-shadow">{activity.title}</h1>
            <p className="text-[12.5px] font-extrabold text-white/90 mt-1 flex items-center gap-1">
              <MapPin className="size-[13px]" /> {activity.location.city ?? "Ubicación a confirmar"}
            </p>
          </div>
        </div>

        <div className={`px-5 pt-5 lg:px-0 lg:grid lg:gap-5 lg:items-start ${hasVoting ? "lg:grid-cols-[minmax(0,1fr)_minmax(320px,0.7fr)]" : "lg:grid-cols-1"}`}>
          <div className="min-w-0" ref={infoColumnRef}>
            <ActivityAboutCard description={activity.description} dateTime={activity.dateTime} />
            <WeatherWidget
              loading={weather.loading}
              unavailable={weather.unavailable}
              forecast={weather.weather?.activityForecast ?? null}
              current={weather.weather?.currentWeather ?? null}
              conditions={activity.weatherConditions}
              forcedExceeded={hasVoting}
            />
            <ActivityRequirementsCard
              minParticipants={activity.minParticipants}
              anticipationWindow={activity.anticipationWindow}
              reprogramationMaxDays={activity.reprogramationRange.maxDays}
            />
          </div>
          {hasVoting && (
            <VotingRoom
              voting={voting}
              warningText={
                maxRain != null
                  ? `Se superó el máximo de lluvia permitido (${maxRain}%). Elegí una fecha alternativa para reprogramar.`
                  : undefined
              }
              maxHeightPx={votingMaxHeight}
            />
          )}
        </div>
      </div>

      <div className="px-5 py-4 border-t-2 flex items-center gap-4 lg:px-0" style={{ borderColor: "var(--border)" }}>
        <div>
          <AvatarStack names={participantNames.slice(0, 3)} extra={Math.max(0, participantNames.length - 3)} />
          <p className="text-[10px] font-extrabold mt-1" style={{ color: "var(--muted-foreground)" }}>
            {activity.availability ? "quedan cupos disponibles" : "sin cupos disponibles"}
          </p>
        </div>
        {activity.status === "CANCELLED" || activity.status === "FINISHED" ? (
          <p className="flex-1 text-center text-[12.5px] font-bold" style={{ color: "var(--destructive)" }}>
            {activity.status === "CANCELLED" ? "Esta actividad fue cancelada" : "Esta actividad finalizó"}
          </p>
        ) : join.joined ? (
          <>
            <div
              className="flex min-h-[52px] flex-1 items-center justify-center rounded-[18px] border-2 border-white px-3.5 text-[12px] font-black"
              style={{ background: "var(--mint)", color: "var(--mint-ink)", boxShadow: "0 4px 0 var(--mint-ink)", transform: "rotate(-0.6deg)" }}
            >
              Ya estás sumado
            </div>
            <Button
              variant="destructive"
              size="icon-lg"
              className="size-[52px] rounded-[18px]"
              disabled={join.pending}
              onClick={join.requestLeave}
              aria-label="Bajarme de la actividad"
            >
              <LogOut className="size-4" />
            </Button>
          </>
        ) : (
          <Button
            size="xl"
            className="flex-1"
            disabled={join.pending || !activity.availability}
            onClick={join.requestJoin}
          >
            {activity.availability ? "Sumarme a la actividad" : "Sin cupos disponibles"}
          </Button>
        )}
      </div>

      <ConfirmModal
        open={voting.confirmOpen}
        onOpenChange={(v) => !v && voting.cancelVote()}
        title="Confirmá tu voto"
        description={`¿Confirmás tu voto por "${voting.selectedOption?.label ?? ""}"? No vas a poder cambiarlo después.`}
        confirmLabel="Confirmar voto"
        pending={voting.pending}
        onConfirm={() => void voting.confirmVote().then(() => toast("Tu voto quedó registrado."), () => toast("No pudimos registrar tu voto.", "error"))}
      />
      <ConfirmModal
        open={join.confirmOpen}
        onOpenChange={(v) => !v && join.cancelJoin()}
        title="¿Sumarte a esta actividad?"
        description="Vas a recibir notificaciones sobre el clima, la fecha y los demás participantes."
        confirmLabel="Sí, sumarme"
        pending={join.pending}
        onConfirm={() => void join.confirmJoin().then(() => toast("Te sumaste a la actividad."), () => toast("No pudimos sumarte a la actividad.", "error"))}
      />
      <ConfirmModal
        open={join.leaveConfirmOpen}
        onOpenChange={(v) => !v && join.cancelLeave()}
        title="¿Darte de baja de la actividad?"
        description="Vas a dejar de recibir notificaciones sobre el clima, la fecha y los demás participantes."
        confirmLabel="Sí, bajarme"
        pending={join.pending}
        onConfirm={() => void join.confirmLeave().then(() => toast("Te diste de baja de la actividad."), () => toast("No pudimos darte de baja.", "error"))}
      />
    </div>
  );
}

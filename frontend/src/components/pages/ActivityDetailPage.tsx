"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft, MapPin, Clock } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ActivityGallery } from "@/components/activities/ActivityGallery";
import { PillBadge, TypeBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { ConfirmModal } from "@/components/common/ConfirmModal";
import { WeatherWidget } from "@/components/activities/WeatherWidget";
import { VotingRoom } from "@/components/activities/VotingRoom";
import { useActivity } from "@/hooks/useActivity";
import { useActivityWeather } from "@/hooks/useActivityWeather";
import { useVoting } from "@/hooks/useVoting";
import { useJoinActivity } from "@/hooks/useJoinActivity";
import { useAuth } from "@/hooks/useAuth";
import { STATUS_META } from "@/lib/activityVisuals";
import { mapActivityStatus, mapActivityType, pickScene } from "@/lib/activityMapping";
import { formatActivityWhen } from "@/lib/formatDate";
import { participantDisplayName } from "@/lib/initials";

export function ActivityDetailPage({ id }: { id: string }) {
  const router = useRouter();
  const { user } = useAuth();
  const { activity, loading, notFound, error } = useActivity(id);
  const weather = useActivityWeather(id);
  const voting = useVoting(id);

  const initialJoined = activity?.participants.some((p) => p.userId === user?.id) ?? false;
  const join = useJoinActivity(id, initialJoined);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20">
        <p className="font-display font-semibold text-lg">Cargando…</p>
      </div>
    );
  }

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
  const statusMeta = STATUS_META[status];
  const hasVoting = voting.votation !== null;
  const participantNames = activity.participants.map((p) => participantDisplayName(p.userId, user));
  const maxRain = activity.weatherConditions.maxRainProbability;

  return (
    <div className="fade-in lg:max-w-5xl lg:mx-auto lg:py-8 lg:px-8 lg:pb-24">
      <div>
        <div className="relative lg:rounded-3xl lg:overflow-hidden">
          <ActivityGallery images={activity.imageUrls} scene={scene} title={activity.title} />
          <div className="absolute inset-x-0 bottom-0 h-28" style={{ background: "linear-gradient(to top, rgba(58,51,82,.75), transparent)" }} />
          <button
            onClick={() => router.back()}
            className="tap absolute top-6 left-5 w-10 h-10 rounded-full bg-white/90 backdrop-blur flex items-center justify-center"
            aria-label="Volver"
          >
            <ArrowLeft className="size-[18px]" />
          </button>
          <div className="absolute left-5 right-5 bottom-4">
            <div className="flex gap-2 mb-2">
              <TypeBadge type={type} />
              <PillBadge bg={statusMeta.bg} ink={statusMeta.ink}>
                {statusMeta.label}
              </PillBadge>
            </div>
            <h1 className="font-display font-semibold text-[22px] text-white leading-tight drop-shadow">{activity.title}</h1>
            <p className="text-[12.5px] font-extrabold text-white/90 mt-1 flex items-center gap-1">
              <MapPin className="size-[13px]" /> {activity.location.city ?? "Ubicación a confirmar"}
            </p>
          </div>
        </div>

        <div className="px-5 pt-5 lg:px-0 lg:grid lg:grid-cols-[minmax(0,1fr)_minmax(320px,0.7fr)] lg:gap-5 lg:items-start">
          <WeatherWidget loading={weather.loading} unavailable={weather.unavailable} forecast={weather.weather?.activityForecast ?? null} />

          {hasVoting && (
            <VotingRoom
              voting={voting}
              warningText={
                maxRain != null
                  ? `Se superó el máximo de lluvia permitido (${maxRain}%). Elegí una fecha alternativa para reprogramar.`
                  : undefined
              }
            />
          )}

          <div className="mb-4 lg:col-start-1 lg:row-start-1 lg:mt-36">
            <h3 className="font-display font-semibold text-[15px] mb-2">Sobre la actividad</h3>
            <p className="text-[13px] font-semibold leading-relaxed mb-3" style={{ color: "var(--muted-foreground)" }}>
              {activity.description ?? "Sin descripción."}
            </p>
            <div className="grid grid-cols-2 gap-3">
              <Card className="p-3.5 rounded-2xl">
                <p className="text-[10px] font-extrabold uppercase mb-1 px-4" style={{ color: "var(--muted-foreground)" }}>
                  Fecha y hora
                </p>
                <p className="font-display font-semibold text-[13px] px-4 flex items-center gap-1">
                  <Clock className="size-[13px]" /> {formatActivityWhen(activity.dateTime)}
                </p>
              </Card>
              <Card className="p-3.5 rounded-2xl">
                <p className="text-[10px] font-extrabold uppercase mb-1 px-4" style={{ color: "var(--muted-foreground)" }}>
                  Participantes
                </p>
                <p className="font-display font-semibold text-[13px] px-4">
                  👥 {activity.participantCount}/{activity.maxParticipants}
                </p>
              </Card>
            </div>
          </div>
        </div>
      </div>

      <div className="px-5 py-4 border-t-2 flex items-center gap-4 lg:px-0" style={{ borderColor: "var(--border)" }}>
        <div>
          <AvatarStack names={participantNames.slice(0, 3)} extra={Math.max(0, participantNames.length - 3)} />
          <p className="text-[10px] font-extrabold mt-1" style={{ color: "var(--muted-foreground)" }}>
            {activity.availability ? "quedan cupos disponibles" : "sin cupos disponibles"}
          </p>
        </div>
        {hasVoting ? (
          <p className="flex-1 text-center text-[12.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
            Votá en la sala de votación arriba ↑
          </p>
        ) : (
          <Button
            className="flex-1 h-auto py-3.5 rounded-2xl font-display font-semibold"
            disabled={join.pending || (!join.joined && !activity.availability)}
            style={join.joined ? { background: "var(--mint)", color: "var(--mint-ink)" } : undefined}
            onClick={() => (join.joined ? void join.leave() : join.requestJoin())}
          >
            {join.joined ? "¡Estás sumado! ✓" : activity.availability ? "Sumarme a la actividad" : "Sin cupos disponibles"}
          </Button>
        )}
      </div>

      <ConfirmModal
        open={voting.confirmOpen}
        onOpenChange={(v) => !v && voting.cancelVote()}
        title="Confirmá tu voto"
        description={`¿Confirmás tu voto por "${voting.selectedOption?.label ?? ""}"? No vas a poder cambiarlo después.`}
        confirmLabel="Confirmar voto"
        onConfirm={() => void voting.confirmVote()}
      />
      <ConfirmModal
        open={join.confirmOpen}
        onOpenChange={(v) => !v && join.cancelJoin()}
        title="¿Sumarte a esta actividad?"
        description="Vas a recibir notificaciones sobre el clima, la fecha y los demás participantes."
        confirmLabel="Sí, sumarme"
        onConfirm={() => void join.confirmJoin()}
      />
    </div>
  );
}

"use client";

import { useRouter } from "next/navigation";
import { ArrowLeft, MapPin, Clock } from "lucide-react";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Scene } from "@/components/common/Scene";
import { PillBadge, TypeBadge } from "@/components/common/PillBadge";
import { AvatarStack } from "@/components/common/AvatarStack";
import { ConfirmModal } from "@/components/common/ConfirmModal";
import { WeatherWidget } from "@/components/activities/WeatherWidget";
import { VotingRoom } from "@/components/activities/VotingRoom";
import { useActivity } from "@/hooks/useActivity";
import { useActivityWeather } from "@/hooks/useActivityWeather";
import { useVoting } from "@/hooks/useVoting";
import { useJoinActivity } from "@/hooks/useJoinActivity";
import { STATUS_META } from "@/data/mockData";
import { mapActivityType, mapActivityStatus, pickScene } from "@/lib/activityMapping";
import { formatActivityWhen } from "@/lib/formatDate";
import { userIdToDisplayName } from "@/lib/initials";
import { BACKEND_FALLBACK_USER_ID } from "@/lib/constants";

export function ActivityDetailPage({ id }: { id: string }) {
  const router = useRouter();
  const { activity, loading, notFound, error } = useActivity(id);
  const weather = useActivityWeather(id);
  const voting = useVoting(id);

  const initialJoined =
    activity?.participants.some((p) => p.userId === BACKEND_FALLBACK_USER_ID) ?? false;
  const join = useJoinActivity(id, initialJoined);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center gap-3 py-20">
        <p className="font-display font-semibold text-lg">Cargando actividad…</p>
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
        <p className="font-display font-semibold text-lg" style={{ color: "var(--rose-ink)" }}>
          {error}
        </p>
      </div>
    );
  }

  const hasVoting = activity.status === "PROPOSED";
  const statusMeta = STATUS_META[mapActivityStatus(activity.status)];
  const forecast = weather.weather?.activityForecast ?? weather.weather?.currentWeather ?? null;
  const participantNames = activity.participants.map((p) => userIdToDisplayName(p.userId));

  return (
    <div className="fade-in">
      <div>
        <div className="relative">
          <Scene scene={pickScene(activity.id)} height={220} />
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
              <TypeBadge type={mapActivityType(activity.type)} />
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

        <div className="px-5 pt-5">
          <WeatherWidget loading={weather.loading} unavailable={weather.unavailable} forecast={forecast} />

          {hasVoting && (
            <VotingRoom
              voting={voting}
              warningText={`El pronóstico no cumple las condiciones configuradas (máx. ${activity.weatherConditions.maxRainProbability}% de lluvia). Elegí una fecha alternativa.`}
            />
          )}

          {activity.description && (
            <div className="mb-4">
              <h3 className="font-display font-semibold text-[15px] mb-2">Sobre la actividad</h3>
              <p className="text-[13px] font-semibold leading-relaxed mb-3" style={{ color: "var(--muted-foreground)" }}>
                {activity.description}
              </p>
            </div>
          )}

          <div className="grid grid-cols-2 gap-3 mb-4">
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

      <div className="px-5 py-4 border-t-2 flex items-center gap-4" style={{ borderColor: "var(--border)" }}>
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
            onClick={() => (join.joined ? join.leave() : join.requestJoin())}
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
        onConfirm={voting.confirmVote}
      />
      <ConfirmModal
        open={join.confirmOpen}
        onOpenChange={(v) => !v && join.cancelJoin()}
        title="¿Sumarte a esta actividad?"
        description="Vas a recibir notificaciones sobre el clima, la fecha y los demás participantes."
        confirmLabel="Sí, sumarme"
        onConfirm={join.confirmJoin}
      />
    </div>
  );
}

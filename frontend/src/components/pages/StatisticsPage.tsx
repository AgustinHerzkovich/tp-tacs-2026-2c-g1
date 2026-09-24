"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, CloudSun } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Chip } from "@/components/common/Chip";
import { ErrorState, LoadingState } from "@/components/common/AsyncState";
import { StatisticsSkeleton } from "@/components/common/Skeletons";
import { useAuth } from "@/hooks/useAuth";
import { api } from "@/lib/api";
import type { ActivityStatisticsResponse, StatisticsResponse } from "@/types/backend";
import { dateKey, formatLocalDate, localDayRange, sanitizeDate, shiftDays } from "@/lib/statisticsRange";

function formatRangeLabel(fromIso?: string, toIso?: string): string {
  if (fromIso && toIso) return `${formatLocalDate(fromIso)} al ${formatLocalDate(toIso)}`;
  if (fromIso) return `desde el ${formatLocalDate(fromIso)}`;
  if (toIso) return `hasta el ${formatLocalDate(toIso)}`;
  return "últimos 7 días";
}

const ACTIVITY_METRICS: {
  key: keyof ActivityStatisticsResponse;
  label: string;
  icon: string;
  tone: "mint" | "violet" | "rose" | "sky";
}[] = [
  { key: "created", label: "Creadas", icon: "✨", tone: "mint" },
  { key: "rescheduled", label: "Reprogramadas", icon: "🔁", tone: "violet" },
  { key: "cancelled", label: "Canceladas", icon: "🚫", tone: "rose" },
  { key: "cancelledByWeather", label: "Suspendidas por clima", icon: "🌧️", tone: "sky" },
];

export function StatisticsPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { initialized, isAuthenticated, hasRole } = useAuth();

  const [statistics, setStatistics] = useState<StatisticsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);

  const [from, setFrom] = useState(() => sanitizeDate(searchParams?.get("from")));
  const [to, setTo] = useState(() => sanitizeDate(searchParams?.get("to")));
  const [observedSignature, setObservedSignature] = useState(() => `${from}|${to}`);

  const [appliedSignature, setAppliedSignature] = useState<string | null>(null);
  const [appliedParams, setAppliedParams] = useState<{ from?: string; to?: string } | undefined>(undefined);

  const todayKey = useMemo(() => dateKey(new Date()), []);
  const invalidRange = Boolean(from && to && from > to);
  const signature = `${from}|${to}`;

  const urlFrom = sanitizeDate(searchParams?.get("from"));
  const urlTo = sanitizeDate(searchParams?.get("to"));
  const urlSignature = `${urlFrom}|${urlTo}`;
  if (urlSignature !== observedSignature) {
    setObservedSignature(urlSignature);
    if (urlFrom !== from || urlTo !== to) {
      setFrom(urlFrom);
      setTo(urlTo);
    }
  }

  if (!invalidRange && appliedSignature !== signature) {
    setAppliedSignature(signature);
    const range = localDayRange(from, to);
    setAppliedParams(range);
    setStatistics(null);
    setError(null);
    setLoading(true);
  }

  const presets = [
    { label: "Últimos 7 días", from: shiftDays(todayKey, -6), to: todayKey },
    { label: "Últimos 30 días", from: shiftDays(todayKey, -29), to: todayKey },
    { label: "Últimos 90 días", from: shiftDays(todayKey, -89), to: todayKey },
    { label: "Este mes", from: `${todayKey.slice(0, 8)}01`, to: todayKey },
  ];

  useEffect(() => {
    if (!initialized) return;
    if (!isAuthenticated) {
      router.replace("/login");
      return;
    }
    if (!hasRole("ADMIN")) {
      router.replace("/mis-actividades");
      return;
    }

    let cancelled = false;
    api.statistics
      .get(appliedParams)
      .then((result) => {
        if (!cancelled) setStatistics(result);
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : "No pudimos cargar las estadísticas.");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [appliedParams, attempt, hasRole, initialized, isAuthenticated, router]);

  const replaceUrl = (nextFrom: string, nextTo: string) => {
    const params = new URLSearchParams();
    if (nextFrom) params.set("from", nextFrom);
    if (nextTo) params.set("to", nextTo);
    const query = params.toString();
    router.replace(query ? `/estadisticas?${query}` : "/estadisticas", { scroll: false });
  };

  const handleFromChange = (value: string) => {
    setFrom(value);
    replaceUrl(value, to);
  };

  const handleToChange = (value: string) => {
    setTo(value);
    replaceUrl(from, value);
  };

  const applyPreset = (nextFrom: string, nextTo: string) => {
    setFrom(nextFrom);
    setTo(nextTo);
    replaceUrl(nextFrom, nextTo);
  };

  const retry = () => {
    setError(null);
    setLoading(true);
    setAttempt((current) => current + 1);
  };

  if (!initialized) {
    return (
      <main className="min-h-screen px-5 py-6">
        <LoadingState label="Comprobando sesión..." />
      </main>
    );
  }
  if (!isAuthenticated || !hasRole("ADMIN")) return null;

  const totalEvents = statistics
    ? statistics.activities.created +
      statistics.activities.rescheduled +
      statistics.activities.cancelled +
      statistics.activities.cancelledByWeather
    : 0;
  const successRate =
    statistics && statistics.weatherProvider.requests > 0
      ? Math.round((statistics.weatherProvider.successful / statistics.weatherProvider.requests) * 100)
      : null;

  return (
    <main className="fade-in min-h-screen px-5 py-6 lg:mx-auto lg:w-full lg:max-w-5xl lg:px-10">
      <div className="flex items-center gap-3 mb-6">
        <button
          type="button"
          onClick={() => router.back()}
          className="tap w-10 h-10 rounded-full bg-white shadow-[0_3px_0_var(--lav)] flex items-center justify-center shrink-0"
          aria-label="Volver"
        >
          <ArrowLeft className="size-4" />
        </button>
        <div>
          <p className="text-[11px] font-extrabold uppercase" style={{ color: "var(--primary)" }}>Panel admin</p>
          <h1 className="font-brand text-2xl lg:text-3xl">Estadísticas</h1>
        </div>
      </div>

      <div className="rounded-[20px] border-2 border-dashed p-4 mb-5 bg-white" style={{ borderColor: "var(--border)" }}>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Label htmlFor="statistics-from" className="mb-1.5 text-[10px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Desde</Label>
            <Input
              id="statistics-from"
              type="date"
              value={from}
              max={todayKey}
              onChange={(event) => handleFromChange(event.target.value)}
              className="rounded-xl border-2 font-extrabold"
              style={{ borderColor: "var(--border)" }}
            />
          </div>
          <div>
            <Label htmlFor="statistics-to" className="mb-1.5 text-[10px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Hasta</Label>
            <Input
              id="statistics-to"
              type="date"
              value={to}
              max={todayKey}
              onChange={(event) => handleToChange(event.target.value)}
              className="rounded-xl border-2 font-extrabold"
              style={{ borderColor: "var(--border)" }}
              aria-invalid={invalidRange}
            />
          </div>
        </div>
        {invalidRange && (
          <p className="mt-2 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
            La fecha desde no puede ser posterior a la fecha hasta.
          </p>
        )}
        <div className="flex flex-wrap gap-2 mt-4">
          {presets.map((preset, index) => {
            // No explicit range yet (fresh load / cleared filters) defaults to
            // the last 7 days server-side (see StatisticsService.DEFAULT_RANGE),
            // so the first preset should read as selected in that state too.
            const isDefaultRange = !from && !to;
            const isActive = (from === preset.from && to === preset.to) || (isDefaultRange && index === 0);
            return (
              <Chip
                key={preset.label}
                as="button"
                active={isActive}
                onClick={() => !loading && applyPreset(preset.from, preset.to)}
                aria-pressed={isActive}
                aria-disabled={loading}
                className={loading ? "opacity-50 pointer-events-none" : undefined}
              >
                {preset.label}
              </Chip>
            );
          })}
        </div>
      </div>

      {error && <ErrorState message={error} retry={retry} />}
      {!error && loading && !statistics && <StatisticsSkeleton />}
      {!error && statistics && (
        <>
          <p className="text-[12px] font-extrabold mb-4 flex items-center gap-1.5" style={{ color: "var(--muted-foreground)" }}>
            🗓️ Período: {formatRangeLabel(statistics.from, statistics.to)}
          </p>

          <div className="flex items-center gap-1.5 mb-2.5">
            <span className="text-sm">📊</span>
            <span className="text-[11px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>Actividad</span>
            <span className="ml-auto text-[11px] font-extrabold" style={{ color: "var(--muted-foreground)" }}>{totalEvents} eventos</span>
          </div>

          {totalEvents > 0 ? (
            <section className="flex flex-wrap gap-3 mb-5">
              {ACTIVITY_METRICS.map(({ key, label, icon, tone }) => {
                const value = statistics.activities[key];
                const maxMetric = Math.max(
                  statistics.activities.created,
                  statistics.activities.rescheduled,
                  statistics.activities.cancelled,
                  statistics.activities.cancelledByWeather,
                  1,
                );
                return (
                  <div
                    key={key}
                    className="relative rounded-2xl p-3.5 min-w-[150px] flex-1"
                    style={{ background: `var(--${tone})`, boxShadow: "0 6px 16px -10px rgba(58,51,82,.3)" }}
                  >
                    <span className="emoji-3d absolute top-2.5 right-3 text-base" aria-hidden="true">{icon}</span>
                    <p className="font-display text-[26px] font-black leading-none" style={{ color: `var(--${tone}-ink)` }}>{value}</p>
                    <p className="mt-1.5 mb-2 text-[10.5px] font-black uppercase tracking-wide" style={{ color: `var(--${tone}-ink)`, opacity: 0.85 }}>{label}</p>
                    <div className="h-1.5 rounded-full overflow-hidden" style={{ background: "rgba(255,255,255,.55)" }} aria-hidden="true">
                      <div
                        className="h-full rounded-full"
                        style={{ width: `${Math.round((value / maxMetric) * 100)}%`, background: `var(--${tone}-ink)` }}
                      />
                    </div>
                  </div>
                );
              })}
            </section>
          ) : (
            <div className="rounded-[20px] border-2 border-dashed p-7 text-center mb-5 bg-white" style={{ borderColor: "var(--border)" }}>
              <span className="emoji-3d text-3xl" aria-hidden="true">🌤️</span>
              <p className="mt-2.5 text-[13px] font-extrabold" style={{ color: "var(--muted-foreground)" }}>No hay eventos de actividades <br />en el período seleccionado.</p>
            </div>
          )}

          <div
            className="relative rounded-tl-[4px] rounded-tr-[20px] rounded-br-[20px] rounded-bl-[20px] border-2 border-white p-4 pt-5"
            style={{
              background: "linear-gradient(160deg, #eaf6fd, var(--sky))",
              boxShadow: "0 8px 20px -10px rgba(58,51,82,.35)",
              transform: "rotate(-1deg)",
            }}
          >
            <div className="flex items-center gap-2 mb-1">
              <CloudSun className="emoji-3d size-5" style={{ color: "var(--sky-ink)" }} />
              <h2 className="font-display font-black text-base" style={{ color: "var(--sky-ink)" }}>Open-Meteo</h2>
            </div>
            <p className="text-[10.5px] font-extrabold mb-3.5" style={{ color: "var(--sky-ink)", opacity: 0.75 }}>Proveedor de clima</p>
            <dl className="flex flex-wrap gap-3.5">
              <div className="min-w-[45%] flex-1">
                <dt className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--sky-ink)", opacity: 0.7 }}>Consultas</dt>
                <dd className="mt-0.5 font-display text-xl font-black" style={{ color: "var(--sky-ink)" }}>{statistics.weatherProvider.requests}</dd>
              </div>
              <div className="min-w-[45%] flex-1">
                <dt className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--sky-ink)", opacity: 0.7 }}>Exitosas</dt>
                <dd className="mt-0.5 font-display text-xl font-black" style={{ color: "var(--sky-ink)" }}>{statistics.weatherProvider.successful}</dd>
              </div>
              <div className="min-w-[45%] flex-1">
                <dt className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--sky-ink)", opacity: 0.7 }}>Fallidas</dt>
                <dd className="mt-0.5 font-display text-xl font-black" style={{ color: "var(--sky-ink)" }}>{statistics.weatherProvider.failed}</dd>
              </div>
              <div className="min-w-[45%] flex-1">
                <dt className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--sky-ink)", opacity: 0.7 }}>Promedio</dt>
                <dd className="mt-0.5 font-display text-xl font-black" style={{ color: "var(--sky-ink)" }}>
                  {Math.round(statistics.weatherProvider.averageResponseTimeMs)} ms
                </dd>
              </div>
            </dl>
            {successRate !== null && (
              <div className="flex justify-center mt-4">
                <span
                  className="inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-[11px] font-black border-2 border-white"
                  style={{ background: "var(--sun)", color: "var(--sun-ink)", boxShadow: "0 3px 0 rgba(0,0,0,.12)", transform: "rotate(-1.5deg)" }}
                >
                  🎯 {successRate}% de éxito
                </span>
              </div>
            )}
          </div>
        </>
      )}
    </main>
  );
}

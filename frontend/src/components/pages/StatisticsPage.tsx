"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { ArrowLeft, CloudSun } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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

const ACTIVITY_METRICS: { key: keyof ActivityStatisticsResponse; label: string }[] = [
  { key: "created", label: "Creadas" },
  { key: "rescheduled", label: "Reprogramadas" },
  { key: "cancelled", label: "Canceladas" },
  { key: "cancelledByWeather", label: "Suspendidas por clima" },
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

  return (
    <main className="fade-in min-h-screen px-5 py-6 lg:mx-auto lg:max-w-5xl lg:px-10">
      <div className="flex items-center gap-3 mb-6">
        <Button variant="outline" size="icon" className="rounded-full" onClick={() => router.push("/mis-actividades")}>
          <ArrowLeft className="size-4" />
          <span className="sr-only">Volver</span>
        </Button>
        <div>
          <p className="text-[11px] font-extrabold uppercase" style={{ color: "var(--primary)" }}>Panel admin</p>
          <h1 className="font-brand text-2xl">Estadísticas</h1>
        </div>
      </div>

      <Card className="p-4 mb-5">
        <div className="grid grid-cols-2 gap-3 px-4">
          <div>
            <Label htmlFor="statistics-from" className="mb-2 text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Desde</Label>
            <Input id="statistics-from" type="date" value={from} max={todayKey} onChange={(event) => handleFromChange(event.target.value)} className="rounded-xl" />
          </div>
          <div>
            <Label htmlFor="statistics-to" className="mb-2 text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Hasta</Label>
            <Input id="statistics-to" type="date" value={to} max={todayKey} onChange={(event) => handleToChange(event.target.value)} className="rounded-xl" aria-invalid={invalidRange} />
          </div>
        </div>
        {invalidRange && (
          <p className="px-4 mt-2 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>
            La fecha desde no puede ser posterior a la fecha hasta.
          </p>
        )}
        <div className="flex flex-wrap gap-2 px-4 mt-4">
          {presets.map((preset) => (
            <Button
              key={preset.label}
              type="button"
              variant="outline"
              size="sm"
              className="rounded-xl"
              onClick={() => applyPreset(preset.from, preset.to)}
              disabled={loading}
              aria-pressed={from === preset.from && to === preset.to}
            >
              {preset.label}
            </Button>
          ))}
        </div>
      </Card>

      {error && <ErrorState message={error} retry={retry} />}
      {!error && loading && !statistics && <StatisticsSkeleton />}
      {!error && statistics && (
        <>
          <p className="text-[12px] font-bold mb-3" style={{ color: "var(--muted-foreground)" }}>
            Período: {formatRangeLabel(statistics.from, statistics.to)}
          </p>
          {statistics.activities.created +
              statistics.activities.rescheduled +
              statistics.activities.cancelled +
              statistics.activities.cancelledByWeather >
            0 ? (
            <>
              <section className="grid grid-cols-2 lg:grid-cols-4 gap-3 mb-4">
                {ACTIVITY_METRICS.map(({ key, label }) => {
                  const value = statistics.activities[key];
                  const maxMetric = Math.max(
                    statistics.activities.created,
                    statistics.activities.rescheduled,
                    statistics.activities.cancelled,
                    statistics.activities.cancelledByWeather,
                    1,
                  );
                  return (
                    <Card key={key} className="p-4">
                      <p className="text-2xl lg:text-3xl font-display font-semibold">{value}</p>
                      <p className="text-[11px] font-extrabold uppercase mt-0.5" style={{ color: "var(--muted-foreground)" }}>{label}</p>
                      <div
                        className="mt-2 h-1.5 rounded-full overflow-hidden"
                        style={{ background: "var(--secondary)" }}
                        aria-hidden="true"
                      >
                        <div
                          className="h-full rounded-full"
                          style={{ width: `${Math.round((value / maxMetric) * 100)}%`, background: "var(--primary)" }}
                        />
                      </div>
                    </Card>
                  );
                })}
              </section>
            </>
          ) : (
            <Card className="p-5 mb-4">
              <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--muted-foreground)" }}>
                No hay eventos de actividades en el período seleccionado.
              </p>
            </Card>
          )}
          <Card className="p-5">
            <div className="flex items-center gap-2 px-4">
              <CloudSun className="size-5" style={{ color: "var(--primary)" }} />
              <h2 className="font-display font-semibold text-lg">Open-Meteo</h2>
            </div>
            <dl className="grid grid-cols-2 gap-3 px-4 mt-3">
                  <div>
                    <dt className="text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Consultas</dt>
                    <dd className="mt-0.5 text-2xl font-display font-semibold">{statistics.weatherProvider.requests}</dd>
                  </div>
                  <div>
                    <dt className="text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Exitosas</dt>
                    <dd className="mt-0.5 text-2xl font-display font-semibold">{statistics.weatherProvider.successful}</dd>
                  </div>
                  <div>
                    <dt className="text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Fallidas</dt>
                    <dd className="mt-0.5 text-2xl font-display font-semibold">{statistics.weatherProvider.failed}</dd>
                  </div>
                  <div>
                    <dt className="text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Promedio</dt>
                    <dd className="mt-0.5 text-2xl font-display font-semibold">
                      {Math.round(statistics.weatherProvider.averageResponseTimeMs)} ms
                    </dd>
                  </div>
            </dl>
            {statistics.weatherProvider.requests > 0 && (
              <p className="px-4 mt-3 text-xs font-bold" style={{ color: "var(--muted-foreground)" }}>
                Tasa de éxito: {Math.round((statistics.weatherProvider.successful / statistics.weatherProvider.requests) * 100)}%
              </p>
            )}
          </Card>
        </>
      )}
    </main>
  );
}

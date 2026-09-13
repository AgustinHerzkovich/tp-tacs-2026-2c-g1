"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, CloudSun, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";
import { api } from "@/lib/api";
import type { StatisticsResponse } from "@/types/backend";

export function StatisticsPage() {
  const router = useRouter();
  const { initialized, isAuthenticated, hasRole } = useAuth();
  const [statistics, setStatistics] = useState<StatisticsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [appliedRange, setAppliedRange] = useState<{ from?: string; to?: string }>({});
  const [loading, setLoading] = useState(true);

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
      .get(appliedRange)
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
  }, [appliedRange, hasRole, initialized, isAuthenticated, router]);

  if (!initialized || !isAuthenticated || !hasRole("ADMIN")) return null;

  const activityMetrics = statistics
    ? [
        ["Creadas", statistics.activities.created],
        ["Reprogramadas", statistics.activities.rescheduled],
        ["Canceladas", statistics.activities.cancelled],
        ["Suspendidas por clima", statistics.activities.cancelledByWeather],
      ]
    : [];
  const invalidRange = Boolean(from && to && from > to);

  const applyRange = () => {
    if (invalidRange) return;
    setLoading(true);
    setError(null);
    setAppliedRange({
      from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
      to: to ? new Date(`${to}T23:59:59.999`).toISOString() : undefined,
    });
  };

  const clearRange = () => {
    setFrom("");
    setTo("");
    setLoading(true);
    setError(null);
    setAppliedRange({});
  };

  return (
    <main className="fade-in min-h-screen px-5 py-6">
      <div className="flex items-center gap-3 mb-6">
        <Button variant="outline" size="icon" className="rounded-full" onClick={() => router.push("/mis-actividades")}>
          <ArrowLeft className="size-4" />
          <span className="sr-only">Volver</span>
        </Button>
        <div>
          <p className="text-[11px] font-extrabold uppercase" style={{ color: "var(--primary)" }}>Panel admin</p>
          <h1 className="font-display font-semibold text-2xl">Estadísticas</h1>
        </div>
      </div>

      <Card className="p-4 rounded-2xl mb-5">
        <div className="grid grid-cols-2 gap-3 px-4">
          <div>
            <Label htmlFor="statistics-from" className="mb-2 text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Desde</Label>
            <Input id="statistics-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} className="rounded-xl" />
          </div>
          <div>
            <Label htmlFor="statistics-to" className="mb-2 text-[11px] font-extrabold uppercase" style={{ color: "var(--muted-foreground)" }}>Hasta</Label>
            <Input id="statistics-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} className="rounded-xl" aria-invalid={invalidRange} />
          </div>
        </div>
        {invalidRange && <p className="px-4 mt-2 text-xs font-extrabold" style={{ color: "var(--destructive)" }}>La fecha desde no puede ser posterior a la fecha hasta.</p>}
        <div className="flex gap-2 px-4 mt-4">
          <Button type="button" className="flex-1 rounded-xl" onClick={applyRange} disabled={invalidRange || loading}>Aplicar rango</Button>
          <Button type="button" variant="outline" className="rounded-xl" onClick={clearRange} disabled={loading && !from && !to}>Últimos 7 días</Button>
        </div>
      </Card>

      {error && (
        <Card className="p-4 rounded-2xl" style={{ color: "var(--destructive)" }}>{error}</Card>
      )}
      {!error && loading && !statistics && (
        <p className="text-center font-bold py-12" style={{ color: "var(--muted-foreground)" }}>Cargando…</p>
      )}
      {statistics && !loading && (
        <>
          <p className="text-[12px] font-bold mb-3" style={{ color: "var(--muted-foreground)" }}>
            Período: {statistics.from} al {statistics.to}
          </p>
          <section className="grid grid-cols-2 gap-3 mb-4">
            {activityMetrics.map(([label, value]) => (
              <Card key={label} className="p-4 rounded-2xl">
                <p className="text-3xl font-display font-semibold px-4">{value}</p>
                <p className="text-[11px] font-extrabold uppercase px-4" style={{ color: "var(--muted-foreground)" }}>{label}</p>
              </Card>
            ))}
          </section>
          <Card className="p-5 rounded-2xl">
            <div className="flex items-center gap-2 px-4">
              <CloudSun className="size-5" style={{ color: "var(--primary)" }} />
              <h2 className="font-display font-semibold text-lg">Open-Meteo</h2>
            </div>
            <div className="grid grid-cols-2 gap-3 px-4">
              <p><strong>{statistics.weatherProvider.requests}</strong><br /><span className="text-xs">consultas</span></p>
              <p><strong>{statistics.weatherProvider.successful}</strong><br /><span className="text-xs">exitosas</span></p>
              <p><strong>{statistics.weatherProvider.failed}</strong><br /><span className="text-xs">fallidas</span></p>
              <p className="flex items-start gap-1"><RefreshCw className="size-4 mt-0.5" /><span><strong>{Math.round(statistics.weatherProvider.averageResponseTimeMs)} ms</strong><br /><span className="text-xs">promedio</span></span></p>
            </div>
          </Card>
        </>
      )}
    </main>
  );
}

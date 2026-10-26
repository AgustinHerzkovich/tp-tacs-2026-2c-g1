import type { ProviderStatisticsResponse } from "@/types/backend";
import { formatLocalDate } from "@/lib/statisticsRange";

const PROVIDER_NAMES: Record<string, string> = {
  "Open-Meteo": "Proveedor principal",
  Legacy: "Proveedor alternativo",
};

const STATUS: Record<string, { label: string; bg: string; ink: string; bar: string }> = {
  OK: { label: "Funcionando", bg: "var(--mint)", ink: "var(--mint-ink)", bar: "#34d399" },
  DEGRADED: { label: "Degradado", bg: "var(--sun)", ink: "var(--sun-ink)", bar: "#fbbf24" },
  DOWN: { label: "Caído", bg: "var(--rose)", ink: "var(--rose-ink)", bar: "#fb7185" },
};

const NO_DATA = { label: "Sin datos", bg: "var(--muted)", ink: "var(--muted-foreground)", bar: "#cbd5e1" };

const BREAKER: Record<string, string> = {
  CLOSED: "Cerrado: deja pasar las consultas",
  OPEN: "Abierto: corta las consultas",
  HALF_OPEN: "Semiabierto: probando si se recuperó",
  FORCED_OPEN: "Abierto manualmente",
  DISABLED: "Desactivado",
};

const FAILURES: Record<string, string> = {
  TIMEOUT: "No respondió a tiempo",
  UNREACHABLE: "Caído o reiniciándose",
  HTTP_ERROR: "Error HTTP",
  MALFORMED_RESPONSE: "Respuesta ilegible",
  MISMATCHED_ID: "Respuesta de otro pedido",
  PROVIDER_ERROR: "Error informado por el proveedor",
  REJECTED_REQUEST: "Pedido rechazado",
  INVALID_RESULT: "Datos imposibles o incompletos",
  CIRCUIT_OPEN: "Circuito abierto",
  OVERLOADED: "Sin capacidad (contrapresión)",
  RATE_LIMITED: "Límite de la plataforma",
  USER_RATE_LIMITED: "Límite por usuario",
  ERROR: "Error",
  REFUSED: "Rechazada",
};

function statusOf(status: string | null) {
  return (status && STATUS[status]) || NO_DATA;
}

function Counts({ title, counts }: { title: string; counts: Record<string, number> }) {
  const entries = Object.entries(counts).sort((a, b) => b[1] - a[1]);
  if (entries.length === 0) return null;
  return (
    <div className="mt-3">
      <p className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
        {title}
      </p>
      <ul className="mt-1 space-y-0.5">
        {entries.map(([kind, count]) => (
          <li key={kind} className="flex justify-between gap-3 text-[12px] font-bold">
            <span>{FAILURES[kind] ?? kind}</span>
            <span className="tabular-nums">{count}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Usage and health of one weather provider for the administrator panel:
 * current status, circuit breaker state, cache hit ratio, calls and errors,
 * and a strip with its status over the selected period. */
export function ProviderHealthCard({ provider }: { provider: ProviderStatisticsResponse }) {
  const current = statusOf(provider.status);
  const name = PROVIDER_NAMES[provider.provider] ?? provider.provider;
  const hitRatio = provider.cacheHitRatio === null ? "—" : `${Math.round(provider.cacheHitRatio * 100)}%`;

  return (
    <section
      aria-label={name}
      className="rounded-3xl border-[3px] border-white p-4 shadow-[0_8px_18px_-10px_rgba(58,51,82,.3)]"
      style={{ background: "var(--card)" }}
    >
      <div className="flex items-center justify-between gap-3">
        <h3 className="font-display text-base font-black">{name}</h3>
        <span
          className="rounded-full px-2.5 py-1 text-[11px] font-black"
          style={{ background: current.bg, color: current.ink }}
        >
          {current.label}
        </span>
      </div>
      <p className="mt-1 text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
        Circuit breaker: {provider.circuitBreakerState ? BREAKER[provider.circuitBreakerState] ?? provider.circuitBreakerState : "sin datos"}
      </p>

      <dl className="mt-3 grid grid-cols-3 gap-2.5">
        {[
          ["Consultas", provider.requests],
          ["Exitosas", provider.successful],
          ["Fallidas", provider.failed],
          ["Promedio", `${Math.round(provider.averageResponseTimeMs)} ms`],
          ["Hit ratio caché", hitRatio],
          ["Respaldos", provider.fallbacks],
        ].map(([label, value]) => (
          <div key={label}>
            <dt className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
              {label}
            </dt>
            <dd className="font-display text-lg font-black tabular-nums">{value}</dd>
          </div>
        ))}
      </dl>

      <Counts title="Errores" counts={provider.errors} />
      <Counts title="Consultas cortadas antes de llegar" counts={provider.refusals} />

      <div className="mt-3">
        <p className="text-[9.5px] font-black uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
          Estado en el tiempo
        </p>
        {provider.timeline.length === 0 ? (
          <p className="mt-1 text-[12px] font-bold" style={{ color: "var(--muted-foreground)" }}>
            Sin mediciones en el período.
          </p>
        ) : (
          <>
            <div role="img" aria-label={`Estado de ${name} en el período`} className="mt-1.5 flex h-6 gap-px overflow-hidden rounded-md">
              {provider.timeline.map((point) => (
                <span
                  key={point.at}
                  title={`${formatLocalDate(point.at)} ${new Date(point.at).toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" })} · ${statusOf(point.status).label}`}
                  className="min-w-[2px] flex-1"
                  style={{ background: statusOf(point.status).bar }}
                />
              ))}
            </div>
            <div className="mt-1 flex justify-between text-[10px] font-bold" style={{ color: "var(--muted-foreground)" }}>
              <span>{formatLocalDate(provider.timeline[0]!.at)}</span>
              <span>{formatLocalDate(provider.timeline[provider.timeline.length - 1]!.at)}</span>
            </div>
          </>
        )}
      </div>
    </section>
  );
}

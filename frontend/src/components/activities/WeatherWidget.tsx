import { Skeleton } from "@/components/ui/skeleton";
import { formatWeatherLimits, isWithinWeatherLimits } from "@/lib/weatherLimits";
import type { WeatherConditionsDTO, WeatherForecastDTO } from "@/types/backend";

interface WeatherWidgetProps {
  loading: boolean;
  /** True when the backend couldn't get a forecast — never render a
   * "good weather" guess in this case (see useActivityWeather). This flag
   * also covers "the activity is too far in the future to have a forecast
   * yet", since the hook can't currently tell the two cases apart. */
  unavailable: boolean;
  forecast: WeatherForecastDTO | null;
  current?: WeatherForecastDTO | null;
  /** The organizer-configured weather limits, used only for the "within
   * limits" hint below the forecast — informational for the viewer, never
   * the source of truth (the backend scheduler decides whether to open a
   * reprogramming votation). */
  conditions: WeatherConditionsDTO;
  /** Forces the "exceeds limits" visual even if this widget's own forecast
   * fetch hasn't caught up yet. A reprogramming votation only opens once the
   * backend scheduler already found bad weather, so that signal is more
   * trustworthy than re-deriving it from this component's own (separate)
   * weather fetch. */
  forcedExceeded?: boolean;
}

const SHELL = "relative rounded-[26px] border-[3px] border-white p-4 mb-6 shadow-[0_10px_22px_-10px_rgba(58,51,82,.32)] -rotate-1";

export function WeatherWidget({ loading, unavailable, forecast, current, conditions, forcedExceeded }: WeatherWidgetProps) {
  if (loading) {
    return (
      <div className={`${SHELL} bg-muted`} aria-busy="true">
        <span className="sr-only" role="status">Cargando pronóstico</span>
        <Skeleton className="h-2.5 w-32 mb-3" />
        <Skeleton className="h-9 w-28 mb-2" />
        <Skeleton className="h-2.5 w-44" />
      </div>
    );
  }

  if (unavailable || !forecast) {
    return (
      <div className={SHELL} style={{ background: "linear-gradient(160deg,#F1F5F9,#E2E8F0)" }}>
        <p className="text-[11px] font-extrabold uppercase tracking-wide" style={{ color: "#94a3b8" }}>
          Pronóstico para el día
        </p>
        <div className="flex items-baseline gap-2.5 mt-1" style={{ filter: "blur(.3px)" }}>
          <span className="text-[34px] font-black" style={{ color: "#cbd5e1" }}>--°</span>
          <span className="text-[13px] font-extrabold" style={{ color: "#cbd5e1" }}>--% · -- km/h</span>
        </div>
        <p className="text-[11px] font-bold mt-1" style={{ color: "#cbd5e1" }}>Ahora: --°C · --% lluvia · -- km/h</p>
        <div
          className="absolute left-1/2 top-1/2 max-w-[88%] -translate-x-1/2 -translate-y-1/2 rotate-[-7deg] rounded-2xl border-[3px] border-white px-4 py-2.5 text-center shadow-[0_6px_14px_-4px_rgba(58,51,82,.25)]"
          style={{ background: "var(--sun)", color: "var(--sun-ink)" }}
        >
          <span className="text-[11.5px] font-black leading-tight">⏳ Todavía es pronto para el pronóstico</span>
        </div>
      </div>
    );
  }

  const exceeded = forcedExceeded || !isWithinWeatherLimits(forecast, conditions);
  const ink = exceeded ? "var(--rose-ink)" : "var(--sky-ink)";
  const chip = exceeded
    ? { bg: "var(--destructive)", ink: "#fff", label: "⚠️ Supera lo permitido" }
    : { bg: "var(--mint)", ink: "var(--mint-ink)", label: "✓ Dentro de lo permitido" };

  return (
    <div className={SHELL} style={{ background: exceeded ? "linear-gradient(160deg,#FFE4E9,var(--rose))" : "linear-gradient(160deg,#E9F8FF,var(--sky))" }}>
      <span className="emoji-sticker absolute -top-4 right-5 text-[26px]" style={{ transform: "rotate(10deg)" }}>
        {exceeded ? "🌧️" : "⛅️"}
      </span>
      <p className="text-[11px] font-extrabold uppercase tracking-wide" style={{ color: ink }}>
        Pronóstico para el día
      </p>
      <div className="flex items-baseline gap-2.5 mt-1">
        <span className="text-[34px] font-black" style={{ color: ink }}>{Math.round(forecast.temperature)}°C</span>
        <span className="text-[13px] font-extrabold" style={{ color: ink }}>
          💧{Math.round(forecast.chanceOfRain)}% · 🍃{Math.round(forecast.windSpeed)} km/h
        </span>
      </div>
      {current && (
        <p className="text-[11px] font-bold mt-1 opacity-80" style={{ color: ink }}>
          Ahora: {Math.round(current.temperature)}°C · {Math.round(current.chanceOfRain)}% lluvia · {Math.round(current.windSpeed)} km/h
        </p>
      )}
      <div
        className="flex flex-wrap items-center gap-2 mt-3 pt-2.5 border-t-2 border-dashed"
        style={{ borderColor: exceeded ? "rgba(190,18,60,.3)" : "rgba(3,105,161,.3)" }}
      >
        <span
          className="inline-flex items-center gap-1 rounded-full border-2 border-white px-2.5 py-1 text-[11px] font-black whitespace-nowrap"
          style={{ background: chip.bg, color: chip.ink }}
        >
          {chip.label}
        </span>
        <span className="text-[10.5px] font-bold opacity-85" style={{ color: ink }}>
          {formatWeatherLimits(conditions)}
        </span>
      </div>
    </div>
  );
}

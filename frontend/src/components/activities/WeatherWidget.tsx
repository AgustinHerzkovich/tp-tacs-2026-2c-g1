import { Card } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import type { WeatherForecastDTO } from "@/types/backend";

interface WeatherWidgetProps {
  loading: boolean;
  /** True when the backend couldn't get a forecast — never render a
   * "good weather" guess in this case (see useActivityWeather). */
  unavailable: boolean;
  forecast: WeatherForecastDTO | null;
  current?: WeatherForecastDTO | null;
}

export function WeatherWidget({ loading, unavailable, forecast, current }: WeatherWidgetProps) {
  return (
    <Card className="p-4 mb-4 rounded-2xl">
      <p className="font-display font-semibold text-[11.5px] tracking-wide px-4 mb-3" style={{ color: "var(--muted-foreground)" }}>
        PRONÓSTICO PARA LA ACTIVIDAD
      </p>
      {loading && (
        <div className="grid grid-cols-4 gap-1 px-4" aria-busy="true" aria-label="Cargando pronóstico">
          <span className="sr-only" role="status">Cargando pronóstico</span>
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="flex flex-col items-center gap-1.5">
              <Skeleton className="size-8 rounded-full" />
              <Skeleton className="h-3 w-10" />
              <Skeleton className="h-2.5 w-8" />
            </div>
          ))}
        </div>
      )}
      {!loading && (unavailable || !forecast) && (
        <p className="text-center text-[12.5px] font-bold px-4 py-2" style={{ color: "var(--muted-foreground)" }}>
          Pronóstico no disponible por ahora.
        </p>
      )}
      {!loading && !unavailable && forecast && (
        <div>
        {current && <p className="text-[11px] font-bold px-4 mb-3" style={{ color: "var(--muted-foreground)" }}>Ahora: {Math.round(current.temperature)}°C · lluvia {Math.round(current.chanceOfRain)}% · viento {Math.round(current.windSpeed)} km/h</p>}
        <div className="grid grid-cols-4 gap-1 text-center px-4">
          {[
            { icon: "⛅", label: "Clima", value: forecast.chanceOfRain > 50 ? "Lluvia" : "Templado" },
            { icon: "🌡️", label: "Temp.", value: `${Math.round(forecast.temperature)}°C` },
            { icon: "☔", label: "Lluvia", value: `${Math.round(forecast.chanceOfRain)}%` },
            { icon: "💨", label: "Viento", value: `${Math.round(forecast.windSpeed)} km/h` },
          ].map((s, i) => (
            <div key={s.label}>
              <div className="text-2xl emoji-3d floaty" style={{ animationDelay: `${i * 0.25}s` }}>
                {s.icon}
              </div>
              <p className="font-display font-semibold text-[12.5px] tabular-nums mt-1">{s.value}</p>
              <p className="text-[9.5px] font-extrabold" style={{ color: "var(--muted-foreground)" }}>
                {s.label}
              </p>
            </div>
          ))}
        </div>
        </div>
      )}
    </Card>
  );
}

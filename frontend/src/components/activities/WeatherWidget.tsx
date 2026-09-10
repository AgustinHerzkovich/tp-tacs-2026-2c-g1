import { Card } from "@/components/ui/card";
import type { WeatherForecastDTO } from "@/types/backend";

interface WeatherWidgetProps {
  loading: boolean;
  /** True when the backend couldn't get a forecast — never render a
   * "good weather" guess in this case (see useActivityWeather). */
  unavailable: boolean;
  forecast: WeatherForecastDTO | null;
}

export function WeatherWidget({ loading, unavailable, forecast }: WeatherWidgetProps) {
  return (
    <Card className="p-4 mb-4 rounded-2xl">
      <p className="font-display font-semibold text-[11.5px] tracking-wide px-4 mb-3" style={{ color: "var(--muted-foreground)" }}>
        PRONÓSTICO PARA LA ACTIVIDAD
      </p>
      {loading && (
        <p className="text-center text-[12.5px] font-bold px-4 py-2" style={{ color: "var(--muted-foreground)" }}>
          Cargando…
        </p>
      )}
      {!loading && (unavailable || !forecast) && (
        <p className="text-center text-[12.5px] font-bold px-4 py-2" style={{ color: "var(--muted-foreground)" }}>
          Pronóstico no disponible por ahora.
        </p>
      )}
      {!loading && !unavailable && forecast && (
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
      )}
    </Card>
  );
}

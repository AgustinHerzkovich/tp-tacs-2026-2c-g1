import { Card } from "@/components/ui/card";
import type { WeatherForecastDTO } from "@/types/backend";

interface WeatherWidgetProps {
  loading: boolean;
  unavailable: boolean;
  forecast: WeatherForecastDTO | null;
}

export function WeatherWidget({ loading, unavailable, forecast }: WeatherWidgetProps) {
  const rain = forecast?.chanceOfRain ?? null;
  const badRain = rain !== null && rain >= 50;

  const stats = [
    { icon: badRain ? "🌧️" : "⛅", label: "Clima", value: rain !== null ? (badRain ? "Lluvia" : "Templado") : "—" },
    { icon: "🌡️", label: "Temp.", value: forecast ? `${Math.round(forecast.temperature)}°C` : "—" },
    { icon: "☔", label: "Lluvia", value: rain !== null ? `${Math.round(rain)}%` : "—" },
    { icon: "💨", label: "Viento", value: forecast ? `${Math.round(forecast.windSpeed)} km/h` : "—" },
  ];

  return (
    <Card className="p-4 mb-4 rounded-2xl">
      <p className="font-display font-semibold text-[11.5px] tracking-wide px-4 mb-3" style={{ color: "var(--muted-foreground)" }}>
        PRONÓSTICO PARA LA ACTIVIDAD
      </p>
      {loading && (
        <p className="text-[12.5px] font-bold px-4" style={{ color: "var(--muted-foreground)" }}>
          Cargando pronóstico…
        </p>
      )}
      {!loading && unavailable && (
        <p className="text-[12.5px] font-bold px-4" style={{ color: "var(--muted-foreground)" }}>
          Pronóstico no disponible por ahora.
        </p>
      )}
      {!loading && !unavailable && (
        <div className="grid grid-cols-4 gap-1 text-center px-4">
          {stats.map((s, i) => (
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

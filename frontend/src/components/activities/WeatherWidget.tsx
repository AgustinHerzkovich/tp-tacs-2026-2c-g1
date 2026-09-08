import { Card } from "@/components/ui/card";

export function WeatherWidget({ badWeather }: { badWeather: boolean }) {
  const stats = [
    { icon: "⛅", label: "Clima", value: badWeather ? "Lluvia" : "Templado" },
    { icon: "🌡️", label: "Temp.", value: badWeather ? "22°C" : "19°C" },
    { icon: "☔", label: "Lluvia", value: badWeather ? "70%" : "20%" },
    { icon: "💨", label: "Viento", value: badWeather ? "38 km/h" : "12 km/h" },
  ];

  return (
    <Card className="p-4 mb-4 rounded-2xl">
      <p className="font-display font-semibold text-[11.5px] tracking-wide px-4 mb-3" style={{ color: "var(--muted-foreground)" }}>
        PRONÓSTICO PARA LA ACTIVIDAD
      </p>
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
    </Card>
  );
}

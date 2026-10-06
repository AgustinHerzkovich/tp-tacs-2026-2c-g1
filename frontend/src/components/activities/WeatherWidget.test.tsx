import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { WeatherWidget } from "@/components/activities/WeatherWidget";
import type { WeatherConditionsDTO, WeatherForecastDTO } from "@/types/backend";

const FORECAST: WeatherForecastDTO = {
  dateTime: "2026-09-20T14:00:00",
  temperature: 22,
  chanceOfRain: 40,
  windSpeed: 15,
};

const CONDITIONS: WeatherConditionsDTO = {
  maxRainProbability: 60,
  minTemperature: 5,
  maxTemperature: 30,
  maxWindSpeed: 40,
};

describe("WeatherWidget", () => {
  it("shows a skeleton while the forecast is loading", () => {
    render(<WeatherWidget loading unavailable={false} forecast={null} conditions={CONDITIONS} />);
    expect(screen.getByRole("status")).toHaveTextContent("Cargando pronóstico");
  });

  it("blames the forecast when it's unavailable for an upcoming activity", () => {
    render(<WeatherWidget loading={false} unavailable forecast={null} conditions={CONDITIONS} />);
    expect(screen.getByText(/Todavía es pronto para el pronóstico/)).toBeInTheDocument();
  });

  it("says the activity already happened when its date-time is past", () => {
    render(<WeatherWidget loading={false} unavailable forecast={null} conditions={CONDITIONS} expired />);
    expect(screen.getByText(/Esta actividad ya pasó/)).toBeInTheDocument();
    expect(screen.queryByText(/Todavía es pronto/)).not.toBeInTheDocument();
  });

  it("never shows the unavailable badge as good weather", () => {
    render(<WeatherWidget loading={false} unavailable={false} forecast={null} conditions={CONDITIONS} />);
    expect(screen.getByText("--°")).toBeInTheDocument();
    expect(screen.queryByText(/Dentro de lo permitido/)).not.toBeInTheDocument();
  });

  it("renders the forecast and the within-limits chip when the provider answers", () => {
    render(<WeatherWidget loading={false} unavailable={false} forecast={FORECAST} conditions={CONDITIONS} />);
    expect(screen.getByText("22°C")).toBeInTheDocument();
    expect(screen.getByText("✓ Dentro de lo permitido")).toBeInTheDocument();
  });

  it("forces the exceeded visual when a votation is already open", () => {
    render(
      <WeatherWidget loading={false} unavailable={false} forecast={FORECAST} conditions={CONDITIONS} forcedExceeded />,
    );
    expect(screen.getByText("⚠️ Supera lo permitido")).toBeInTheDocument();
  });
});

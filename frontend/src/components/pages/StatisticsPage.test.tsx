import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { StatisticsPage } from "@/components/pages/StatisticsPage";
import type { StatisticsResponse } from "@/types/backend";

const { statisticsGet, authState, replace, push, back } = vi.hoisted(() => ({
  statisticsGet: vi.fn(),
  replace: vi.fn(),
  push: vi.fn(),
  back: vi.fn(),
  authState: {
    initialized: true,
    isAuthenticated: true,
    hasRole: (role: string): boolean => false,
  },
}));

vi.mock("next/navigation", () => {
  const router = { replace, push, back };
  const searchParams = new URLSearchParams();
  return {
    useRouter: () => router,
    useSearchParams: () => searchParams,
  };
});

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => authState,
}));

vi.mock("@/lib/api", () => ({
  api: {
    statistics: { get: statisticsGet },
  },
}));

const STATS: StatisticsResponse = {
  from: "2026-09-01T00:00:00.000Z",
  to: "2026-09-07T23:59:59.999Z",
  activities: { created: 10, rescheduled: 3, cancelled: 2, cancelledByWeather: 1 },
  weatherProvider: { requests: 100, successful: 95, failed: 5, averageResponseTimeMs: 240 },
};

const EMPTY_STATS: StatisticsResponse = {
  ...STATS,
  activities: { created: 0, rescheduled: 0, cancelled: 0, cancelledByWeather: 0 },
};

beforeEach(() => {
  replace.mockReset();
  push.mockReset();
  back.mockReset();
  statisticsGet.mockReset();
  statisticsGet.mockResolvedValue(STATS);
  authState.initialized = true;
  authState.isAuthenticated = true;
  authState.hasRole = () => false;
});

describe("StatisticsPage authorization", () => {
  it("redirects away when the user is not an ADMIN", () => {
    render(<StatisticsPage />);
    expect(replace).toHaveBeenCalledWith("/mis-actividades");
  });

  it("redirects to login when there is no session", () => {
    authState.isAuthenticated = false;
    render(<StatisticsPage />);
    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("waits for the SSO check before deciding", () => {
    authState.initialized = false;
    render(<StatisticsPage />);
    expect(replace).not.toHaveBeenCalled();
    expect(screen.getByText("Comprobando sesión...")).toBeInTheDocument();
  });
});

describe("StatisticsPage range filtering", () => {
  beforeEach(() => {
    authState.hasRole = () => true;
  });

  it("requests the range defaulting to no boundaries and renders the metrics", async () => {
    render(<StatisticsPage />);

    expect(await screen.findByText("Creadas")).toBeInTheDocument();
    expect(screen.getByText("Reprogramadas")).toBeInTheDocument();
    expect(screen.getByText("Canceladas")).toBeInTheDocument();
    expect(screen.getByText("Suspendidas por clima")).toBeInTheDocument();
    expect(screen.getByText("Open-Meteo")).toBeInTheDocument();
    expect(screen.getByText(/95% de éxito/)).toBeInTheDocument();
    expect(screen.getByText(/Período:/)).toBeInTheDocument();

    // With no from/to in the URL the API is invoked without range boundaries;
    // only the presets or a manual selection bound the query.
    const call = statisticsGet.mock.calls[0]?.[0] as { from?: string; to?: string } | undefined;
    expect(call?.from).toBeUndefined();
    expect(call?.to).toBeUndefined();
  });

  it("shows an empty state when no events are registered in the range", async () => {
    statisticsGet.mockResolvedValue(EMPTY_STATS);
    render(<StatisticsPage />);
    expect(
      await screen.findByText(/No hay eventos de actividades en el período seleccionado/),
    ).toBeInTheDocument();
  });

  it("surfaces API failures and allows a retry", async () => {
    statisticsGet.mockRejectedValueOnce(new Error("No pudimos cargar las estadísticas."));
    const user = userEvent.setup();
    render(<StatisticsPage />);

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("No pudimos cargar las estadísticas.");

    const callsBeforeRetry = statisticsGet.mock.calls.length;
    await user.click(screen.getByRole("button", { name: "Reintentar" }));
    expect(await screen.findByText("Creadas")).toBeInTheDocument();
    expect(statisticsGet.mock.calls).toHaveLength(callsBeforeRetry + 1);
  });

  it("applies a preset range and preserves it in the URL", async () => {
    const user = userEvent.setup();
    render(<StatisticsPage />);

    await user.click(screen.getByRole("button", { name: "Últimos 7 días" }));
    expect(replace).toHaveBeenCalledWith(
      expect.stringMatching(/^\/estadisticas\?from=\d{4}-\d{2}-\d{2}&to=\d{4}-\d{2}-\d{2}$/),
      { scroll: false },
    );
  });

  it("rejects a range whose from is later than to", async () => {
    const user = userEvent.setup();
    render(<StatisticsPage />);
    await screen.findByText("Creadas");

    await user.clear(screen.getByLabelText("Desde"));
    await user.type(screen.getByLabelText("Desde"), "2026-09-10");
    await user.clear(screen.getByLabelText("Hasta"));
    await user.type(screen.getByLabelText("Hasta"), "2026-09-05");

    expect(screen.getByText(/La fecha desde no puede ser posterior a la fecha hasta/)).toBeInTheDocument();
  });
});

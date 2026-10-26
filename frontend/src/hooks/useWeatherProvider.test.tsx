import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useWeatherProvider } from "@/hooks/useWeatherProvider";

const { getPreferences, updatePreferences } = vi.hoisted(() => ({
  getPreferences: vi.fn(),
  updatePreferences: vi.fn(),
}));

vi.mock("@/lib/api", () => ({
  api: { users: { getPreferences, updatePreferences } },
}));

describe("useWeatherProvider", () => {
  it("loads the provider in effect for the user", async () => {
    getPreferences.mockResolvedValueOnce({ weatherProvider: "OPEN_METEO" });

    const { result } = renderHook(() => useWeatherProvider());

    await waitFor(() => expect(result.current.provider).toBe("OPEN_METEO"));
  });

  it("does not ask the backend while disabled", () => {
    getPreferences.mockClear();

    renderHook(() => useWeatherProvider(false));

    expect(getPreferences).not.toHaveBeenCalled();
  });

  it("stores the chosen provider and shows what the backend saved", async () => {
    getPreferences.mockResolvedValueOnce({ weatherProvider: "OPEN_METEO" });
    updatePreferences.mockResolvedValueOnce({ weatherProvider: "LEGACY" });
    const { result } = renderHook(() => useWeatherProvider());
    await waitFor(() => expect(result.current.provider).toBe("OPEN_METEO"));

    await act(() => result.current.choose("LEGACY"));

    expect(updatePreferences).toHaveBeenCalledWith({ weatherProvider: "LEGACY" });
    expect(result.current.provider).toBe("LEGACY");
  });

  it("keeps the previous choice and reports the error when saving fails", async () => {
    getPreferences.mockResolvedValueOnce({ weatherProvider: "OPEN_METEO" });
    updatePreferences.mockRejectedValueOnce(new Error("No pudimos guardar tu configuración."));
    const { result } = renderHook(() => useWeatherProvider());
    await waitFor(() => expect(result.current.provider).toBe("OPEN_METEO"));

    await act(() => result.current.choose("LEGACY"));

    expect(result.current.provider).toBe("OPEN_METEO");
    expect(result.current.error).toBe("No pudimos guardar tu configuración.");
  });
});

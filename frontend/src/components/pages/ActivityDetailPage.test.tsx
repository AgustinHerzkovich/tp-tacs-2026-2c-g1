import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ActivityDetailPage } from "@/components/pages/ActivityDetailPage";
import type { ActivityResponse } from "@/types/backend";
import type { UseVoting } from "@/hooks/useVoting";

const { join, leave, activityRefresh, weatherRefresh, votingRefresh, router, toast } = vi.hoisted(() => ({
  join: vi.fn(),
  leave: vi.fn(),
  activityRefresh: vi.fn(),
  weatherRefresh: vi.fn(),
  votingRefresh: vi.fn(),
  router: { back: vi.fn(), push: vi.fn() },
  toast: vi.fn(),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

vi.mock("@/lib/api", () => ({
  api: {
    activities: { join, leave },
    votations: { mine: vi.fn().mockResolvedValue({ content: [] }), vote: vi.fn(), updateOptions: vi.fn(), updateSettings: vi.fn() },
  },
}));

vi.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ user: { id: "user-1", name: "Ana" } }),
}));

vi.mock("@/components/common/ToastProvider", () => ({
  useToast: () => toast,
}));

vi.mock("@/hooks/useActivity", () => ({
  useActivity: () => ({ activity: ACTIVITY, loading: false, notFound: false, error: null, refresh: activityRefresh }),
}));

vi.mock("@/hooks/useActivityWeather", () => ({
  useActivityWeather: () => ({ weather: null, loading: false, unavailable: true, refresh: weatherRefresh }),
}));

vi.mock("@/hooks/useVoting", () => ({
  useVoting: () => VOTING,
}));

// The heavy presentational pieces are not what this page test is about: the
// forecast and votation widgets are covered on their own, and the page only has
// to decide *when* they are re-read.
vi.mock("@/components/activities/ActivityGallery", () => ({ ActivityGallery: () => null }));
vi.mock("@/components/activities/WeatherWidget", () => ({ WeatherWidget: () => null }));
vi.mock("@/components/activities/ActivityAboutCard", () => ({ ActivityAboutCard: () => null }));
vi.mock("@/components/activities/ActivityRequirementsCard", () => ({ ActivityRequirementsCard: () => null }));
vi.mock("@/components/activities/VotingRoom", () => ({ VotingRoom: () => null }));
vi.mock("@/components/common/AvatarStack", () => ({ AvatarStack: () => null }));

const ACTIVITY: ActivityResponse = {
  id: "a1",
  title: "Vóley",
  description: null,
  type: "OUTDOOR",
  location: { city: "CABA", latitude: null, longitude: null },
  dateTime: "2026-09-20T14:00:00",
  timeZone: "America/Argentina/Buenos_Aires",
  availability: true,
  minParticipants: 4,
  maxParticipants: 12,
  participantCount: 2,
  participants: [],
  weatherConditions: { maxRainProbability: null, minTemperature: null, maxTemperature: null, maxWindSpeed: null },
  anticipationWindow: 24,
  reprogramationRange: { maxDays: 3, initialHour: "09:00:00", finalHour: "21:00:00" },
  status: "CONFIRMED",
  imageUrls: [],
  organizerId: "organizer-1",
};

const VOTING = {
  votation: null,
  loading: false,
  error: null,
  options: [],
  total: 0,
  selectedId: null,
  votedId: null,
  selectedOption: undefined,
  confirmOpen: false,
  pending: false,
  select: vi.fn(),
  requestVote: vi.fn(),
  cancelVote: vi.fn(),
  confirmVote: vi.fn(),
  updateOptions: vi.fn(),
  updateSettings: vi.fn(),
  refresh: votingRefresh,
} as unknown as UseVoting;

beforeEach(() => {
  vi.clearAllMocks();
  join.mockResolvedValue(undefined);
  leave.mockResolvedValue(undefined);
  // Shared fixture: the leave case adds the user as a participant, so every
  // test starts from "not joined" again.
  ACTIVITY.participants = [];
});

describe("ActivityDetailPage membership", () => {
  it("re-reads the activity, the forecast and the votation after joining", async () => {
    const user = userEvent.setup();
    render(<ActivityDetailPage id="a1" />);

    await user.click(screen.getByRole("button", { name: "Sumarme a la actividad" }));
    await user.click(screen.getByRole("button", { name: "Sí, sumarme" }));

    expect(join).toHaveBeenCalledWith("a1");
    // The forecast endpoint answers 403 to a non-participant and the votations
    // list only covers activities the user takes part in, so both have to be
    // requested again or the page keeps the "not allowed yet" state.
    expect(activityRefresh).toHaveBeenCalledTimes(1);
    expect(weatherRefresh).toHaveBeenCalledTimes(1);
    expect(votingRefresh).toHaveBeenCalledTimes(1);
  });

  it("re-reads everything after leaving as well", async () => {
    ACTIVITY.participants = [{ userId: "user-1", name: "Ana" }];
    const user = userEvent.setup();
    render(<ActivityDetailPage id="a1" />);

    await user.click(screen.getByRole("button", { name: "Bajarme de la actividad" }));
    await user.click(screen.getByRole("button", { name: "Sí, bajarme" }));

    expect(leave).toHaveBeenCalledWith("a1");
    expect(activityRefresh).toHaveBeenCalledTimes(1);
    expect(weatherRefresh).toHaveBeenCalledTimes(1);
    expect(votingRefresh).toHaveBeenCalledTimes(1);
  });

  it("reads nothing again when the join is cancelled", async () => {
    const user = userEvent.setup();
    render(<ActivityDetailPage id="a1" />);

    await user.click(screen.getByRole("button", { name: "Sumarme a la actividad" }));
    await user.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(join).not.toHaveBeenCalled();
    expect(activityRefresh).not.toHaveBeenCalled();
    expect(weatherRefresh).not.toHaveBeenCalled();
    expect(votingRefresh).not.toHaveBeenCalled();
  });
});
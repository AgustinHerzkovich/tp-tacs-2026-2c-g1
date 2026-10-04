import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { VotingRoom } from "@/components/activities/VotingRoom";
import type { UseVoting } from "@/hooks/useVoting";
import type { VotationDTO } from "@/types/backend";

const VOTATION: VotationDTO = {
  id: "v1",
  activityId: "a1",
  creationDate: "2026-09-01T10:00:00",
  status: "ACTIVE",
  options: [
    { dateTime: "2026-09-20T18:00:00", voteCount: 3, voterNames: ["A", "B", "C"] },
    { dateTime: "2026-09-21T18:00:00", voteCount: 1, voterNames: ["D"] },
  ],
  closingDate: "2026-09-19T18:00:00",
  minQuorum: 0.5,
  votedOption: null,
};

function fakeVoting(overrides: Partial<UseVoting> = {}): UseVoting {
  return {
    votation: VOTATION,
    loading: false,
    error: null,
    options: VOTATION.options.map((option) => ({
      id: option.dateTime,
      label: option.dateTime,
      votes: option.voteCount,
    })),
    total: 4,
    selectedId: null,
    votedId: null,
    selectedOption: undefined,
    confirmOpen: false,
    pending: false,
    select: vi.fn(),
    requestVote: vi.fn(),
    cancelVote: vi.fn(),
    confirmVote: vi.fn(),
    updateOptions: vi.fn().mockResolvedValue(undefined),
    updateSettings: vi.fn().mockResolvedValue(undefined),
    ...overrides,
  };
}

describe("VotingRoom", () => {
  it("hides the administration entry from participants", () => {
    render(<VotingRoom voting={fakeVoting()} />);
    expect(screen.queryByRole("button", { name: /Administrar votación/ })).not.toBeInTheDocument();
  });

  it("offers the administration entry to the organizer", async () => {
    const user = userEvent.setup();
    render(<VotingRoom voting={fakeVoting()} organizer />);
    await user.click(screen.getByRole("button", { name: /Administrar votación/ }));
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });
});

describe("VotationAdminDialog", () => {
  async function openDialog(voting: UseVoting) {
    const user = userEvent.setup();
    render(<VotingRoom voting={voting} organizer />);
    await user.click(screen.getByRole("button", { name: /Administrar votación/ }));
    return user;
  }

  it("seeds the form with the votation's current settings and options", async () => {
    await openDialog(fakeVoting());
    const dialog = screen.getByRole("dialog");

    expect(within(dialog).getByLabelText(/Quórum/)).toHaveValue(50);
    // Whole hours left until closing, never below the backend minimum.
    expect(Number(within(dialog).getByLabelText(/Duración/).getAttribute("value"))).toBeGreaterThanOrEqual(1);
    expect(within(dialog).getByText("3 votos")).toBeInTheDocument();
    expect(within(dialog).getByText("1 voto")).toBeInTheDocument();
  });

  it("lists the options ordered by day and time, no matter how they arrived", async () => {
    await openDialog(
      fakeVoting({
        votation: {
          ...VOTATION,
          options: [
            { dateTime: "2026-09-21T18:00:00", voteCount: 1, voterNames: ["D"] },
            { dateTime: "2026-09-20T18:00:00", voteCount: 3, voterNames: ["A", "B", "C"] },
            { dateTime: "2026-09-20T09:00:00", voteCount: 0, voterNames: [] },
          ],
        },
      }),
    );

    const rows = within(screen.getByRole("dialog")).getAllByRole("listitem");
    expect(rows.map((row) => row.textContent)).toEqual([
      expect.stringContaining("20 sep · 09:00"),
      expect.stringContaining("20 sep · 18:00"),
      expect.stringContaining("21 sep · 18:00"),
    ]);
  });

  it("only offers a date picker for the options added in this session", async () => {
    const user = await openDialog(fakeVoting());
    const dialog = screen.getByRole("dialog");

    expect(within(dialog).getByText(/20 sep · 18:00/)).toBeInTheDocument();
    expect(within(dialog).queryByLabelText("Fecha alternativa 1")).not.toBeInTheDocument();

    await user.click(within(dialog).getByRole("button", { name: /Agregar alternativa/ }));
    expect(within(dialog).getByLabelText("Fecha alternativa 3")).toHaveValue("");
  });

  it("previews the resulting set when the organizer removes one option", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog", { name: /Administrar votación/ });

    await user.click(within(dialog).getByRole("button", { name: /Quitar .*20 sep/ }));
    expect(within(dialog).getByText(/21 sep · 18:00/)).toBeInTheDocument();
    expect(within(dialog).queryByText(/20 sep · 18:00/)).not.toBeInTheDocument();
  });

  it("warns inline before dropping voted options and acknowledges the loss when confirmed", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog", { name: /Administrar votación/ });

    await user.click(within(dialog).getByRole("button", { name: /Quitar .*20 sep/ }));
    await user.click(within(dialog).getByRole("button", { name: "Guardar alternativas" }));

    expect(voting.updateOptions).not.toHaveBeenCalled();
    const warning = within(dialog).getByLabelText("Confirmación de pérdida de votos");
    expect(within(warning).getByText(/Vas a perder 3 votos/)).toBeInTheDocument();
    // No overlay on top of the panel: the warning is the panel's own state, and
    // the frozen list keeps the count in the warning from going stale.
    expect(screen.getAllByRole("dialog")).toHaveLength(1);
    expect(within(dialog).getByRole("button", { name: /Quitar .*21 sep/ })).toBeDisabled();
    expect(within(dialog).getByRole("button", { name: /Agregar alternativa/ })).toBeDisabled();

    await user.click(
      within(warning).getByRole("button", { name: "Perder los votos y guardar" }),
    );
    expect(voting.updateOptions).toHaveBeenCalledWith(["2026-09-21T18:00:00"], true);
  });

  it("keeps the draft and saves nothing when the vote loss is not confirmed", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog", { name: /Administrar votación/ });

    await user.click(within(dialog).getByRole("button", { name: /Quitar .*20 sep/ }));
    await user.click(within(dialog).getByRole("button", { name: "Guardar alternativas" }));
    await user.click(within(dialog).getByRole("button", { name: "Volver" }));

    await waitFor(() =>
      expect(within(dialog).queryByLabelText("Confirmación de pérdida de votos")).not.toBeInTheDocument(),
    );
    expect(voting.updateOptions).not.toHaveBeenCalled();
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).queryByText(/20 sep · 18:00/)).not.toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /Quitar .*21 sep/ })).toBeEnabled();
  });

  it("saves right away when the removed option carries no votes", async () => {
    const voting = fakeVoting({
      votation: {
        ...VOTATION,
        options: [
          { dateTime: "2026-09-20T18:00:00", voteCount: 0, voterNames: [] },
          { dateTime: "2026-09-21T18:00:00", voteCount: 1, voterNames: ["D"] },
        ],
      },
    });
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog", { name: /Administrar votación/ });

    await user.click(within(dialog).getByRole("button", { name: /Quitar .*20 sep/ }));
    await user.click(within(dialog).getByRole("button", { name: "Guardar alternativas" }));

    expect(within(dialog).queryByLabelText("Confirmación de pérdida de votos")).not.toBeInTheDocument();
    expect(voting.updateOptions).toHaveBeenCalledWith(["2026-09-21T18:00:00"]);
  });

  it("appends an added option to the existing ones", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: /Agregar alternativa/ }));
    await user.type(within(dialog).getByLabelText("Fecha alternativa 3"), "2026-09-22T10:00");
    await user.click(within(dialog).getByRole("button", { name: "Guardar alternativas" }));

    expect(voting.updateOptions).toHaveBeenCalledWith([
      "2026-09-20T18:00:00",
      "2026-09-21T18:00:00",
      "2026-09-22T10:00:00",
    ]);
  });

  it("blocks saving while an added option has no date", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: /Agregar alternativa/ }));
    expect(within(dialog).getByRole("button", { name: "Guardar alternativas" })).toBeDisabled();

    await user.type(within(dialog).getByLabelText("Fecha alternativa 3"), "2026-09-22T10:00");
    expect(within(dialog).getByRole("button", { name: "Guardar alternativas" })).toBeEnabled();
  });

  it("blocks saving when the same date is listed twice", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: /Agregar alternativa/ }));
    await user.type(within(dialog).getByLabelText("Fecha alternativa 3"), "2026-09-20T18:00");
    expect(within(dialog).getByRole("button", { name: "Guardar alternativas" })).toBeDisabled();
  });

  it("sends the quorum as a fraction and the duration as an ISO-8601 duration", async () => {
    const voting = fakeVoting();
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog");

    const quorum = within(dialog).getByLabelText(/Quórum/);
    await user.clear(quorum);
    await user.type(quorum, "75");
    const duration = within(dialog).getByLabelText(/Duración/);
    await user.clear(duration);
    await user.type(duration, "6");

    await user.click(within(dialog).getByRole("button", { name: "Guardar configuración" }));
    expect(voting.updateSettings).toHaveBeenCalledWith(0.75, 6);
  });

  it("keeps the sheet open and shows the failure when the backend rejects the change", async () => {
    const voting = fakeVoting({
      updateOptions: vi.fn().mockRejectedValue(new Error("Algunas fechas están fuera del rango permitido.")),
    });
    const user = await openDialog(voting);
    const dialog = screen.getByRole("dialog");

    await user.click(within(dialog).getByRole("button", { name: "Guardar alternativas" }));

    await waitFor(() =>
      expect(
        within(screen.getByRole("dialog")).getByRole("alert"),
      ).toHaveTextContent("Algunas fechas están fuera del rango permitido."),
    );
    expect(dialog).toBeInTheDocument();
  });

  it("locks the form when the votation is no longer active", async () => {
    const voting = fakeVoting({ votation: { ...VOTATION, status: "CLOSED" } });
    await openDialog(voting);

    const dialog = screen.getByRole("dialog");
    expect(within(dialog).getByLabelText(/Quórum/)).toBeDisabled();
    expect(within(dialog).getByLabelText(/Duración/)).toBeDisabled();
    expect(within(dialog).getByRole("button", { name: "Guardar configuración" })).toBeDisabled();
    expect(within(dialog).queryByLabelText("Fecha alternativa 1")).not.toBeInTheDocument();
    expect(within(dialog).getByRole("button", { name: /Quitar .*20 sep/ })).toBeDisabled();
    expect(within(dialog).getByRole("button", { name: "Guardar alternativas" })).toBeDisabled();
    expect(within(dialog).getByRole("button", { name: /Agregar alternativa/ })).toBeDisabled();
  });

  it("does not render without a votation", () => {
    render(<VotingRoom voting={fakeVoting({ votation: null })} organizer />);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});

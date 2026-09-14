import { describe, expect, it } from "vitest";
import { getInitials, participantDisplayName } from "@/lib/initials";
import type { CurrentUser } from "@/types/domain";

const me: CurrentUser = { id: "me-1", name: "Vale Ríos", roles: ["USER"] };

describe("getInitials", () => {
  it("takes the first two words' initials, uppercased", () => {
    expect(getInitials("Vale Ríos")).toBe("VR");
    expect(getInitials("luna mendez perez")).toBe("LM");
  });

  it("handles a single word and empty input", () => {
    expect(getInitials("Sofi")).toBe("S");
    expect(getInitials("")).toBe("");
  });
});

describe("participantDisplayName", () => {
  it("returns the current user's name and the generic label for anyone else", () => {
    expect(participantDisplayName("me-1", me)).toBe("Vale Ríos");
    expect(participantDisplayName("someone-else", me)).toBe("Invitade");
    expect(participantDisplayName("u", null)).toBe("Invitade");
  });
});
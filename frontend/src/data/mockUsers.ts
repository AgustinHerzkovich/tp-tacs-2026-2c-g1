// Mock login roster. Shaped exactly like the backend's user record — just
// `id` + `name`, see backend/src/main/java/com/solnotfound/entity/user/User.java
// and dto/UserDTO.java — because that's *all* the backend ever stores per
// user. There's no seed/demo data on the backend side to mirror (a user
// record is only ever created lazily from a JWT subject on first use), so
// this roster is invented for the mock login screen, reusing the same cast
// of names that already shows up as mock activity participants
// (see PEOPLE in mockData.ts) for a bit of continuity.
//
// "development-user" matches the id the backend falls back to when a request
// carries no JWT (see e.g. NotificationController.currentUserId) — logging in
// as Juan keeps that behavior consistent in local/dev testing.

import type { UserDTO } from "@/types/backend";

export const MOCK_USERS: UserDTO[] = [
  { id: "development-user", name: "Juan Luengo" },
  { id: "vale-rios", name: "Vale Ríos" },
  { id: "fede-molina", name: "Fede Molina" },
  { id: "cami-paz", name: "Cami Paz" },
  { id: "nico-gimenez", name: "Nico Giménez" },
  { id: "sol-alvarez", name: "Sol Álvarez" },
];

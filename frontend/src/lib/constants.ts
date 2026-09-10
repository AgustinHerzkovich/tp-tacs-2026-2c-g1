/** The identity the backend falls back to when a request carries no JWT —
 * see e.g. `VotationController#currentUserId` / `NotificationController`.
 * There's no real login yet (the frontend's own auth is mocked, see
 * useAuth/TODO.md), so every backend write happens as this user regardless
 * of which mock user is "logged in" in the UI. Used to figure out things
 * like "did *I* already join this activity" against real participant data. */
export const BACKEND_FALLBACK_USER_ID = "development-user";

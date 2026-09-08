import type { Middleware } from "@reduxjs/toolkit";
import { login, logout } from "@/store/session/sessionSlice";
import type { CurrentUser } from "@/types/domain";

export const SESSION_STORAGE_KEY = "planazo:session-user";

/** Reads the persisted mock session. Client-only (returns null during SSR,
 * where there's no `window`) — call this once, from an effect, never from a
 * component body, or the server/client initial render will disagree and
 * React will flag a hydration mismatch. */
export function readPersistedUser(): CurrentUser | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(SESSION_STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CurrentUser) : null;
  } catch {
    return null;
  }
}

function writePersistedUser(user: CurrentUser | null): void {
  try {
    if (user) {
      window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(user));
    } else {
      window.localStorage.removeItem(SESSION_STORAGE_KEY);
    }
  } catch {
    // Best-effort: localStorage can throw (private browsing, quota, disabled).
  }
}

/** Mirrors every login/logout into localStorage so a page refresh doesn't
 * drop the mock session. The initial read on mount is a separate step (see
 * `readPersistedUser`, dispatched from `StoreProvider`) — this middleware
 * only keeps storage in sync with whatever happens *after* that. */
export const sessionPersistenceMiddleware: Middleware = () => (next) => (action) => {
  const result = next(action);
  if (login.match(action)) writePersistedUser(action.payload);
  else if (logout.match(action)) writePersistedUser(null);
  return result;
};

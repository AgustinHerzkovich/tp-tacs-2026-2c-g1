import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { CurrentUser } from "@/types/domain";

export interface SessionState {
  user: CurrentUser | null;
  /** True once the client has checked localStorage for a persisted session.
   * Starts false on both server and client so the very first render always
   * matches (no hydration mismatch) — `useRequireAuth` waits for this before
   * deciding to redirect to /login, so a persisted session isn't wrongly
   * treated as "logged out" for the one tick before it's read. */
  hydrated: boolean;
}

const initialState: SessionState = {
  user: null,
  hydrated: false,
};

const sessionSlice = createSlice({
  name: "session",
  initialState,
  reducers: {
    login(state, action: PayloadAction<CurrentUser>) {
      state.user = action.payload;
    },
    logout(state) {
      state.user = null;
    },
    hydrate(state, action: PayloadAction<CurrentUser | null>) {
      state.user = action.payload;
      state.hydrated = true;
    },
  },
});

export const { login, logout, hydrate } = sessionSlice.actions;
export default sessionSlice.reducer;

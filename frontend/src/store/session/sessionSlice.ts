import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import type { CurrentUser } from "@/types/domain";

export interface SessionState {
  user: CurrentUser | null;
  /** True once Keycloak has checked the browser's SSO session. */
  initialized: boolean;
}

const initialState: SessionState = {
  user: null,
  initialized: false,
};

const sessionSlice = createSlice({
  name: "session",
  initialState,
  reducers: {
    authenticated(state, action: PayloadAction<CurrentUser>) {
      state.user = action.payload;
      state.initialized = true;
    },
    anonymous(state) {
      state.user = null;
      state.initialized = true;
    },
  },
});

export const { authenticated, anonymous } = sessionSlice.actions;
export default sessionSlice.reducer;

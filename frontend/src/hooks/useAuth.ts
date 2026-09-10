"use client";

import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { login as loginAction, logout as logoutAction } from "@/store/session/sessionSlice";
import type { CurrentUser } from "@/types/domain";

export interface UseAuth {
  user: CurrentUser | null;
  isAuthenticated: boolean;
  login: (user: CurrentUser) => void;
  logout: () => void;
}

/** Mock auth, backed by the Redux session slice — there's no real backend
 * login yet (see TODO.md), so `login` just sets the current user directly
 * instead of exchanging credentials for a JWT. */
export function useAuth(): UseAuth {
  const user = useAppSelector((state) => state.session.user);
  const dispatch = useAppDispatch();

  return {
    user,
    isAuthenticated: user !== null,
    login: (nextUser: CurrentUser) => dispatch(loginAction(nextUser)),
    logout: () => dispatch(logoutAction()),
  };
}

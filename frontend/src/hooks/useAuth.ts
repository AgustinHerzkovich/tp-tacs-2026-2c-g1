"use client";

import { getKeycloak } from "@/lib/keycloak";
import { useAppSelector } from "@/store/hooks";
import type { CurrentUser } from "@/types/domain";

export interface UseAuth {
  user: CurrentUser | null;
  initialized: boolean;
  isAuthenticated: boolean;
  hasRole: (role: string) => boolean;
  login: () => Promise<void>;
  logout: () => Promise<void>;
}

/** Exposes the OIDC session while keeping all token operations in keycloak-js. */
export function useAuth(): UseAuth {
  const user = useAppSelector((state) => state.session.user);
  const initialized = useAppSelector((state) => state.session.initialized);

  return {
    user,
    initialized,
    isAuthenticated: initialized && user !== null,
    hasRole: (role: string) => user?.roles.includes(role) ?? false,
    login: () =>
      getKeycloak().login({ redirectUri: `${window.location.origin}/mis-actividades` }),
    logout: () => getKeycloak().logout({ redirectUri: `${window.location.origin}/login` }),
  };
}

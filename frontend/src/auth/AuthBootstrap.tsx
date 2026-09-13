"use client";

import { useEffect } from "react";
import type { KeycloakTokenParsed } from "keycloak-js";
import { getKeycloak } from "@/lib/keycloak";
import { useAppDispatch } from "@/store/hooks";
import { anonymous, authenticated } from "@/store/session/sessionSlice";
import type { CurrentUser } from "@/types/domain";

type PlanazoToken = KeycloakTokenParsed & {
  name?: string;
  preferred_username?: string;
};

function userFromToken(token: PlanazoToken | undefined): CurrentUser | null {
  if (!token?.sub) return null;
  return {
    id: token.sub,
    name: token.name ?? token.preferred_username ?? token.sub,
    roles: token.realm_access?.roles ?? [],
  };
}

/** Restores an existing Keycloak SSO session after reload while keeping the
 * newly issued access and refresh tokens only in the adapter's memory. */
export function AuthBootstrap() {
  const dispatch = useAppDispatch();

  useEffect(() => {
    const keycloak = getKeycloak();
    const synchronizeSession = () => {
      const user = userFromToken(keycloak.tokenParsed as PlanazoToken | undefined);
      dispatch(user ? authenticated(user) : anonymous());
    };

    keycloak.onAuthSuccess = synchronizeSession;
    keycloak.onAuthRefreshSuccess = synchronizeSession;
    keycloak.onAuthLogout = () => dispatch(anonymous());
    keycloak.onAuthError = () => dispatch(anonymous());
    keycloak.onAuthRefreshError = () => dispatch(anonymous());
    keycloak.onTokenExpired = () => {
      void keycloak.updateToken(-1).then(synchronizeSession).catch(() => dispatch(anonymous()));
    };

    void keycloak
      .init({ onLoad: "check-sso", pkceMethod: "S256", checkLoginIframe: false })
      .then(synchronizeSession)
      .catch(() => dispatch(anonymous()));
  }, [dispatch]);

  return null;
}

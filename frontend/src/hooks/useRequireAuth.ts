"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import type { CurrentUser } from "@/types/domain";

/** Redirects only after Keycloak has confirmed there is no SSO session. */
export function useRequireAuth(): CurrentUser | null {
  const { user, initialized } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (initialized && !user) router.replace("/login");
  }, [initialized, user, router]);

  return user;
}

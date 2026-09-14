"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { usePathname } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import type { CurrentUser } from "@/types/domain";

/** Redirects only after Keycloak has confirmed there is no SSO session. */
export function useRequireAuth(): CurrentUser | null {
  const { user, initialized } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (initialized && !user) router.replace(`/login?returnTo=${encodeURIComponent(pathname ?? "/mis-actividades")}`);
  }, [initialized, pathname, user, router]);

  return user;
}

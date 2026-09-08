"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useAppSelector } from "@/store/hooks";
import type { CurrentUser } from "@/types/domain";

/** Redirects to /login when nobody is logged in. Use at the top of any
 * protected layout/page; render nothing (or a tiny placeholder) while
 * `user` is null so the redirect doesn't flash protected content first.
 *
 * Waits for the session slice's `hydrated` flag before redirecting — right
 * after mount, `user` is briefly null even for someone with a persisted
 * session (see StoreProvider), and redirecting on that first tick would
 * bounce a logged-in person to /login before their session gets restored. */
export function useRequireAuth(): CurrentUser | null {
  const { user } = useAuth();
  const hydrated = useAppSelector((state) => state.session.hydrated);
  const router = useRouter();

  useEffect(() => {
    if (hydrated && !user) router.replace("/login");
  }, [hydrated, user, router]);

  return user;
}

"use client";

import { useEffect, useSyncExternalStore } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";

const noopSubscribe = () => () => {};

/** True only once hydration has committed. The server always renders the
 * "checking session" state (it can't know Keycloak's client-side auth
 * status), so this keeps the very first client render identical to that —
 * otherwise a session restored before/during hydration (e.g. right after a
 * Keycloak logout redirect back to this page) makes React's first client
 * pass disagree with the server HTML and throws a hydration-mismatch error. */
function useHasMounted(): boolean {
  return useSyncExternalStore(noopSubscribe, () => true, () => false);
}

export function LoginPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const requestedRoute = searchParams?.get("returnTo");
  const returnTo = requestedRoute?.startsWith("/") ? requestedRoute : "/mis-actividades";
  const { initialized, isAuthenticated, login } = useAuth();
  const ready = useHasMounted() && initialized;

  useEffect(() => {
    if (isAuthenticated) router.replace(returnTo);
  }, [isAuthenticated, returnTo, router]);

  return (
    <div className="fade-in min-h-screen flex flex-col justify-center px-6 py-10 lg:max-w-xl lg:mx-auto lg:w-full">
      <div className="text-center mb-8">
        <h1
          className="font-brand sticker-outline text-5xl"
          style={{ color: "var(--primary)", transform: "rotate(-3deg)", filter: "drop-shadow(2px 3px 0 rgba(46,42,69,.18))" }}
        >
          Planazo
        </h1>
        <p className="font-brand text-lg mt-3">Organizá tu próximo plan</p>
        <p className="text-[13px] font-bold mt-1" style={{ color: "var(--muted-foreground)" }}>
          Iniciá sesión o creá tu cuenta para empezar.
        </p>
      </div>

      <Button
        size="xl"
        disabled={!ready || isAuthenticated}
        onClick={() => void login()}
      >
        {ready ? "Ingresar o registrarme" : "Comprobando sesión..."}
      </Button>
    </div>
  );
}

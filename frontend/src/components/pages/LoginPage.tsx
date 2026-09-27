"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";
import { parseTelegramChatId, useTelegramLink, type TelegramLinkStatus } from "@/hooks/useTelegramLink";

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
  const telegramChatId = searchParams?.get("t") === "true" ? parseTelegramChatId(searchParams.get("cid")) : null;
  const fromTelegram = telegramChatId !== null;
  const { initialized, isAuthenticated, login } = useAuth();
  const ready = useHasMounted() && initialized;
  const telegramLink = useTelegramLink(telegramChatId, isAuthenticated);

  useEffect(() => {
    if (isAuthenticated && !fromTelegram) router.replace(returnTo);
  }, [isAuthenticated, fromTelegram, returnTo, router]);

  if (fromTelegram && isAuthenticated) {
    return <TelegramLinkResult status={telegramLink.status} retry={telegramLink.retry} onContinueHere={() => router.replace("/")} />;
  }

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
          {fromTelegram
            ? "Iniciá sesión para vincular tu cuenta con el bot de Telegram."
            : "Iniciá sesión o creá tu cuenta de forma segura con Keycloak."}
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

const TELEGRAM_LINK_MESSAGES: Record<TelegramLinkStatus, { title: string; detail: string }> = {
  idle: { title: "Vinculando tu cuenta...", detail: "Esperá un momento." },
  linking: { title: "Vinculando tu cuenta...", detail: "Esperá un momento." },
  linked: { title: "¡Listo! Te logueaste correctamente.", detail: "Tu cuenta quedó vinculada con Telegram. ¿Cómo querés seguir?" },
  error: { title: "No pudimos vincular tu cuenta con Telegram.", detail: "Probá de nuevo en un rato." },
};

const BACK_TO_TELEGRAM_MESSAGE = {
  title: "Ya podés volver a Telegram",
  detail: "Volvé al chat del bot y mandá /start. Podés cerrar esta pestaña.",
};

/** Outcome of linking the Telegram chat. Once linked, the user picks whether to
 * keep using the web app (goes to the home page) or go back to the bot. */
function TelegramLinkResult({
  status,
  retry,
  onContinueHere,
}: {
  status: TelegramLinkStatus;
  retry: () => void;
  onContinueHere: () => void;
}) {
  const [backToTelegram, setBackToTelegram] = useState(false);
  const { title, detail } = backToTelegram ? BACK_TO_TELEGRAM_MESSAGE : TELEGRAM_LINK_MESSAGES[status];
  const pending = status === "idle" || status === "linking";

  return (
    <div className="fade-in min-h-screen flex flex-col justify-center px-6 py-10 lg:max-w-xl lg:mx-auto lg:w-full text-center">
      <div role={status === "error" ? "alert" : "status"} className="mb-8">
        <p className="font-brand text-lg">{title}</p>
        <p className="text-[13px] font-bold mt-1" style={{ color: status === "error" ? "var(--destructive)" : "var(--muted-foreground)" }}>
          {detail}
        </p>
      </div>
      {status === "error" && (
        <Button size="xl" onClick={retry}>
          Reintentar
        </Button>
      )}
      {status === "linked" && !backToTelegram && (
        <div className="flex flex-col gap-3">
          <Button size="xl" onClick={onContinueHere}>
            Seguir en la página
          </Button>
          <Button size="xl" variant="outline" onClick={() => setBackToTelegram(true)}>
            Seguir desde Telegram
          </Button>
        </div>
      )}
      {pending && (
        <Button size="xl" disabled>
          Vinculando...
        </Button>
      )}
    </div>
  );
}

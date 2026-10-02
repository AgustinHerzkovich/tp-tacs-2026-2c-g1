"use client";

import { useEffect, useState, useSyncExternalStore } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";
import { parseTelegramLinkCode, useTelegramLink, type UseTelegramLink } from "@/hooks/useTelegramLink";

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
  const telegramCode = searchParams?.get("t") === "true" ? parseTelegramLinkCode(searchParams.get("link")) : null;
  const fromTelegram = telegramCode !== null;
  const { initialized, isAuthenticated, login, user } = useAuth();
  const ready = useHasMounted() && initialized;
  const telegramLink = useTelegramLink(telegramCode);

  useEffect(() => {
    if (isAuthenticated && !fromTelegram) router.replace(returnTo);
  }, [isAuthenticated, fromTelegram, returnTo, router]);

  if (fromTelegram && isAuthenticated) {
    return (
      <TelegramLinkFlow
        telegramLink={telegramLink}
        accountName={user?.name ?? null}
        onLeave={() => router.replace("/")}
      />
    );
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
            : "Iniciá sesión o creá tu cuenta para empezar."}
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

const BACK_TO_TELEGRAM_MESSAGE = {
  title: "Ya podés volver a Telegram",
  detail: "Volvé al chat del bot y mandá /start. Podés cerrar esta pestaña.",
};

/** Telegram linking for a signed-in user who opened the bot's login link. Nothing is linked until
 * the user confirms: a link sent by someone else must not attach this account to their chat.
 * Once linked, the user picks whether to keep using the web app or go back to the bot. */
function TelegramLinkFlow({
  telegramLink,
  accountName,
  onLeave,
}: {
  telegramLink: UseTelegramLink;
  accountName: string | null;
  onLeave: () => void;
}) {
  const [backToTelegram, setBackToTelegram] = useState(false);
  const { status, error, canRetry, link } = telegramLink;

  const { title, detail } = backToTelegram
    ? BACK_TO_TELEGRAM_MESSAGE
    : status === "linked"
      ? { title: "¡Listo! Tu cuenta quedó vinculada.", detail: "¿Cómo querés seguir?" }
      : status === "error"
        ? { title: "No pudimos vincular tu cuenta con Telegram.", detail: error ?? "Probá de nuevo en un rato." }
        : {
            title: "¿Vincular tu cuenta con Telegram?",
            detail: `${accountName ? `Vas a vincular la cuenta de ${accountName}` : "Vas a vincular tu cuenta"} con el chat del bot que te mandó este link. Confirmá solo si lo pediste vos desde Telegram.`,
          };

  return (
    <div className="fade-in min-h-screen flex flex-col justify-center px-6 py-10 lg:max-w-xl lg:mx-auto lg:w-full text-center">
      <div role={status === "error" ? "alert" : "status"} className="mb-8">
        <p className="font-brand text-lg">{title}</p>
        <p className="text-[13px] font-bold mt-1" style={{ color: status === "error" ? "var(--destructive)" : "var(--muted-foreground)" }}>
          {detail}
        </p>
      </div>
      {(status === "idle" || status === "linking") && (
        <div className="flex flex-col gap-3">
          <Button size="xl" disabled={status === "linking"} onClick={link}>
            {status === "linking" ? "Vinculando..." : "Vincular"}
          </Button>
          <Button size="xl" variant="outline" disabled={status === "linking"} onClick={onLeave}>
            Cancelar
          </Button>
        </div>
      )}
      {status === "error" && (
        <div className="flex flex-col gap-3">
          {canRetry && (
            <Button size="xl" onClick={link}>
              Reintentar
            </Button>
          )}
          <Button size="xl" variant="outline" onClick={onLeave}>
            Ir al inicio
          </Button>
        </div>
      )}
      {status === "linked" && !backToTelegram && (
        <div className="flex flex-col gap-3">
          <Button size="xl" onClick={onLeave}>
            Seguir en la página
          </Button>
          <Button size="xl" variant="outline" onClick={() => setBackToTelegram(true)}>
            Seguir desde Telegram
          </Button>
        </div>
      )}
    </div>
  );
}

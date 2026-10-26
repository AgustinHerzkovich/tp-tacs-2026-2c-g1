"use client";

import { useCallback, useState } from "react";
import { api, ApiError } from "@/lib/api";

export type TelegramLinkStatus = "idle" | "linking" | "linked" | "error";

export interface UseTelegramLink {
  status: TelegramLinkStatus;
  /** User-facing message of the last failure, or null. */
  error: string | null;
  /** False when the code itself was rejected (expired or already used): retrying cannot work,
   * the user has to ask the bot for a new link. */
  canRetry: boolean;
  /** Links the chat of the code to the signed-in user. Only call it after the user confirms. */
  link: () => void;
}

/** Parses the `link` query param of the bot's login link: the single-use code issued by the
 * backend (URL-safe base64). Returns null for anything else, such as the old `cid` links. The
 * param is not called `code` because that name is reserved by OAuth: Keycloak rejects a
 * redirect_uri that already carries it (`invalid_redirect_uri`). */
export function parseTelegramLinkCode(value: string | null | undefined): string | null {
  return value && /^[A-Za-z0-9_-]{20,128}$/.test(value) ? value : null;
}

/** Redeems the bot's single-use link code for the signed-in user, via PUT /api/users/me/telegram.
 * Nothing happens until `link` is called: the page asks the user to confirm first, so opening a
 * link someone else sent never links an account silently. */
export function useTelegramLink(code: string | null): UseTelegramLink {
  const [status, setStatus] = useState<TelegramLinkStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [canRetry, setCanRetry] = useState(true);

  const link = useCallback(() => {
    if (code === null || status === "linking" || status === "linked") return;
    setStatus("linking");
    setError(null);
    api.users
      .linkTelegram(code)
      .then(() => setStatus("linked"))
      .catch((err: unknown) => {
        setStatus("error");
        setError(err instanceof Error ? err.message : "No pudimos vincular tu cuenta con Telegram.");
        setCanRetry(!(err instanceof ApiError && err.code === "TELEGRAM_LINK_CODE_INVALID"));
      });
  }, [code, status]);

  return { status, error, canRetry, link };
}

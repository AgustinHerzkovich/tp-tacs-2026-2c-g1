"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/lib/api";

export type TelegramLinkStatus = "idle" | "linking" | "linked" | "error";

export interface UseTelegramLink {
  status: TelegramLinkStatus;
  retry: () => void;
}

/** Parses the `cid` query param of the Telegram login link; null unless it is an integer chat id. */
export function parseTelegramChatId(value: string | null | undefined): number | null {
  if (!value || !/^-?\d+$/.test(value)) return null;
  const chatId = Number(value);
  return Number.isSafeInteger(chatId) ? chatId : null;
}

/** Links the Telegram chat from the bot's login link to the signed-in user, via
 * PUT /api/users/me/telegram. It runs once, as soon as `enabled` (the user is
 * authenticated) and there is a valid chat id; `retry` runs it again after an
 * error. The ref guard keeps React's double-invoked effects from sending two requests. */
export function useTelegramLink(chatId: number | null, enabled: boolean): UseTelegramLink {
  const [status, setStatus] = useState<TelegramLinkStatus>("idle");
  const started = useRef(false);

  const link = useCallback(async () => {
    if (chatId === null) return;
    setStatus("linking");
    try {
      await api.users.linkTelegram(chatId);
      setStatus("linked");
    } catch {
      setStatus("error");
    }
  }, [chatId]);

  useEffect(() => {
    if (!enabled || chatId === null || started.current) return;
    started.current = true;
    void link();
  }, [enabled, chatId, link]);

  return { status, retry: () => void link() };
}

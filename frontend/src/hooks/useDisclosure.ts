"use client";

import { useCallback, useState } from "react";

export interface UseDisclosure {
  isOpen: boolean;
  open: () => void;
  close: () => void;
  toggle: () => void;
}

/** Tiny open/close boolean state helper, shared by drawers, dialogs and modals. */
export function useDisclosure(initial = false): UseDisclosure {
  const [isOpen, setIsOpen] = useState(initial);
  const open = useCallback(() => setIsOpen(true), []);
  const close = useCallback(() => setIsOpen(false), []);
  const toggle = useCallback(() => setIsOpen((v) => !v), []);
  return { isOpen, open, close, toggle };
}

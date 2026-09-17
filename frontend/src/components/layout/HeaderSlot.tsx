"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";

/** Lets a page register content into the shared sticky `Header` (rendered by
 * the `(withHeader)` layout, a sibling of the page — not an ancestor — so
 * plain props can't reach it). Any page under that layout can call
 * `useHeaderCenterSlot(node)` to project content between the logo and the
 * icon cluster; `null`/omitted leaves the header exactly as it is today. */
const HeaderSlotContext = createContext<{
  centerSlot: ReactNode;
  setCenterSlot: (node: ReactNode) => void;
} | null>(null);

export function HeaderSlotProvider({ children }: { children: ReactNode }) {
  const [centerSlot, setCenterSlot] = useState<ReactNode>(null);
  return (
    <HeaderSlotContext.Provider value={{ centerSlot, setCenterSlot }}>
      {children}
    </HeaderSlotContext.Provider>
  );
}

export function useHeaderSlotValue() {
  const ctx = useContext(HeaderSlotContext);
  if (!ctx) throw new Error("useHeaderSlotValue must be used within HeaderSlotProvider");
  return ctx.centerSlot;
}

export function useHeaderCenterSlot(node: ReactNode) {
  const ctx = useContext(HeaderSlotContext);
  if (!ctx) throw new Error("useHeaderCenterSlot must be used within HeaderSlotProvider");
  const { setCenterSlot } = ctx;
  useEffect(() => {
    setCenterSlot(node);
    return () => setCenterSlot(null);
  }, [node, setCenterSlot]);
}

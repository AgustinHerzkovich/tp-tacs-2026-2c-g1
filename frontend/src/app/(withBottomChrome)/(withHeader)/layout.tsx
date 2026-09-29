"use client";

import type { ReactNode } from "react";
import { Header } from "@/components/layout/Header";
import { NotifDrawer } from "@/components/layout/NotifDrawer";
import { HeaderSlotProvider, useHeaderSlotValue } from "@/components/layout/HeaderSlot";
import { useDisclosure } from "@/hooks/useDisclosure";
import { useNotifications } from "@/hooks/useNotifications";

/** Adds the top app bar (logo, notification bell, avatar) — only for the two
 * main tabs (Explorar, Mis Actividades). The activity detail page sits
 * outside this group on purpose: it uses its own hero image + back button
 * instead of the app bar. */
export default function HeaderLayout({ children }: { children: ReactNode }) {
  return (
    <HeaderSlotProvider>
      <HeaderLayoutContent>{children}</HeaderLayoutContent>
    </HeaderSlotProvider>
  );
}

function HeaderLayoutContent({ children }: { children: ReactNode }) {
  const drawer = useDisclosure();
  const notifications = useNotifications();
  const centerSlot = useHeaderSlotValue();

  return (
    <div className="flex flex-col min-h-full">
      <Header onBellClick={drawer.open} unread={notifications.unreadCount} centerSlot={centerSlot} />
      <div className="flex-1">{children}</div>
      <NotifDrawer open={drawer.isOpen} onOpenChange={(v) => (v ? drawer.open() : drawer.close())} {...notifications} />
    </div>
  );
}

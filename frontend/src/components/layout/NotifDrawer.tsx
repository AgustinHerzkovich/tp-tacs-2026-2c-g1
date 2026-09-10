"use client";

import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { useNotifications } from "@/hooks/useNotifications";
import { NOTIF_META } from "@/data/mockData";
import { mapNotificationKind, iconForNotificationKind } from "@/lib/notificationMapping";
import { formatRelativeTime } from "@/lib/formatDate";

interface NotifDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function NotifDrawer({ open, onOpenChange }: NotifDrawerProps) {
  const { notifications, loading, error } = useNotifications();

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[84%] sm:max-w-[360px] px-0 gap-0">
        <SheetHeader className="px-5 pt-1 pb-4 border-b-2" style={{ borderColor: "var(--border)" }}>
          <SheetTitle className="font-display font-semibold text-[13px] tracking-wide" style={{ color: "var(--muted-foreground)" }}>
            TUS ALERTAS RECIENTES
          </SheetTitle>
        </SheetHeader>
        <div className="px-4 py-4 space-y-3 overflow-y-auto">
          {loading && (
            <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--muted-foreground)" }}>
              Cargando...
            </p>
          )}
          {error && (
            <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--rose-ink)" }}>
              {error}
            </p>
          )}
          {!loading && !error && notifications.length === 0 && (
            <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--muted-foreground)" }}>
              No tenés notificaciones todavía.
            </p>
          )}
          {notifications.map((n) => {
            const kind = mapNotificationKind(n.type);
            const m = NOTIF_META[kind];
            return (
              <div key={n.id} className="rounded-2xl p-3.5 border-2" style={{ background: m.bg, borderColor: m.border }}>
                <div className="flex gap-3">
                  <span className="emoji-3d text-2xl shrink-0">{iconForNotificationKind(kind)}</span>
                  <div className="min-w-0 flex-1">
                    <p className="font-display font-semibold text-[13.5px]" style={{ color: m.ink }}>
                      {n.title}
                    </p>
                    <p className="text-[12px] font-bold mt-0.5 leading-snug" style={{ color: m.ink, opacity: 0.85 }}>
                      {n.message}
                    </p>
                    <p className="text-[10.5px] font-extrabold mt-1.5" style={{ color: m.ink, opacity: 0.6 }}>
                      {formatRelativeTime(n.createdAt)}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </SheetContent>
    </Sheet>
  );
}

"use client";

import { useRouter } from "next/navigation";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { NOTIF_META } from "@/lib/activityVisuals";
import type { NotificationView } from "@/hooks/useNotifications";
import { PageControls } from "@/components/common/PageControls";

interface NotifDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  notifications: NotificationView[];
  loading: boolean;
  error: string | null;
  markRead: (id: string) => Promise<void>;
  page: number;
  totalPages: number;
  setPage: (page: number) => void;
}

export function NotifDrawer({ open, onOpenChange, notifications, loading, error, markRead, page, totalPages, setPage }: NotifDrawerProps) {
  const router = useRouter();

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
              Cargando…
            </p>
          )}
          {error && (
            <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--destructive)" }}>
              {error}
            </p>
          )}
          {!loading && !error && notifications.length === 0 && (
            <p className="text-center text-[13px] font-bold py-6" style={{ color: "var(--muted-foreground)" }}>
              No tenés alertas nuevas.
            </p>
          )}
          {notifications.map((n) => {
            const m = NOTIF_META[n.kind];
            return (
              <button
                key={n.id}
                type="button"
                onClick={() => void markRead(n.id).then(() => { onOpenChange(false); router.push(`/actividades/${n.activityId}`); })}
                className="tap block w-full text-left rounded-2xl p-3.5 border-2"
                style={{ background: m.bg, borderColor: m.border }}
              >
                <div className="flex gap-3">
                  <span className="emoji-3d text-2xl shrink-0">{n.icon}</span>
                  <div className="min-w-0 flex-1">
                    <p className="font-display font-semibold text-[13.5px]" style={{ color: m.ink }}>
                      {n.title}
                    </p>
                    <p className="text-[12px] font-bold mt-0.5 leading-snug" style={{ color: m.ink, opacity: 0.85 }}>
                      {n.body}
                    </p>
                    <p className="text-[10.5px] font-extrabold mt-1.5" style={{ color: m.ink, opacity: 0.6 }}>
                      {n.time}
                    </p>
                  </div>
                </div>
              </button>
            );
          })}
          {!loading && !error && <PageControls page={page} totalPages={totalPages} onPageChange={setPage} />}
        </div>
      </SheetContent>
    </Sheet>
  );
}

"use client";

import { useRouter } from "next/navigation";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { Skeleton } from "@/components/ui/skeleton";
import { NOTIF_META, TONE_META } from "@/lib/activityVisuals";
import type { NotificationView } from "@/hooks/useNotifications";
import { PageControls } from "@/components/common/PageControls";
import { useState } from "react";
import { useToast } from "@/components/common/ToastProvider";

interface NotifDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  notifications: NotificationView[];
  unreadCount: number;
  total: number;
  loading: boolean;
  error: string | null;
  markRead: (id: string) => Promise<void>;
  page: number;
  totalPages: number;
  setPage: (page: number) => void;
}

/** One alert. Unread cards get the sticker treatment (tone fill, rotation,
 * hard shadow, red dot) so they read as "new" at a glance; read ones flatten
 * out to white/borderless-shadow/no-rotation with a muted "✓ Leída" tag —
 * same information, clearly lower priority. Both are tappable straight
 * through to the activity ("Ver actividad →"). */
function NotifCard({
  notification,
  read,
  rotate,
  pending,
  onClick,
}: {
  notification: NotificationView;
  read: boolean;
  rotate: number;
  pending: boolean;
  onClick: () => void;
}) {
  const m = TONE_META[NOTIF_META[notification.kind].tone];

  if (read) {
    return (
      <button
        type="button"
        disabled={pending}
        onClick={onClick}
        className="tap block w-full text-left rounded-2xl p-3.5 border-2 mb-2.5"
        style={{ borderColor: "var(--border)" }}
      >
        <div className="flex gap-2.5 items-start">
          <span className="text-[19px] shrink-0 opacity-55">{notification.icon}</span>
          <div className="min-w-0 flex-1">
            <p className="font-display font-extrabold text-[13px]" style={{ color: "var(--foreground)", opacity: 0.75 }}>
              {notification.title}
            </p>
            <p className="text-[11.5px] font-semibold mt-0.5 leading-snug" style={{ color: "var(--muted-foreground)" }}>
              {notification.body}
            </p>
            <div className="flex items-center justify-between mt-1.5">
              <span className="text-[9.5px] font-extrabold" style={{ color: "var(--mint-ink)", opacity: 0.8 }}>
                ✓ Leída · {notification.time}
              </span>
              <span className="text-[10px] font-extrabold" style={{ color: "var(--muted-foreground)" }}>
                Ver actividad →
              </span>
            </div>
          </div>
        </div>
      </button>
    );
  }

  return (
    <button
      type="button"
      disabled={pending}
      onClick={onClick}
      className="tap relative block w-full text-left p-3.5 border-2 border-white mb-3.5"
      style={{ background: m.bg, borderRadius: "4px 18px 18px 18px", boxShadow: `0 5px 0 ${m.ink}`, transform: `rotate(${rotate}deg)` }}
    >
      <span
        aria-hidden="true"
        className="absolute -top-1.5 right-2.5 w-3 h-3 rounded-full border-2 border-white"
        style={{ background: "var(--primary)" }}
      />
      <div className="flex gap-2.5">
        <span className="emoji-3d text-2xl shrink-0">{notification.icon}</span>
        <div className="min-w-0 flex-1">
          <p className="font-display font-extrabold text-[13.5px]" style={{ color: m.ink }}>
            {notification.title}
          </p>
          <p className="text-[12px] font-bold mt-1 leading-snug" style={{ color: m.ink, opacity: 0.85 }}>
            {notification.body}
          </p>
          <div className="flex items-center justify-between mt-2">
            <span className="text-[10px] font-extrabold" style={{ color: m.ink, opacity: 0.6 }}>
              {notification.time}
            </span>
            <span className="text-[10.5px] font-black" style={{ color: m.ink }}>
              Ver actividad →
            </span>
          </div>
        </div>
      </div>
    </button>
  );
}

export function NotifDrawer({
  open,
  onOpenChange,
  notifications,
  unreadCount,
  total,
  loading,
  error,
  markRead,
  page,
  totalPages,
  setPage,
}: NotifDrawerProps) {
  const router = useRouter();
  const toast = useToast();
  const [pendingId, setPendingId] = useState<string | null>(null);

  const unread = notifications.filter((n) => !n.read);
  const read = notifications.filter((n) => n.read);

  const handleOpen = (n: NotificationView) => {
    if (pendingId) return;
    // Already read: no need to hit the mark-read endpoint again, just go.
    if (n.read) {
      onOpenChange(false);
      router.push(`/actividades/${n.activityId}`);
      return;
    }
    setPendingId(n.id);
    void markRead(n.id)
      .then(() => {
        toast("Notificación marcada como leída.");
        onOpenChange(false);
        router.push(`/actividades/${n.activityId}`);
      })
      .catch(() => toast("No pudimos marcar la notificación.", "error"))
      .finally(() => setPendingId(null));
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="w-[84%] sm:max-w-[360px] px-0 gap-0">
        <SheetHeader className="px-5 pt-5 pb-4 border-b-2" style={{ borderColor: "var(--border)" }}>
          <SheetTitle className="font-brand" style={{ fontSize: 24, letterSpacing: "0.02em", color: "var(--foreground)" }}>
            Notificaciones
          </SheetTitle>
          {!loading && !error && notifications.length > 0 && (
            <p className="text-[11.5px] font-extrabold" style={{ color: "var(--primary)" }}>
              {unreadCount > 0 ? `${unreadCount} sin leer · ${total} en total` : `Estás al día · ${total} en total`}
            </p>
          )}
        </SheetHeader>
        <div className="px-4 py-4 overflow-y-auto">
          {loading && (
            <div className="space-y-3" aria-busy="true" aria-label="Cargando alertas">
              <span className="sr-only" role="status">Cargando alertas</span>
              {Array.from({ length: 3 }, (_, i) => (
                <div key={i} className="flex gap-3 rounded-2xl border-2 p-3.5" style={{ borderColor: "var(--border)" }}>
                  <Skeleton className="size-8 rounded-full shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <Skeleton className="h-3.5 w-2/3" />
                    <Skeleton className="h-3 w-full" />
                    <Skeleton className="h-2.5 w-1/4" />
                  </div>
                </div>
              ))}
            </div>
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

          {!loading && !error && unread.length > 0 && (
            <>
              <div className="flex items-center mb-3">
                <span
                  className="inline-flex items-center gap-1.5 border-2 border-white px-3 py-1.5 text-[10.5px] font-black uppercase tracking-wide"
                  style={{
                    background: "var(--rose)",
                    color: "var(--rose-ink)",
                    borderRadius: "10px 10px 10px 2px",
                    boxShadow: "0 3px 0 var(--rose-ink)",
                    transform: "rotate(-1.5deg)",
                  }}
                >
                  🔥 Nuevas
                </span>
              </div>
              {unread.map((n, i) => (
                <NotifCard
                  key={n.id}
                  notification={n}
                  read={false}
                  rotate={i % 2 === 0 ? -1 : 1.2}
                  pending={pendingId === n.id}
                  onClick={() => handleOpen(n)}
                />
              ))}
            </>
          )}

          {!loading && !error && read.length > 0 && (
            <>
              <div className="flex items-center gap-2.5 mt-1 mb-3">
                <span className="text-[10.5px] font-extrabold uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
                  Leídas
                </span>
                <div className="flex-1 h-0 border-t-2 border-dashed" style={{ borderColor: "var(--border)" }} />
              </div>
              {read.map((n) => (
                <NotifCard key={n.id} notification={n} read rotate={0} pending={pendingId === n.id} onClick={() => handleOpen(n)} />
              ))}
            </>
          )}

          {!loading && !error && <PageControls page={page} totalPages={totalPages} onPageChange={setPage} />}
        </div>
      </SheetContent>
    </Sheet>
  );
}

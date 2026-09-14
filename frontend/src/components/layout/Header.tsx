"use client";

import Link from "next/link";
import { BarChart3, Bell, LogOut, UserRound } from "lucide-react";
import { useState } from "react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/hooks/useAuth";
import { getInitials } from "@/lib/initials";
import { ConfirmModal } from "@/components/common/ConfirmModal";

interface HeaderProps {
  onBellClick: () => void;
  unread?: number;
}

export function Header({ onBellClick, unread = 0 }: HeaderProps) {
  const { user, hasRole, logout } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);

  return (
    <div
      className="sticky top-0 z-20 backdrop-blur px-5 pt-6 pb-3 flex items-center justify-between"
      style={{ background: "rgba(255,248,242,.9)" }}
    >
      <h1 className="font-display font-semibold text-[26px]" style={{ color: "var(--primary)" }}>
        Planazo
      </h1>
      <div className="flex items-center gap-3">
        {hasRole("ADMIN") && (
          <Link
            href="/estadisticas"
            className="tap w-10 h-10 rounded-full bg-white border-2 flex items-center justify-center"
            style={{ borderColor: "var(--border)" }}
            aria-label="Estadísticas"
          >
            <BarChart3 className="size-[18px]" style={{ color: "var(--foreground)" }} />
          </Link>
        )}
        <button
          onClick={onBellClick}
          className="tap relative w-10 h-10 rounded-full bg-white border-2 flex items-center justify-center"
          style={{ borderColor: "var(--border)" }}
          aria-label="Notificaciones"
        >
          <Bell className="size-[18px]" style={{ color: "var(--foreground)" }} />
          {unread > 0 && (
            <span
              className="absolute top-1.5 right-2 w-2.5 h-2.5 rounded-full border-2 border-white"
              style={{ background: "var(--rose-ink)" }}
            />
          )}
        </button>
        <div className="relative">
          <button onClick={() => setProfileOpen((open) => !open)} aria-label="Abrir menú de perfil" aria-expanded={profileOpen} className="tap rounded-full focus-visible:ring-2 focus-visible:ring-[var(--ring)]">
            <Avatar>
              <AvatarFallback className="font-display font-semibold text-[13px]" style={{ background: "var(--lav)", color: "var(--lav-ink)" }}>
                {user ? getInitials(user.name) : "?"}
              </AvatarFallback>
            </Avatar>
          </button>
          {profileOpen && (
            <div className="absolute right-0 top-12 z-50 w-64 rounded-2xl border-2 bg-white p-3 shadow-xl" role="menu">
              <div className="flex items-center gap-3 border-b pb-3" style={{ borderColor: "var(--border)" }}>
                <UserRound className="size-5" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-extrabold">{user?.name}</p>
                  <p className="text-[11px] font-bold" style={{ color: "var(--muted-foreground)" }}>{hasRole("ADMIN") ? "Administrador" : "Usuario"}</p>
                </div>
              </div>
              <button type="button" role="menuitem" className="mt-2 flex min-h-11 w-full items-center gap-2 rounded-xl px-3 text-left text-sm font-extrabold hover:bg-muted focus-visible:ring-2 focus-visible:ring-[var(--ring)]" onClick={() => { setProfileOpen(false); setLogoutOpen(true); }}>
                <LogOut className="size-4" /> Cerrar sesión
              </button>
            </div>
          )}
        </div>
      </div>
      <ConfirmModal open={logoutOpen} onOpenChange={setLogoutOpen} title="¿Cerrar sesión?" description={`Vas a salir de la cuenta de ${user?.name ?? "Planazo"}.`} confirmLabel="Cerrar sesión" destructive onConfirm={() => void logout()} />
    </div>
  );
}

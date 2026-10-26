"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { BarChart3, Bell, CloudSun, LogOut, UserRound } from "lucide-react";
import { useState, type ReactNode } from "react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/hooks/useAuth";
import { getInitials } from "@/lib/initials";
import { ConfirmModal } from "@/components/common/ConfirmModal";
import { useWeatherProvider } from "@/hooks/useWeatherProvider";
import type { WeatherProvider } from "@/types/backend";

const PROVIDER_OPTIONS: Array<{ value: WeatherProvider; label: string }> = [
  { value: "OPEN_METEO", label: "Principal" },
  { value: "LEGACY", label: "Alternativo" },
];

interface HeaderProps {
  onBellClick: () => void;
  unread?: number;
  /** Optional content projected between the logo and the icon cluster (see
   * `HeaderSlot.tsx`) — used by Explorar to dock its search bar summary here
   * once the full search bar has scrolled out of view. */
  centerSlot?: ReactNode;
}

export function Header({ onBellClick, unread = 0, centerSlot }: HeaderProps) {
  const router = useRouter();
  const { user, hasRole, logout } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const isAdmin = hasRole("ADMIN");
  const weatherProvider = useWeatherProvider(profileOpen);

  return (
    <div
      className="sticky top-0 z-20 backdrop-blur px-5 pt-6 pb-3 flex items-center justify-between gap-3"
      style={{ background: "rgba(255,248,242,.9)" }}
    >
      <h1
        className="font-brand sticker-outline text-[28px] lg:text-[34px] shrink-0"
        style={{ color: "var(--primary)", transform: "rotate(-3deg)", filter: "drop-shadow(2px 3px 0 rgba(46,42,69,.18))" }}
      >
        {centerSlot ? (
          <>
            {/* The docked search-bar summary needs the room back on mobile. */}
            <span className="lg:hidden">Pzo</span>
            <span className="hidden lg:inline">Planazo</span>
          </>
        ) : (
          "Planazo"
        )}
      </h1>
      {centerSlot && <div className="flex-1 min-w-0 flex justify-center">{centerSlot}</div>}
      <div className="flex items-center gap-3 shrink-0">
        {isAdmin && (
          <Link
            href="/estadisticas"
            className="tap hidden lg:flex w-10 h-10 rounded-full bg-white shadow-[0_3px_0_var(--lav)] items-center justify-center"
            aria-label="Estadísticas"
          >
            <BarChart3 className="size-[18px]" style={{ color: "var(--foreground)" }} />
          </Link>
        )}
        <button
          onClick={onBellClick}
          className="tap relative hidden lg:flex w-10 h-10 rounded-full bg-white shadow-[0_3px_0_var(--lav)] items-center justify-center"
          aria-label="Notificaciones"
        >
          <Bell className="size-[18px]" style={{ color: "var(--sun-ink)" }} />
          {unread > 0 && (
            <span
              aria-hidden="true"
              className="absolute -top-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-white"
              style={{ background: "var(--primary)" }}
            />
          )}
        </button>
        <div className="relative">
          <button
            onClick={() => setProfileOpen((open) => !open)}
            aria-label="Abrir menú de perfil"
            aria-expanded={profileOpen}
            className="tap relative rounded-full shadow-[0_3px_0_var(--lav-ink)] focus-visible:ring-2 focus-visible:ring-[var(--ring)]"
          >
            <Avatar>
              <AvatarFallback className="font-brand text-[13px]" style={{ background: "var(--lav)", color: "var(--lav-ink)" }}>
                {user ? getInitials(user.name) : "?"}
              </AvatarFallback>
            </Avatar>
            {unread > 0 && (
              <span
                aria-hidden="true"
                className="lg:hidden absolute -top-0.5 -right-0.5 w-3 h-3 rounded-full border-2 border-white"
                style={{ background: "var(--primary)" }}
              />
            )}
          </button>
          {profileOpen && (
            <div className="absolute right-0 top-12 z-50 w-64 rounded-2xl border-2 bg-white p-3 shadow-xl" role="menu">
              <div className="flex items-center gap-3 border-b pb-3" style={{ borderColor: "var(--border)" }}>
                <UserRound className="size-5" aria-hidden="true" />
                <div className="min-w-0">
                  <p className="truncate text-sm font-extrabold">{user?.name}</p>
                  <p className="text-[11px] font-bold" style={{ color: "var(--muted-foreground)" }}>{isAdmin ? "Administrador" : "Usuario"}</p>
                </div>
              </div>
              <div className="mt-2 border-b pb-3" style={{ borderColor: "var(--border)" }}>
                <p className="flex items-center gap-1.5 px-1 text-[11px] font-extrabold uppercase tracking-wide" style={{ color: "var(--muted-foreground)" }}>
                  <CloudSun className="size-3.5" aria-hidden="true" /> Proveedor del clima
                </p>
                <div role="radiogroup" aria-label="Proveedor del clima" className="mt-2 grid grid-cols-2 gap-1.5">
                  {PROVIDER_OPTIONS.map((option) => {
                    const selected = weatherProvider.provider === option.value;
                    return (
                      <button
                        key={option.value}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        disabled={weatherProvider.provider === null || weatherProvider.saving}
                        onClick={() => void weatherProvider.choose(option.value)}
                        className="min-h-9 rounded-xl border-2 px-2 text-[12.5px] font-extrabold disabled:opacity-60"
                        style={
                          selected
                            ? { background: "var(--primary)", color: "var(--primary-foreground)", borderColor: "var(--primary)" }
                            : { borderColor: "var(--border)" }
                        }
                      >
                        {option.label}
                      </button>
                    );
                  })}
                </div>
                {weatherProvider.error && (
                  <p role="alert" className="mt-1.5 px-1 text-[11px] font-bold" style={{ color: "var(--destructive)" }}>
                    {weatherProvider.error}
                  </p>
                )}
              </div>
              <div className="lg:hidden mt-2 pb-2 border-b" style={{ borderColor: "var(--border)" }}>
                {isAdmin && (
                  <button type="button" role="menuitem" className="flex min-h-11 w-full items-center gap-2 rounded-xl px-3 text-left text-sm font-extrabold hover:bg-muted focus-visible:ring-2 focus-visible:ring-[var(--ring)]" onClick={() => { setProfileOpen(false); router.push("/estadisticas"); }}>
                    <BarChart3 className="size-4" /> Estadísticas
                  </button>
                )}
                <button type="button" role="menuitem" className="flex min-h-11 w-full items-center gap-2 rounded-xl px-3 text-left text-sm font-extrabold hover:bg-muted focus-visible:ring-2 focus-visible:ring-[var(--ring)]" onClick={() => { setProfileOpen(false); onBellClick(); }}>
                  <Bell className="size-4" /> Notificaciones
                  {unread > 0 && <span className="w-2 h-2 rounded-full" style={{ background: "var(--rose-ink)" }} />}
                </button>
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

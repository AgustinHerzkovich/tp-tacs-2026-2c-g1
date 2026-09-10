"use client";

import { Bell } from "lucide-react";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/hooks/useAuth";
import { getInitials } from "@/lib/initials";

interface HeaderProps {
  onBellClick: () => void;
  unread?: number;
}

export function Header({ onBellClick, unread = 0 }: HeaderProps) {
  const { user, logout } = useAuth();

  const handleAvatarClick = () => {
    void logout();
  };

  return (
    <div
      className="sticky top-0 z-20 backdrop-blur px-5 pt-6 pb-3 flex items-center justify-between"
      style={{ background: "rgba(255,248,242,.9)" }}
    >
      <h1 className="font-display font-semibold text-[26px]" style={{ color: "var(--primary)" }}>
        Planazo
      </h1>
      <div className="flex items-center gap-3">
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
        <button onClick={handleAvatarClick} aria-label="Cerrar sesión" className="tap">
          <Avatar>
            <AvatarFallback className="font-display font-semibold text-[13px]" style={{ background: "var(--lav)", color: "var(--lav-ink)" }}>
              {user ? getInitials(user.name) : "?"}
            </AvatarFallback>
          </Avatar>
        </button>
      </div>
    </div>
  );
}

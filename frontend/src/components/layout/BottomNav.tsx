"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const TABS = [
  { href: "/explorar", label: "Explorar", icon: "🧭" },
  { href: "/mis-actividades", label: "Mis Actividades", icon: "⚡" },
] as const;

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="px-3 pb-3 pt-2 flex gap-2 border-t-2 lg:px-5" style={{ background: "var(--background)", borderColor: "var(--border)" }} aria-label="Navegación principal">
      {TABS.map((tab) => {
        const active = pathname === tab.href;
        return (
          <Link
            key={tab.href}
            href={tab.href}
            aria-current={active ? "page" : undefined}
            className="tap flex-1 flex items-center justify-center gap-2 py-3 rounded-full font-display font-extrabold text-[13px] transition-colors"
            style={
              active
                ? { background: "var(--primary)", color: "var(--primary-foreground)", boxShadow: "0 4px 0 #a8330c" }
                : { background: "#fff", color: "var(--muted-foreground)", boxShadow: "0 2px 8px rgba(58,51,82,.1)" }
            }
          >
            <span className="emoji-3d text-[17px]">{tab.icon}</span>
            {tab.label}
          </Link>
        );
      })}
    </nav>
  );
}

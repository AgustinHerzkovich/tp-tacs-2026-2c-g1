"use client";

import type { ReactNode } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { BottomNav } from "@/components/layout/BottomNav";
import { useRequireAuth } from "@/hooks/useRequireAuth";

/** Persistent bottom chrome, fixed to the viewport (not just "last in the
 * flex column", which scrolled away on tall pages) so it never scrolls out
 * of view. The tab bar shows on every screen except the creation wizard
 * itself (`/crear` lives outside this group on purpose). The "Crear Nueva
 * Actividad" CTA is scoped to Mis Actividades only — it doesn't make sense
 * while browsing Explorar or looking at one activity's detail.
 *
 * Also gates the whole group behind login — Explorar, Mis Actividades and
 * the activity detail page all require a mock session. */
export default function BottomChromeLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const showCreateButton = pathname === "/mis-actividades";
  const user = useRequireAuth();

  if (!user) return null;

  return (
    <>
      {/* Reserve space so scrolled content never ends up hidden behind the fixed bar below. */}
      <div className={showCreateButton ? "pb-36" : "pb-20"}>{children}</div>

      <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] z-30" style={{ background: "var(--background)" }}>
        {showCreateButton && (
          <div className="px-4 pt-3 pb-3 border-t-2" style={{ borderColor: "var(--border)" }}>
            <Button
              asChild
              className="w-full h-auto py-3.5 rounded-2xl text-[15px] font-display font-semibold shadow-[0_16px_30px_-12px_rgba(255,107,71,.55)]"
            >
              <Link href="/crear">✨ Crear Nueva Actividad</Link>
            </Button>
          </div>
        )}
        <BottomNav />
      </div>
    </>
  );
}

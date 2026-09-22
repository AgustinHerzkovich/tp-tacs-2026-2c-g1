"use client";

import type { ReactNode } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { Plus } from "lucide-react";
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
 * Also gates the whole group behind Keycloak authentication. */
export default function BottomChromeLayout({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const showCreateButton = pathname === "/mis-actividades";
  const user = useRequireAuth();

  if (!user) return null;

  return (
    <>
      {/* Reserve space so scrolled content never ends up hidden behind the fixed bar below. */}
      <div className={showCreateButton ? "pb-36" : "pb-20"}>{children}</div>

      <div className="fixed bottom-0 inset-x-0 z-30 flex flex-col items-center gap-3">
        {showCreateButton && (
          <Button asChild size="xl" className="gap-2">
            <Link href="/crear">
              <Plus className="size-[18px]" /> Crear actividad
            </Link>
          </Button>
        )}
        <div
          className="w-full overflow-hidden lg:max-w-3xl lg:mx-auto lg:rounded-t-3xl lg:shadow-[0_-8px_30px_-8px_rgba(58,51,82,.25)]"
          style={{ background: "var(--background)" }}
        >
          <BottomNav />
        </div>
      </div>
    </>
  );
}

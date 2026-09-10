import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Fredoka, Nunito } from "next/font/google";
import { StoreProvider } from "@/store/StoreProvider";
import "./globals.css";

const fredoka = Fredoka({
  variable: "--font-display",
  weight: ["500", "600", "700"],
  subsets: ["latin"],
});

const nunito = Nunito({
  variable: "--font-body",
  weight: ["400", "500", "600", "700", "800"],
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Planazo",
  description: "Planificá actividades grupales con reglas climáticas y votación de fechas.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es" className={`${fredoka.variable} ${nunito.variable} h-full antialiased`}>
      <body className="min-h-screen flex flex-col items-center" style={{ background: "#f3ece3" }}>
        <StoreProvider>
          {/* Mobile-only shell: the app always renders as a fixed-width phone
              column, centered, even on a desktop-wide browser window. */}
          <div className="w-full max-w-[430px] min-h-screen flex flex-col" style={{ background: "var(--background)" }}>
            {children}
          </div>
        </StoreProvider>
      </body>
    </html>
  );
}

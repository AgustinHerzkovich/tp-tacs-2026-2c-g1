import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Nunito } from "next/font/google";
import localFont from "next/font/local";
import { StoreProvider } from "@/store/StoreProvider";
import { TopProgressBar } from "@/components/layout/TopProgressBar";
import { ToastProvider } from "@/components/common/ToastProvider";
import "./globals.css";

const beautySmile = localFont({
  src: "../assets/fonts/beauty-smile.otf",
  variable: "--font-brand",
  display: "swap",
});

const nunito = Nunito({
  variable: "--font-body",
  weight: ["400", "500", "600", "700", "800"],
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: { default: "Planazo", template: "%s | Planazo" },
  description: "Planificá actividades grupales con reglas climáticas y votación de fechas.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    // `es-AR` (and not the generic `es`) because Chromium picks the format of the
    // native date/time pickers — the votation alternatives, the reprogramming
    // window and the statistics range — from the document language: es-AR gives
    // 24h times and dd/mm/yyyy, while a plain `es` follows the browser's region
    // and can end up on 12h with AM/PM.
    <html lang="es-AR" className={`${beautySmile.variable} ${nunito.variable} h-full antialiased`}>
      <body className="min-h-screen">
        <StoreProvider>
          <ToastProvider>
            <div className="w-full min-h-screen flex flex-col">
              <TopProgressBar />
              {children}
            </div>
          </ToastProvider>
        </StoreProvider>
      </body>
    </html>
  );
}

import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Fredoka, Nunito } from "next/font/google";
import { StoreProvider } from "@/store/StoreProvider";
import { TopProgressBar } from "@/components/layout/TopProgressBar";
import { ToastProvider } from "@/components/common/ToastProvider";
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
  title: { default: "Planazo", template: "%s | Planazo" },
  description: "Planificá actividades grupales con reglas climáticas y votación de fechas.",
  icons: { icon: "/favicon.ico" },
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="es" className={`${fredoka.variable} ${nunito.variable} h-full antialiased`}>
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

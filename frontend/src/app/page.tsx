import type { Metadata } from "next";
import { LandingPage } from "@/components/pages/LandingPage";

export const metadata: Metadata = { title: "Organizá tu próximo plan" };

export default function Page() {
  return <LandingPage />;
}

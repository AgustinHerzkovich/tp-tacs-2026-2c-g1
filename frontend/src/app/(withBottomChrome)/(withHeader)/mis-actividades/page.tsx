import type { Metadata } from "next";
import { MisActividadesPage } from "@/components/pages/MisActividadesPage";

export const metadata: Metadata = { title: "Mis actividades" };

export default function Page() {
  return <MisActividadesPage />;
}

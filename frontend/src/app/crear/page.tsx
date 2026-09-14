import type { Metadata } from "next";
import { CrearActividadPage } from "@/components/pages/CrearActividadPage";

export const metadata: Metadata = { title: "Crear actividad" };

export default function Page() {
  return <CrearActividadPage />;
}

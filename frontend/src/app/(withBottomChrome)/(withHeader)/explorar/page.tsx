import type { Metadata } from "next";
import { ExplorarPage } from "@/components/pages/ExplorarPage";

export const metadata: Metadata = { title: "Explorar actividades" };

export default function Page() {
  return <ExplorarPage />;
}

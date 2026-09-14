import type { Metadata } from "next";
import { ActivityDetailPage } from "@/components/pages/ActivityDetailPage";

export const metadata: Metadata = { title: "Detalle de actividad" };

export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ActivityDetailPage id={id} />;
}

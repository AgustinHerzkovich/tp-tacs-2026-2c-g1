import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function NotFound() {
  return <main className="min-h-screen flex flex-col items-center justify-center text-center p-6"><h1 className="font-display text-3xl font-semibold">No encontramos esa página</h1><p className="mt-2 mb-5 font-bold" style={{ color: "var(--muted-foreground)" }}>Puede que el enlace haya cambiado o ya no exista.</p><Button asChild><Link href="/mis-actividades">Volver a mis actividades</Link></Button></main>;
}

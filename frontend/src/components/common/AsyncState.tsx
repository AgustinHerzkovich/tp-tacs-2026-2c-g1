import { AlertTriangle, LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

export function LoadingState({ label = "Cargando..." }: { label?: string }) {
  return <div className="flex items-center justify-center gap-2 py-12 font-bold" aria-live="polite" style={{ color: "var(--muted-foreground)" }}><LoaderCircle className="size-5 animate-spin" />{label}</div>;
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return <div className="rounded-2xl border-2 p-5 text-center" role="alert" style={{ borderColor: "var(--rose)", background: "#fff" }}><AlertTriangle className="size-6 mx-auto mb-2" style={{ color: "var(--destructive)" }} /><p className="text-sm font-extrabold">{message}</p>{retry && <Button variant="outline" className="mt-3 rounded-xl" onClick={retry}>Reintentar</Button>}</div>;
}

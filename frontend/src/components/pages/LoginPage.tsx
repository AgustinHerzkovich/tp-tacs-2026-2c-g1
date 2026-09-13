"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/hooks/useAuth";

export function LoginPage() {
  const router = useRouter();
  const { initialized, isAuthenticated, login } = useAuth();

  useEffect(() => {
    if (isAuthenticated) router.replace("/mis-actividades");
  }, [isAuthenticated, router]);

  return (
    <div className="fade-in min-h-screen flex flex-col justify-center px-6 py-10 lg:max-w-xl lg:mx-auto lg:w-full">
      <div className="text-center mb-8">
        <h1 className="font-display font-semibold text-4xl" style={{ color: "var(--primary)" }}>
          Planazo
        </h1>
        <p className="font-display font-semibold text-lg mt-2">Organizá tu próximo plan</p>
        <p className="text-[13px] font-bold mt-1" style={{ color: "var(--muted-foreground)" }}>
          Iniciá sesión o creá tu cuenta de forma segura con Keycloak.
        </p>
      </div>

      <Button
        className="h-auto py-3.5 rounded-2xl font-display font-semibold"
        disabled={!initialized || isAuthenticated}
        onClick={() => void login()}
      >
        {initialized ? "Ingresar o registrarme" : "Comprobando sesión..."}
      </Button>
    </div>
  );
}

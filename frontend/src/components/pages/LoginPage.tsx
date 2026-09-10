"use client";

import { useRouter } from "next/navigation";
import { Card } from "@/components/ui/card";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useAuth } from "@/hooks/useAuth";
import { MOCK_USERS } from "@/data/mockUsers";
import { AV_COLORS } from "@/data/mockData";
import { getInitials } from "@/lib/initials";
import type { UserDTO } from "@/types/backend";

export function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();

  const handleSelect = (user: UserDTO) => {
    login(user);
    router.push("/mis-actividades");
  };

  return (
    <div className="fade-in min-h-screen flex flex-col justify-center px-6 py-10">
      <div className="text-center mb-8">
        <h1 className="font-display font-semibold text-4xl" style={{ color: "var(--primary)" }}>
          Planazo
        </h1>
        <p className="font-display font-semibold text-lg mt-2">¿Quién sos?</p>
        <p className="text-[13px] font-bold mt-1" style={{ color: "var(--muted-foreground)" }}>
          Elegí un perfil para entrar (login mock, todavía sin backend real).
        </p>
      </div>

      <div className="space-y-3">
        {MOCK_USERS.map((user, i) => (
          <Card
            key={user.id}
            onClick={() => handleSelect(user)}
            className="tap p-3.5 flex flex-row items-center gap-3.5 rounded-2xl cursor-pointer"
          >
            <Avatar size="lg">
              <AvatarFallback
                className="font-display font-bold text-[15px]"
                style={{ background: AV_COLORS[i % AV_COLORS.length] }}
              >
                {getInitials(user.name)}
              </AvatarFallback>
            </Avatar>
            <div className="min-w-0 flex-1">
              <p className="font-display font-semibold text-[15px] truncate">{user.name}</p>
              <p className="text-[11.5px] font-bold" style={{ color: "var(--muted-foreground)" }}>
                {user.id}
              </p>
            </div>
            <span className="font-display font-semibold text-[13px]" style={{ color: "var(--primary)" }}>
              Entrar →
            </span>
          </Card>
        ))}
      </div>
    </div>
  );
}

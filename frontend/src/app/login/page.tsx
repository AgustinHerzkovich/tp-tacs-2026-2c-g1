import type { Metadata } from "next";
import { LoginPage } from "@/components/pages/LoginPage";

export const metadata: Metadata = { title: "Ingresar" };

export default function Page() {
  return <LoginPage />;
}

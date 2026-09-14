"use client";

import { createContext, useContext, useState, type ReactNode } from "react";

type ToastKind = "success" | "error";

interface ToastMessage {
  id: number;
  text: string;
  kind: ToastKind;
}

const ToastContext = createContext<(text: string, kind?: ToastKind) => void>(() => undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [messages, setMessages] = useState<ToastMessage[]>([]);

  const notify = (text: string, kind: ToastKind = "success") => {
    const id = Date.now() + Math.random();
    setMessages((current) => [...current, { id, text, kind }]);
    window.setTimeout(() => setMessages((current) => current.filter((message) => message.id !== id)), 3500);
  };

  return (
    <ToastContext value={notify}>
      {children}
      <div className="fixed inset-x-4 bottom-24 z-[100] flex flex-col items-center gap-2 lg:bottom-6" aria-live="polite" aria-atomic="true">
        {messages.map((message) => (
          <div key={message.id} role={message.kind === "error" ? "alert" : "status"} className="max-w-md rounded-2xl border-2 bg-white px-4 py-3 text-sm font-extrabold shadow-xl" style={{ borderColor: message.kind === "error" ? "var(--destructive)" : "var(--primary)" }}>
            {message.text}
          </div>
        ))}
      </div>
    </ToastContext>
  );
}

export function useToast() {
  return useContext(ToastContext);
}

"use client";

import { useSyncExternalStore } from "react";
import { getPendingRequests, subscribeLoading } from "@/lib/loading";

export function TopProgressBar() {
  const pending = useSyncExternalStore(subscribeLoading, getPendingRequests, getPendingRequests);
  const showing = pending > 0;

  return (
    <>
      {showing && <div aria-hidden className="fixed inset-0 z-40 cursor-wait bg-[rgba(58,51,82,.25)]" />}
      <div
        aria-hidden
        className={`pointer-events-none fixed top-0 left-1/2 z-50 h-[3px] w-full max-w-[430px] -translate-x-1/2 overflow-hidden transition-opacity duration-200 ${
          showing ? "opacity-100" : "opacity-0"
        }`}
      >
        <div
          className={`h-full w-[40%] rounded-full ${showing ? "progress-indeterminate" : ""}`}
          style={{ background: "var(--primary)", boxShadow: "0 0 8px rgba(255,107,71,.6)" }}
        />
      </div>
    </>
  );
}
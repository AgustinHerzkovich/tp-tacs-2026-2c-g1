"use client";

import { useState, type ReactNode } from "react";
import { Provider } from "react-redux";
import { AuthBootstrap } from "@/auth/AuthBootstrap";
import { makeStore, type AppStore } from "@/store/store";

/** Creates one Redux store per browser session and initializes OIDC below it. */
export function StoreProvider({ children }: { children: ReactNode }) {
  const [store] = useState<AppStore>(() => makeStore());

  return (
    <Provider store={store}>
      <AuthBootstrap />
      {children}
    </Provider>
  );
}

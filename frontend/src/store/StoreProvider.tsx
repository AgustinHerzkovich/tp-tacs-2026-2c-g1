"use client";

import { useEffect, useState, type ReactNode } from "react";
import { Provider } from "react-redux";
import { makeStore, type AppStore } from "@/store/store";
import { hydrate } from "@/store/session/sessionSlice";
import { readPersistedUser } from "@/store/session/sessionPersistence";

/** One Redux store per client session (created lazily via useState's
 * initializer, not a ref, so it stays safe under the React Compiler) — the
 * pattern Next.js's App Router needs so server-rendered requests never share
 * store state across users. */
export function StoreProvider({ children }: { children: ReactNode }) {
  const [store] = useState<AppStore>(() => makeStore());

  // Runs once, client-only, after the first render — so the server-rendered
  // HTML and the client's first paint always agree (both start logged out /
  // not-yet-hydrated), and only *then* do we check localStorage and restore
  // a persisted mock session. See sessionSlice's `hydrated` flag for why this
  // has to be a separate step from the store's initial state.
  useEffect(() => {
    store.dispatch(hydrate(readPersistedUser()));
  }, [store]);

  return <Provider store={store}>{children}</Provider>;
}

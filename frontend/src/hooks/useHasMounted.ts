"use client";

import { useSyncExternalStore } from "react";

const noopSubscribe = () => () => {};

/** True only once hydration has committed. The server always renders the
 * pre-hydration state (it can't know client-only state like Keycloak's SSO
 * status), so this keeps the very first client render identical to that —
 * otherwise state resolved before/during hydration (e.g. a session restored
 * right after a Keycloak redirect back to the page) makes React's first
 * client pass disagree with the server HTML and throws a hydration-mismatch
 * error. */
export function useHasMounted(): boolean {
  return useSyncExternalStore(noopSubscribe, () => true, () => false);
}

"use client";

import { useState } from "react";

export interface UseJoinActivity {
  joined: boolean;
  confirmOpen: boolean;
  requestJoin: () => void;
  cancelJoin: () => void;
  confirmJoin: () => void;
  leave: () => void;
}

/** Join/leave state for an activity's sticky action button, gated by a confirm dialog. */
export function useJoinActivity(): UseJoinActivity {
  const [joined, setJoined] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const requestJoin = () => setConfirmOpen(true);
  const cancelJoin = () => setConfirmOpen(false);
  const confirmJoin = () => {
    setJoined(true);
    setConfirmOpen(false);
  };
  const leave = () => setJoined(false);

  return { joined, confirmOpen, requestJoin, cancelJoin, confirmJoin, leave };
}

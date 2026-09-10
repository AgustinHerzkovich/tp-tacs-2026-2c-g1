"use client";

import { useState } from "react";
import { api } from "@/lib/api";

export interface UseJoinActivity {
  joined: boolean;
  confirmOpen: boolean;
  pending: boolean;
  requestJoin: () => void;
  cancelJoin: () => void;
  confirmJoin: () => Promise<void>;
  leave: () => Promise<void>;
}

/** Join/leave for one activity's sticky action button, backed by
 * PUT/DELETE /api/activities/:id/participants/me. */
export function useJoinActivity(activityId: string, initialJoined: boolean): UseJoinActivity {
  const [joined, setJoined] = useState(initialJoined);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pending, setPending] = useState(false);

  const requestJoin = () => setConfirmOpen(true);
  const cancelJoin = () => setConfirmOpen(false);

  const confirmJoin = async () => {
    setPending(true);
    try {
      await api.activities.join(activityId);
      setJoined(true);
    } finally {
      setPending(false);
      setConfirmOpen(false);
    }
  };

  const leave = async () => {
    setPending(true);
    try {
      await api.activities.leave(activityId);
      setJoined(false);
    } finally {
      setPending(false);
    }
  };

  return { joined, confirmOpen, pending, requestJoin, cancelJoin, confirmJoin, leave };
}

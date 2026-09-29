"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { formatActivityWhen } from "@/lib/formatDate";
import type { VotationDTO } from "@/types/backend";

export interface VoteOptionView {
  /** The option's raw ISO LocalDateTime — also what the backend expects as
   * the vote's body (see PUT /votations/:id/votes/me). */
  id: string;
  label: string;
  votes: number;
}

export interface UseVoting {
  votation: VotationDTO | null;
  loading: boolean;
  error: string | null;
  options: VoteOptionView[];
  total: number;
  selectedId: string | null;
  votedId: string | null;
  selectedOption: VoteOptionView | undefined;
  confirmOpen: boolean;
  pending: boolean;
  select: (id: string) => void;
  requestVote: () => void;
  cancelVote: () => void;
  confirmVote: () => Promise<void>;
  updateOptions: (dates: string[]) => Promise<void>;
  updateSettings: (minQuorum: number, durationHours: number) => Promise<void>;
}

/** Finds and drives the reprogramming vote for one activity: the most recent
 * votation of that activity (GET /votations?activityId=…, newest first). The
 * backend only returns votations of activities the current user organizes or
 * joined, so `votation` stays null for anyone else.
 *
 * `votedId` comes from the backend's `votedOption`, computed from the
 * authenticated user's id, so it survives reloads and never confuses two
 * users that share a display name. */
export function useVoting(activityId: string): UseVoting {
  const [votation, setVotation] = useState<VotationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pending, setPending] = useState(false);

  const load = useCallback(() => {
    api.votations
      .mine({ activityId, size: 1 })
      .then((page) => {
        setVotation(page.content[0] ?? null);
        setError(null);
      })
      .catch((err: unknown) => setError(err instanceof Error ? err.message : "No pudimos cargar la votación."))
      .finally(() => setLoading(false));
  }, [activityId]);

  useEffect(() => {
    load();
  }, [load]);

  const options: VoteOptionView[] = useMemo(
    () =>
      (votation?.options ?? []).map((option) => ({
        id: option.dateTime,
        label: formatActivityWhen(option.dateTime),
        votes: option.voteCount,
      })),
    [votation],
  );
  const total = options.reduce((sum, option) => sum + option.votes, 0);
  const selectedOption = options.find((option) => option.id === selectedId);

  const votedId = votation?.votedOption ?? null;

  const select = (id: string) => {
    setSelectedId(id);
  };
  const requestVote = () => {
    if (selectedId) setConfirmOpen(true);
  };
  const cancelVote = () => setConfirmOpen(false);

  const confirmVote = async () => {
    if (!votation || !selectedId || pending) return;
    setPending(true);
    try {
      const updated = await api.votations.vote(votation.id, selectedId);
      setVotation(updated);
      setConfirmOpen(false);
    } finally {
      setPending(false);
    }
  };

  const updateOptions = async (dates: string[]) => {
    if (!votation) return;
    setVotation(await api.votations.updateOptions(votation.id, { dates }));
  };

  const updateSettings = async (minQuorum: number, durationHours: number) => {
    if (!votation) return;
    setVotation(await api.votations.updateSettings(votation.id, { minQuorum, duration: `PT${durationHours}H` }));
  };

  return {
    votation,
    loading,
    error,
    options,
    total,
    selectedId,
    votedId,
    selectedOption,
    confirmOpen,
    pending,
    select,
    requestVote,
    cancelVote,
    confirmVote,
    updateOptions,
    updateSettings,
  };
}

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
  select: (id: string) => void;
  requestVote: () => void;
  cancelVote: () => void;
  confirmVote: () => Promise<void>;
}

/** Finds and drives the reprogramming vote for one activity. `/votations` only
 * returns votations the backend's current identity organizes or joined (see
 * AGENTS.md re: no real auth yet), so this activity's votation may not
 * appear if it's not "mine" — `votation` stays null in that case. */
export function useVoting(activityId: string): UseVoting {
  const [votation, setVotation] = useState<VotationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [votedId, setVotedId] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const load = useCallback(() => {
    api.votations
      .mine()
      .then((all) => {
        setVotation(all.find((v) => v.activityId === activityId) ?? null);
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

  const select = (id: string) => {
    if (!votedId) setSelectedId(id);
  };
  const requestVote = () => {
    if (selectedId) setConfirmOpen(true);
  };
  const cancelVote = () => setConfirmOpen(false);

  const confirmVote = async () => {
    if (!votation || !selectedId) return;
    await api.votations.vote(votation.id, selectedId);
    setVotedId(selectedId);
    setConfirmOpen(false);
    load();
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
    select,
    requestVote,
    cancelVote,
    confirmVote,
  };
}

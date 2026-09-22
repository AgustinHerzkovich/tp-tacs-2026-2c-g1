"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { formatActivityWhen } from "@/lib/formatDate";
import type { UserDTO, VotationDTO } from "@/types/backend";

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

/** Finds and drives the reprogramming vote for one activity. GET /votations
 * only returns votations the current identity organizes or joined, so this
 * activity's votation may not appear if it's not "mine" — `votation` stays
 * null in that case.
 *
 * `currentUser`, when given, lets `votedId` be reconstructed from the
 * votation's own data (matching the identity against each option's
 * `voterNames`) so a previously-cast vote still shows as selected after a
 * page reload — the API has no other "did I already vote" signal. */
export function useVoting(activityId: string, currentUser?: Pick<UserDTO, "id" | "name"> | null): UseVoting {
  const [votation, setVotation] = useState<VotationDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [votedId, setVotedId] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pending, setPending] = useState(false);

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

  const persistedVotedId = useMemo(() => {
    if (!votation || !currentUser) return null;
    const voted = votation.options.find(
      (option) => option.voterNames.includes(currentUser.name) || option.voterNames.includes(currentUser.id),
    );
    return voted?.dateTime ?? null;
  }, [votation, currentUser]);
  const effectiveVotedId = persistedVotedId ?? votedId;

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
      setVotedId(selectedId);
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
    votedId: effectiveVotedId,
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

"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import { compareLocalDateTimes, formatActivityWhen } from "@/lib/formatDate";
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
  updateOptions: (dates: string[], allowVoteLoss?: boolean) => Promise<void>;
  updateSettings: (minQuorum: number, durationHours: number) => Promise<void>;
  /** Re-requests the votation. Needed after joining or leaving, because the
   * backend only lists votations of activities the user takes part in: without
   * this the card stays empty (or stale) until the page is opened again. */
  refresh: () => void;
}

/** Finds and drives the reprogramming vote for one activity: the most recent
 * votation of that activity (GET /votations?activityId=…, newest first). The
 * backend only returns votations of activities the current user organizes or
 * joined, so `votation` stays null for anyone else.
 *
 * `votedId` comes from the backend's `votedOption`, computed from the
 * authenticated user's id, so it survives reloads and never confuses two
 * users that share a display name.
 *
 * `updateOptions` replaces the whole alternative list. Dropping an
 * alternative that already carries votes needs `allowVoteLoss`, which the
 * caller passes only after the organizer confirmed the loss; without it the
 * backend rejects the request with `VOTATION_VOTES_AT_RISK`. */
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
      (votation?.options ?? [])
        .map((option) => ({
          id: option.dateTime,
          label: formatActivityWhen(option.dateTime),
          votes: option.voteCount,
        }))
        // Earliest date first, so the alternatives always read as a calendar
        // instead of the order the backend happened to store them in. Sorting
        // here covers every screen that lists them (the activity detail above
        // all), instead of each one sorting on its own.
        .sort((a, b) => compareLocalDateTimes(a.id, b.id)),
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

  const updateOptions = async (dates: string[], allowVoteLoss?: boolean) => {
    if (!votation) return;
    setVotation(await api.votations.updateOptions(votation.id, { dates, allowVoteLoss }));
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
    refresh: load,
  };
}

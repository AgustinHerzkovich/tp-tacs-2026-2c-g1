"use client";

import { useMemo, useState } from "react";
import { VOTE_OPTIONS_INIT } from "@/data/mockData";
import type { VoteOption } from "@/types/domain";

export interface UseVoting {
  options: VoteOption[];
  total: number;
  selectedId: string | null;
  votedId: string | null;
  selectedOption: VoteOption | undefined;
  confirmOpen: boolean;
  select: (id: string) => void;
  requestVote: () => void;
  cancelVote: () => void;
  confirmVote: () => void;
}

/**
 * Voting-room state for an activity's reprogramming options: which option is
 * selected, which one was actually voted, running tallies, and the
 * confirm-before-voting dialog. Casting a vote is a two-step flow: selecting
 * an option arms it, `confirmVote` commits it (after the user confirms in the
 * dialog opened by `requestVote`).
 */
export function useVoting(initialOptions: VoteOption[] = VOTE_OPTIONS_INIT): UseVoting {
  const [options, setOptions] = useState<VoteOption[]>(initialOptions);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [votedId, setVotedId] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const total = useMemo(() => options.reduce((sum, o) => sum + o.votes, 0), [options]);
  const selectedOption = options.find((o) => o.id === selectedId);

  const select = (id: string) => {
    if (!votedId) setSelectedId(id);
  };

  const requestVote = () => {
    if (selectedId) setConfirmOpen(true);
  };

  const cancelVote = () => setConfirmOpen(false);

  const confirmVote = () => {
    setOptions((opts) => opts.map((o) => (o.id === selectedId ? { ...o, votes: o.votes + 1 } : o)));
    setVotedId(selectedId);
    setConfirmOpen(false);
  };

  return {
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

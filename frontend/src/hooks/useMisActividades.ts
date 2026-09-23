"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toMisActivity } from "@/lib/activityMapping";
import { useAuth } from "@/hooks/useAuth";
import type { VotationDTO } from "@/types/backend";
import type { MisActivity } from "@/types/domain";

const PAGE_SIZE = 12;

export interface PendingVote extends MisActivity {
  /** Whether the current user organizes this activity (vs. only joined it)
   * — the pending-vote card is the one place both roles mix together, so it
   * needs its own tag to disambiguate (the two feeds below don't, since the
   * section itself already says which role it is). */
  isOrganizer: boolean;
}

interface UseMisActividades {
  organizedFeed: MisActivity[];
  organizedTotal: number;
  organizedPage: number;
  organizedTotalPages: number;
  setOrganizedPage: (page: number) => void;
  joinedFeed: MisActivity[];
  joinedTotal: number;
  joinedPage: number;
  joinedTotalPages: number;
  setJoinedPage: (page: number) => void;
  /** Activities with a reprogramming vote that's still open and that the
   * current user hasn't cast yet — see the module doc for how that's
   * determined. */
  votingPending: PendingVote[];
  loading: boolean;
  error: string | null;
  refresh: () => void;
}

function hasVoted(votation: VotationDTO, user: { id: string; name: string } | null): boolean {
  if (!user) return false;
  return votation.options.some(
    (option) => option.voterNames.includes(user.name) || option.voterNames.includes(user.id),
  );
}

/** "Mis actividades" data, split by role instead of merged: activities the
 * user organizes (GET /activities/organizers/me) vs. activities they only
 * joined as a participant (GET /activities/participants/me), each with its
 * own page. `ActivityResponse` carries no organizer id of its own, so an
 * activity that shows up in BOTH pages (the organizer also joined their own
 * activity) is kept only in `organizedFeed` — the same "am I the organizer"
 * signal ActivityDetailPage.tsx uses. This dedup only looks at whichever
 * page of each feed is currently loaded, same as the pagination itself.
 *
 * `votingPending` cross-references those loaded activities against
 * GET /votations (api.votations.mine(), which is NOT paginated — it always
 * returns every votation the user organizes or joined) to keep only
 * activities whose reprogramming vote is still `ACTIVE` and that the
 * current user hasn't voted in yet, via the same voterNames match
 * useVoting.ts uses on the detail page. A CLOSED votation (see
 * VotationClosingScheduler on the backend) or one the user already voted in
 * is excluded — that's the fix over the old behavior, which only checked
 * the activity's status and kept showing it after the user voted. */
export function useMisActividades(): UseMisActividades {
  const { user } = useAuth();
  const [organizedFeed, setOrganizedFeed] = useState<MisActivity[]>([]);
  const [organizedTotal, setOrganizedTotal] = useState(0);
  const [organizedPage, setOrganizedPage] = useState(0);
  const [organizedTotalPages, setOrganizedTotalPages] = useState(0);
  const [joinedFeed, setJoinedFeed] = useState<MisActivity[]>([]);
  const [joinedTotal, setJoinedTotal] = useState(0);
  const [joinedPage, setJoinedPage] = useState(0);
  const [joinedTotalPages, setJoinedTotalPages] = useState(0);
  const [votations, setVotations] = useState<VotationDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadedRequest, setLoadedRequest] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const requestKey = [organizedPage, joinedPage, reloadKey].join("|");
  const requestPending = loading || loadedRequest !== requestKey;

  useEffect(() => {
    let cancelled = false;

    Promise.all([
      api.activities.organized(organizedPage, PAGE_SIZE),
      api.activities.mine(joinedPage, PAGE_SIZE),
      api.votations.mine(),
    ])
      .then(([organized, joined, myVotations]) => {
        if (cancelled) return;
        const organizedIds = new Set(organized.content.map((a) => a.id));
        const joinedOnly = joined.content.filter((a) => !organizedIds.has(a.id));
        // totalElements comes from the raw, un-deduped /participants/me page — subtract
        // however many of this page's items got folded into "organized" so the count
        // badge doesn't contradict what's actually on screen (still an approximation
        // for activities dedup'd on a page we haven't loaded).
        const dedupedOnThisPage = joined.content.length - joinedOnly.length;

        setOrganizedFeed(organized.content.map(toMisActivity));
        setOrganizedTotal(organized.totalElements);
        setOrganizedTotalPages(organized.totalPages);
        setJoinedFeed(joinedOnly.map(toMisActivity));
        setJoinedTotal(Math.max(0, joined.totalElements - dedupedOnThisPage));
        setJoinedTotalPages(joined.totalPages);
        setVotations(myVotations);
        setError(null);
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : "No pudimos cargar tus actividades.");
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
          setLoadedRequest(requestKey);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [organizedPage, joinedPage, reloadKey, requestKey]);

  const organizedIds = new Set(organizedFeed.map((a) => a.id));
  const votingPending: PendingVote[] = [...organizedFeed, ...joinedFeed].flatMap((activity) => {
    if (activity.status !== "propuesta") return [];
    const votation = votations.find((v) => v.activityId === activity.id);
    if (!votation || votation.status !== "ACTIVE" || hasVoted(votation, user)) return [];
    return [{ ...activity, isOrganizer: organizedIds.has(activity.id) }];
  });

  const refresh = useCallback(() => setReloadKey((k) => k + 1), []);

  return {
    organizedFeed,
    organizedTotal,
    organizedPage,
    organizedTotalPages,
    setOrganizedPage,
    joinedFeed,
    joinedTotal,
    joinedPage,
    joinedTotalPages,
    setJoinedPage,
    votingPending,
    loading: requestPending,
    error,
    refresh,
  };
}

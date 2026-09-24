"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { toMisActivity } from "@/lib/activityMapping";
import { useAuth } from "@/hooks/useAuth";
import type { ActivityResponse } from "@/types/backend";
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

/** How many pending votes "Te toca votar" shows at most. */
const PENDING_VOTES_SIZE = 20;

/** "Mis actividades" data, split by role instead of merged: activities the
 * user organizes (GET /activities/organizers/me) vs. activities they only
 * joined as a participant (GET /activities/participants/me), each with its
 * own page. An activity the organizer also joined is kept only in
 * `organizedFeed`, using the activity's `organizerId`.
 *
 * `votingPending` does not depend on those pages: it asks the backend for
 * the user's ACTIVE votations they have not voted in yet
 * (GET /votations?status=ACTIVE&votedByMe=false) and loads their PROPOSED
 * activities in a single GET /activities?ids=…, so a pending vote shows up
 * even if its activity is on another page. */
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
  const [pendingActivities, setPendingActivities] = useState<ActivityResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadedRequest, setLoadedRequest] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const userId = user?.id ?? null;
  const requestKey = [organizedPage, joinedPage, reloadKey, userId].join("|");
  const requestPending = loading || loadedRequest !== requestKey;

  useEffect(() => {
    let cancelled = false;

    const pendingVotes = api.votations
      .mine({ status: "ACTIVE", votedByMe: false, size: PENDING_VOTES_SIZE })
      .then(async (page) => {
        const ids = page.content.map((votation) => votation.activityId);
        // An empty `ids` filter would match every activity, so skip the call.
        if (ids.length === 0) return [];
        const activities = await api.activities.list({ ids, status: "PROPOSED", size: PENDING_VOTES_SIZE });
        return activities.content;
      });

    Promise.all([
      api.activities.organized(organizedPage, PAGE_SIZE),
      api.activities.mine(joinedPage, PAGE_SIZE),
      pendingVotes,
    ])
      .then(([organized, joined, pending]) => {
        if (cancelled) return;
        const joinedOnly = joined.content.filter((a) => a.organizerId !== userId);
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
        setPendingActivities(pending);
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
  }, [organizedPage, joinedPage, reloadKey, requestKey, userId]);

  const votingPending: PendingVote[] = pendingActivities.map((activity) => ({
    ...toMisActivity(activity),
    isOrganizer: activity.organizerId === userId,
  }));

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

import { EXPLORE_FEED, MIS_FEED, VOTING_PENDING, findActivity } from "@/data/mockData";
import type { Activity, ExploreActivity, MisActivity } from "@/types/domain";

interface UseActivities {
  exploreFeed: ExploreActivity[];
  misFeed: MisActivity[];
  votingPending: ExploreActivity;
  getActivity: (id: string) => Activity | undefined;
}

/**
 * Central read access to activity data. Backed by mock data for now; swap the
 * body for a real fetch/React Query call once the backend endpoint exists —
 * the pages and components consuming this hook won't need to change.
 */
export function useActivities(): UseActivities {
  return {
    exploreFeed: EXPLORE_FEED,
    misFeed: MIS_FEED,
    votingPending: VOTING_PENDING,
    getActivity: findActivity,
  };
}

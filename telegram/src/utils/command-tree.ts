export const COMMAND_TREE: Record<string, readonly string[]> = {
  "/seeActivities": [],
  "/myActivities": [
    "/checkActivityClimate",
    "/cancelActivity",
    "/vote",
    "/checkVotingResults",
  ],
  "/createActivity": [
    "/configureClimate",
    "/configureAnticipation",
    "/configureReprogramation",
  ],
};

export const COMMAND_PARENT: Record<string, string> = Object.fromEntries(
  Object.entries(COMMAND_TREE).flatMap(([parent, children]) =>
    children.map((child) => [child, parent])
  )
);
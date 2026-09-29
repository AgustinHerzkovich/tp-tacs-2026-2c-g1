// Removes the data created by loadtest/run.sh from a Planazo MongoDB database.
//
// Dry run (default): prints what would be deleted.
//   mongosh "<mongodb uri>" loadtest/cleanup.js
// Apply:
//   APPLY=1 mongosh "<mongodb uri>" loadtest/cleanup.js
// DB_NAME overrides the database when the URI does not name one.
//
// Load test activities are matched by both their title prefix and the fixed description that
// run.sh writes, so real activities are never selected by accident. Votations, notifications and
// statistics events of those activities are removed with them. Users are removed only when they
// organized a load test activity and are no longer referenced by any remaining document.

const LOADTEST_TITLE = /^Load test /;
const LOADTEST_DESCRIPTION = "Actividad creada por la prueba de carga";

const apply = process.env.APPLY === "1";
const target = process.env.DB_NAME ? db.getSiblingDB(process.env.DB_NAME) : db;

const activityFilter = { title: LOADTEST_TITLE, description: LOADTEST_DESCRIPTION };
const activities = target.activities.find(activityFilter, { _id: 1, organizer: 1 }).toArray();
const activityIds = activities.map((activity) => activity._id);
const organizerIds = [...new Set(activities.map((activity) => activity.organizer))];

const related = {
  votations: { activity: { $in: activityIds } },
  notifications: { activity: { $in: activityIds } },
  statistics_events: { activityId: { $in: activityIds } },
};

// A user stays if anything outside the load test data still points to it.
function stillReferenced(userId) {
  const outsideActivities = { _id: { $nin: activityIds } };
  return (
    target.activities.countDocuments({
      ...outsideActivities,
      $or: [{ organizer: userId }, { participants: userId }],
    }) > 0 ||
    target.votations.countDocuments({
      activity: { $nin: activityIds },
      "options.users": userId,
    }) > 0 ||
    target.notifications.countDocuments({
      activity: { $nin: activityIds },
      receiverUser: userId,
    }) > 0
  );
}
const removableUserIds = organizerIds.filter((userId) => !stillReferenced(userId));

print(`database: ${target.getName()} (${apply ? "APPLY" : "dry run"})`);
print(`activities: ${activityIds.length}`);
for (const [collection, filter] of Object.entries(related)) {
  print(`${collection}: ${target.getCollection(collection).countDocuments(filter)}`);
}
print(
  `users: ${removableUserIds.length} of ${organizerIds.length} load test organizers ` +
    "(the rest are still referenced and are kept)",
);

if (!apply) {
  print("Nothing deleted. Re-run with APPLY=1 to delete the documents above.");
} else {
  for (const [collection, filter] of Object.entries(related)) {
    target.getCollection(collection).deleteMany(filter);
  }
  target.activities.deleteMany({ _id: { $in: activityIds } });
  target.users.deleteMany({ _id: { $in: removableUserIds } });
  print("Deleted.");
}

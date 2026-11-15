// Fills the activities collection up to SEARCH_VOLUME documents for the search load test,
// so the search runs against a realistic amount of data. It clones the activities
// that already exist (seed or created by hand), so every clone has exactly the shape the backend
// maps, and varies what the search filters on: title, city, type, date and status.
// Clones carry `searchVolumeClone: true`; SEARCH_VOLUME=0 removes them all.
//
// Run it from the repository root (see docs/OBSERVABILIDAD.md):
//   docker compose exec -T -e SEARCH_VOLUME=50000 mongodb mongosh --quiet \
//     "mongodb://admin:password123@localhost:27017/mi_base_de_datos?authSource=admin" \
//     --eval "$(cat loadtest/search-volume.js)"

const target = Number(process.env.SEARCH_VOLUME ?? 50000);
const activities = db.getCollection("activities");
const marker = { searchVolumeClone: true };

if (target === 0) {
  const removed = activities.deleteMany(marker).deletedCount;
  print(`search-volume: removed ${removed} clones`);
  quit(0);
}

const originals = activities.find({ searchVolumeClone: { $exists: false } }).toArray();
if (originals.length === 0) {
  print("search-volume: there are no activities to clone; start the stack with APP_SEED_ENABLED=true");
  quit(1);
}

const existing = activities.countDocuments({});
const missing = target - existing;
if (missing <= 0) {
  print(`search-volume: ${existing} activities, nothing to add`);
  quit(0);
}

const words = ["Caminata", "Picnic", "Bicicleteada", "Fútbol", "Kayak", "Asado", "Yoga", "Trekking",
  "Running", "Fotografía", "Escalada", "Pesca", "Feria", "Concierto", "Taller", "Torneo"];
const places = ["Palermo", "Tigre", "Costanera", "Parque", "Sierras", "Lago", "Bosque", "Playa"];
const cities = [...new Set(originals.map((a) => a.location && a.location.city && a.location.city.name)
  .filter(Boolean))];
const types = ["OUTDOOR", "INDOOR", "MIXED"];
const statuses = ["CONFIRMED", "CONFIRMED", "CONFIRMED", "PROPOSED", "FINISHED", "CANCELLED"];
const now = Date.now();
const day = 24 * 60 * 60 * 1000;

const batchSize = 1000;
let batch = [];
for (let i = 0; i < missing; i++) {
  const original = originals[i % originals.length];
  const clone = { ...original };
  clone._id = crypto.randomUUID();
  clone.title = `${words[i % words.length]} en ${places[(i * 7) % places.length]} #${existing + i}`;
  clone.type = types[i % types.length];
  clone.status = statuses[i % statuses.length];
  clone.statusHistory = [clone.status];
  clone.dateTime = new Date(now + ((i % 120) - 30) * day + (i % 24) * 60 * 60 * 1000);
  clone.imageKeys = [];
  if (cities.length > 0 && clone.location) {
    clone.location = { ...clone.location, city: { ...clone.location.city, name: cities[i % cities.length] } };
  }
  clone.searchVolumeClone = true;
  batch.push(clone);
  if (batch.length === batchSize) {
    activities.insertMany(batch, { ordered: false });
    batch = [];
  }
}
if (batch.length > 0) activities.insertMany(batch, { ordered: false });

print(`search-volume: ${activities.countDocuments({})} activities (${missing} clones added from ${originals.length} originals)`);

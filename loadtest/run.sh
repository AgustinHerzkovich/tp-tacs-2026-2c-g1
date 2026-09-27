#!/usr/bin/env bash
# Load test for the Planazo backend using Vegeta.
#
# Usage: loadtest/run.sh <smoke|read|mixed> [rate ...]
#   loadtest/run.sh read            # default stages: 10/s 50/s 100/s
#   loadtest/run.sh mixed 20/s 40/s
#
# Every stage gets fresh tokens, runs `vegeta attack`, writes its reports under
# loadtest/results/<timestamp>-<scenario>/<rate>/ and is checked against the thresholds below.
# The script stops at the first failing stage and exits with a non-zero status.
#
# Locally it uses the `planazo-loadtest` client and loadtestNN users from
# keycloak/configure-local.sh. Any other target (e.g. GCP) is refused unless ALLOW_REMOTE=1 and
# LOADTEST_PASSWORD are set, and then it skips the weather endpoint and starts at lower rates.
# See docs/LOAD_TEST.md before running it anywhere but localhost.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8090}"
KEYCLOAK_REALM="${KEYCLOAK_REALM:-solnotfound}"
KEYCLOAK_CLIENT_ID="${KEYCLOAK_CLIENT_ID:-planazo-loadtest}"
LOADTEST_USERS="${LOADTEST_USERS:-10}"
LOADTEST_USER_PREFIX="${LOADTEST_USER_PREFIX:-loadtest}"
DURATION="${DURATION:-60s}"
TIMEOUT="${TIMEOUT:-10s}"
MIN_OK_RATIO="${MIN_OK_RATIO:-0.99}"
MAX_P95_MS="${MAX_P95_MS:-500}"
TIME_ZONE="${TIME_ZONE:-America/Argentina/Buenos_Aires}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BOUNDARY="planazo-loadtest-boundary"

usage() {
  sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 2
}

# Succeeds when the URL points to this machine.
is_local_url() {
  [[ "$1" =~ ^https?://(localhost|127\.0\.0\.1|\[::1\])(:[0-9]+)?(/|$) ]]
}

# Decides local/remote defaults and refuses remote targets without explicit opt-in.
configure_target() {
  if is_local_url "$BASE_URL" && is_local_url "$KEYCLOAK_URL"; then
    REMOTE=false
    DEFAULT_RATES="${RATES:-10/s 50/s 100/s}"
    INCLUDE_WEATHER="${INCLUDE_WEATHER:-true}"
    return
  fi
  REMOTE=true
  [[ "${ALLOW_REMOTE:-}" == 1 ]] ||
    fail "BASE_URL/KEYCLOAK_URL are not local; set ALLOW_REMOTE=1 after coordinating the test (docs/LOAD_TEST.md)"
  [[ -n "${LOADTEST_PASSWORD:-}" ]] ||
    fail "remote targets need LOADTEST_PASSWORD (the password of the temporary load test users)"
  DEFAULT_RATES="${RATES:-5/s 10/s 20/s 50/s}"
  # Remotely the weather endpoint calls the real provider; do not load it unless asked to.
  INCLUDE_WEATHER="${INCLUDE_WEATHER:-false}"
}

log() { printf '[loadtest] %s\n' "$*" >&2; }
fail() {
  log "ERROR: $*"
  exit 1
}

require_tools() {
  local tool
  for tool in vegeta jq curl base64; do
    command -v "$tool" >/dev/null 2>&1 || fail "'$tool' is required and was not found in PATH"
  done
}

# Prints a date N days from now as an ISO local date-time (GNU date or BSD/macOS date).
future_date_time() {
  date -d "+$1 days" '+%Y-%m-%dT10:00:00' 2>/dev/null || date -v "+$1d" '+%Y-%m-%dT10:00:00'
}

# Prints an access token for <prefix>NN obtained with the password grant. Locally the password
# defaults to the username (keycloak/configure-local.sh); remote runs require LOADTEST_PASSWORD.
fetch_token() {
  local username
  username="$(printf '%s%02d' "$LOADTEST_USER_PREFIX" "$1")"
  curl -fsS \
    --data-urlencode grant_type=password \
    --data-urlencode "client_id=$KEYCLOAK_CLIENT_ID" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=${LOADTEST_PASSWORD:-$username}" \
    "$KEYCLOAK_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token" |
    jq -er .access_token ||
    fail "could not get a token for $username; check the Keycloak client, users and password"
}

# Fills TOKENS[1..LOADTEST_USERS]. Called once per stage because access tokens are short-lived.
fetch_tokens() {
  local i
  TOKENS=()
  for ((i = 1; i <= LOADTEST_USERS; i++)); do
    TOKENS[i]="$(fetch_token "$i")"
  done
}

# Prints the JSON of a CreateActivityRequest; $1 makes the title unique.
activity_json() {
  jq -nc \
    --arg title "Load test $1" \
    --arg dateTime "$(future_date_time 7)" \
    '{
      title: $title,
      description: "Actividad creada por la prueba de carga",
      type: "OUTDOOR",
      location: {city: "Buenos Aires", latitude: -34.6037, longitude: -58.3816},
      dateTime: $dateTime,
      minParticipants: 1,
      maxParticipants: 1000,
      weatherConditions: {maxRainProbability: 60, minTemperature: 5, maxTemperature: 35, maxWindSpeed: 40},
      anticipationWindow: 2,
      reprogramationRange: {maxDays: 3, initialHour: "09:00:00", finalHour: "18:00:00"}
    }'
}

# Writes a multipart/form-data body with a single JSON "activity" part to $2.
write_multipart_body() {
  printf -- '--%s\r\nContent-Disposition: form-data; name="activity"\r\nContent-Type: application/json\r\n\r\n%s\r\n--%s--\r\n' \
    "$BOUNDARY" "$1" "$BOUNDARY" >"$2"
}

# Prints one Vegeta JSON target. Args: method path token [body_file content_type].
target() {
  local method="$1" path="$2" token="$3" body_file="${4:-}" content_type="${5:-}" body=""
  if [[ -n "$body_file" ]]; then
    body="$(base64 <"$body_file" | tr -d '\n')"
  fi
  jq -nc \
    --arg method "$method" \
    --arg url "$BASE_URL$path" \
    --arg token "$token" \
    --arg body "$body" \
    --arg contentType "$content_type" \
    --arg timeZone "$TIME_ZONE" \
    '{method: $method, url: $url, header: {"X-Time-Zone": [$timeZone]}}
     | if $token != "" then .header.Authorization = ["Bearer " + $token] else . end
     | if $body != "" then .body = $body | .header["Content-Type"] = [$contentType] else . end'
}

# Creates one activity per load test user so every user has an activity it organizes
# (needed for /activities/{id}/weather) and the mixed scenario has activities to join.
warm_up() {
  local i body
  OWN_IDS=()
  fetch_tokens
  for ((i = 1; i <= LOADTEST_USERS; i++)); do
    body="$(activity_json "warm-up $i $RUN_ID")"
    OWN_IDS[i]="$(curl -fsS \
      -H "Authorization: Bearer ${TOKENS[i]}" \
      -H "X-Time-Zone: $TIME_ZONE" \
      -F "activity=$body;type=application/json" \
      "$BASE_URL/activities" | jq -er .id)" || fail "warm-up could not create an activity"
  done
  mapfile -t SEED_IDS < <(
    curl -fsS -H "Authorization: Bearer ${TOKENS[1]}" "$BASE_URL/activities?page=0&size=50" |
      jq -r '.content[].id'
  )
  ((${#SEED_IDS[@]} > 0)) || fail "GET /activities returned no activities"
  log "warm-up: ${#OWN_IDS[@]} activities created, ${#SEED_IDS[@]} activities available to read"
}

# Prints the read targets of user $1.
read_targets() {
  local i="$1" token="${TOKENS[$1]}"
  local seed_id="${SEED_IDS[$(((i - 1) % ${#SEED_IDS[@]}))]}"
  target GET "/activities?page=0&size=12" "$token"
  target GET "/activities?type=OUTDOOR&status=CONFIRMED&page=0&size=12" "$token"
  target GET "/activities?city=Buenos%20Aires&availability=true&page=0&size=12" "$token"
  target GET "/activities/$seed_id" "$token"
  if [[ "$INCLUDE_WEATHER" == true ]]; then
    target GET "/activities/${OWN_IDS[i]}/weather" "$token"
  fi
  target GET "/activities/organizers/me?page=0&size=12" "$token"
  target GET "/activities/participants/me?page=0&size=12" "$token"
  target GET "/votations?page=0&size=20" "$token"
  target GET "/notifications" "$token"
}

# Writes the targets of $SCENARIO to $1.
write_targets() {
  local file="$1" body_dir="$2" i other body_file
  : >"$file"
  case "$SCENARIO" in
    smoke)
      target GET /healthcheck "" >>"$file"
      ;;
    read)
      for ((i = 1; i <= LOADTEST_USERS; i++)); do
        read_targets "$i" >>"$file"
      done
      ;;
    mixed)
      # Per user: 9 reads + 1 create + 1 join (about 80% reads / 20% writes).
      for ((i = 1; i <= LOADTEST_USERS; i++)); do
        other=$((i % LOADTEST_USERS + 1))
        body_file="$body_dir/activity-$i.multipart"
        write_multipart_body "$(activity_json "$RUN_ID user $i")" "$body_file"
        {
          read_targets "$i"
          target POST /activities "${TOKENS[i]}" "$body_file" "multipart/form-data; boundary=$BOUNDARY"
          target PUT "/activities/${OWN_IDS[other]}/participants/me" "${TOKENS[i]}"
        } >>"$file"
      done
      ;;
  esac
}

# Checks report.json against the thresholds; prints a summary and returns 1 on failure.
check_report() {
  local report="$1"
  jq -e -r \
    --argjson minOk "$MIN_OK_RATIO" \
    --argjson maxP95 "$MAX_P95_MS" \
    '(.status_codes // {}) as $codes
     | ([$codes | to_entries[] | select(.key | test("^[23]")) | .value] | add // 0) as $ok
     | ([$codes | to_entries[] | select(.key | test("^5")) | .value] | add // 0) as $server
     | ($codes["0"] // 0) as $transport
     | ($ok / .requests) as $ratio
     | (.latencies["95th"] / 1e6) as $p95
     | "requests=\(.requests) throughput=\(.throughput * 100 | round / 100)/s ok=\($ratio * 10000 | round / 100)% 5xx=\($server) transport_errors=\($transport) p50=\(.latencies["50th"] / 1e6 | round)ms p95=\($p95 | round)ms p99=\(.latencies["99th"] / 1e6 | round)ms codes=\($codes | tostring)",
       if $ratio >= $minOk and $server == 0 and $transport == 0 and $p95 < $maxP95
       then "PASS" else error("FAIL") end' \
    "$report"
}

run_stage() {
  local rate="$1" stage_dir
  stage_dir="$RUN_DIR/${rate//\//per}"
  mkdir -p "$stage_dir"
  fetch_tokens
  write_targets "$stage_dir/targets.jsonl" "$stage_dir"
  log "stage $rate for $DURATION ($(wc -l <"$stage_dir/targets.jsonl" | tr -d ' ') targets)"

  vegeta attack \
    -format=json \
    -targets="$stage_dir/targets.jsonl" \
    -rate="$rate" \
    -duration="$DURATION" \
    -timeout="$TIMEOUT" \
    -name="$SCENARIO-$rate" \
    -output="$stage_dir/results.bin"

  vegeta report "$stage_dir/results.bin" >"$stage_dir/report.txt"
  vegeta report -type=json "$stage_dir/results.bin" >"$stage_dir/report.json"
  vegeta report -type='hist[0,50ms,100ms,250ms,500ms,1s,5s]' "$stage_dir/results.bin" >"$stage_dir/histogram.txt"
  vegeta plot -title="Planazo $SCENARIO $rate" "$stage_dir/results.bin" >"$stage_dir/plot.html"
  cat "$stage_dir/report.txt" "$stage_dir/histogram.txt" >&2

  if ! check_report "$stage_dir/report.json" >&2; then
    log "stage $rate did not meet the thresholds (ok >= $MIN_OK_RATIO, no 5xx, p95 < ${MAX_P95_MS}ms); stopping"
    return 1
  fi
}

main() {
  SCENARIO="${1:-}"
  case "$SCENARIO" in
    smoke | read | mixed) shift ;;
    *) usage ;;
  esac
  configure_target
  local rates=("$@")
  if ((${#rates[@]} == 0)); then
    read -r -a rates <<<"$DEFAULT_RATES"
  fi

  require_tools
  curl -fsS "$BASE_URL/healthcheck" >/dev/null || fail "$BASE_URL/healthcheck is not responding"

  RUN_ID="$(date '+%Y%m%d-%H%M%S')"
  RUN_DIR="$SCRIPT_DIR/results/$RUN_ID-$SCENARIO"
  # targets.jsonl holds live bearer tokens: keep results readable only by the current user.
  umask 077
  mkdir -p "$RUN_DIR"
  log "scenario=$SCENARIO base_url=$BASE_URL remote=$REMOTE weather=$INCLUDE_WEATHER rates=${rates[*]} duration=$DURATION results=$RUN_DIR"

  if [[ "$SCENARIO" != smoke ]]; then
    warm_up
  fi

  local rate
  for rate in "${rates[@]}"; do
    run_stage "$rate" || exit 1
  done
  log "all stages passed; reports in $RUN_DIR"
  curl -fsS "$BASE_URL/healthcheck" >/dev/null || fail "healthcheck failing after the test"
}

main "$@"

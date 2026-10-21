#!/bin/sh
# Runs the legacy weather binary inside the container.
#
# - The binary only listens on loopback, so socat forwards the container's port 9978 to it.
# - It prints its API token after signing in or restoring the session; that line is copied to
#   /token/api-token (a volume the backend mounts read-only), so the token never has to be
#   copied by hand or versioned.
# - When the binary exits (it crashes on its own), this script exits too and the container
#   restart policy starts it again; the session in /data is reused, so no new sign-in is needed.
set -eu

# In the cloud there is no way to sign in interactively: the session saved by a local sign-in is
# mounted read-only from Secret Manager. The binary rewrites token.json on start, so it gets a
# writable copy. It is read with cat: cp refuses to copy a Cloud Run secret file ("replaced while
# being copied") because the mounted volume reports changing metadata.
if [ -n "${SESSION_SECRET_FILE:-}" ] && [ -f "$SESSION_SECRET_FILE" ]; then
  mkdir -p "$XDG_CONFIG_HOME/tacs"
  (umask 077 && cat "$SESSION_SECRET_FILE" > "$XDG_CONFIG_HOME/tacs/token.json")
  if [ ! -s "$XDG_CONFIG_HOME/tacs/token.json" ]; then
    echo "[entrypoint] The session secret is empty; load it from a local sign-in" >&2
    exit 1
  fi
fi

socat TCP-LISTEN:9978,fork,reuseaddr,bind=0.0.0.0 TCP:127.0.0.1:9977 &

# Watchdog: Docker only restarts a container whose main process exits, not an unhealthy one. When
# the binary stops answering its liveness probe three times in a row it is killed, so the
# container exits and the restart policy brings it back.
(
  sleep 20
  failures=0
  while true; do
    if curl -fsS --max-time 2 -o /dev/null http://127.0.0.1:9977/healthz; then
      failures=0
    else
      failures=$((failures + 1))
    fi
    if [ "$failures" -ge 3 ]; then
      echo "  [entrypoint] liveness probe failed 3 times, restarting the service"
      for proc in /proc/[0-9]*; do
        if [ "$(cat "$proc/comm" 2>/dev/null)" = "tacs" ]; then kill "${proc#/proc/}"; fi
      done
      failures=0
    fi
    sleep 10
  done
) &

mkdir -p /token
ESC="$(printf '\033')"
# A shell loop instead of awk: Debian's mawk buffers its input, so nothing would be logged until
# its buffer fills.
/usr/local/bin/tacs 2>&1 | while IFS= read -r line; do
  plain="$(printf '%s' "$line" | sed "s/${ESC}\[[0-9;]*m//g" | tr -d ' \t\r')"
  if printf '%s' "$plain" | grep -Eq '^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]{20,}$'; then
    printf '%s' "$plain" > /token/api-token.tmp && mv /token/api-token.tmp /token/api-token
    echo "  [entrypoint] API token stored for the backend"
    continue
  fi
  printf '%s\n' "$line"
done

#!/usr/bin/env bash
# Signs the legacy weather service in with an institutional Google account.
#
# The binary waits 5 minutes for Google to redirect the browser to http://localhost:<port>/ inside
# its container, which the browser cannot reach. This script shows the sign-in link, then takes the
# address the browser ends up on (an error page is expected) and delivers it inside the container.
set -euo pipefail

SERVICE="${1:-legacy-weather}"

link="$(docker compose logs --no-log-prefix "$SERVICE" | grep -o 'https://accounts.google.com[^ ]*' | tail -n 1 || true)"
if [ -z "$link" ]; then
  echo "The service is not waiting for a sign-in (it may already have a session)."
  echo "Check it with: docker compose logs $SERVICE"
  exit 1
fi

echo "1. Open this link and sign in with your @frba.utn.edu.ar account (you have 5 minutes):"
echo
echo "   $link"
echo
echo "2. The browser will fail to open a http://localhost:<port>/?state=...&code=... address."
read -r -p "   Paste that full address here: " callback

case "$callback" in
  http://localhost:*) ;;
  *) echo "That is not the http://localhost address the browser was sent to." >&2; exit 1 ;;
esac

docker compose exec -T "$SERVICE" curl -fsS --max-time 30 -o /dev/null "$callback"
sleep 2
docker compose logs --no-log-prefix --tail 20 "$SERVICE" | grep -E "signed in|session|API token stored" || true

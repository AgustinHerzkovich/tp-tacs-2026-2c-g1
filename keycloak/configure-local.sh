#!/bin/sh

set -eu

KCADM=/opt/keycloak/bin/kcadm.sh
SERVER=http://keycloak:8080
REALM=solnotfound

"$KCADM" config credentials \
  --server "$SERVER" \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USERNAME:-admin}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD:-admin}"

"$KCADM" update "realms/$REALM" \
  -s displayName=Planazo \
  -s displayNameHtml=Planazo \
  -s loginTheme=planazo \
  -s internationalizationEnabled=true \
  -s 'supportedLocales=["es"]' \
  -s defaultLocale=es

ensure_user() {
  username="$1"
  password="$2"
  email="$3"

  user_id=$("$KCADM" get users -r "$REALM" -q "username=$username" --fields id --format csv --noquotes)
  if [ -z "$user_id" ]; then
    "$KCADM" create users -r "$REALM" \
      -s "username=$username" \
      -s enabled=true \
      -s "email=$email" \
      -s emailVerified=true
  fi

  "$KCADM" set-password -r "$REALM" --username "$username" --new-password "$password"
}

ensure_user alumno alumno alumno@planazo.local
ensure_user admin admin admin@planazo.local

"$KCADM" add-roles -r "$REALM" --uusername admin --rolename ADMIN

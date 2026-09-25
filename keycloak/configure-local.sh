#!/bin/sh

set -eu

KCADM=/opt/keycloak/bin/kcadm.sh
SERVER=http://keycloak:8080
REALM=solnotfound
PUBLIC_URL="${KEYCLOAK_PUBLIC_URL:-http://localhost:8090}"
USER_ROLE=USER
BOT_ROLE=TELEGRAM_BOT
BOT_CLIENT_ID="${TELEGRAM_BOT_CLIENT_ID:-solnotfoundTelegramBot}"
BOT_CLIENT_SECRET="${TELEGRAM_BOT_CLIENT_SECRET:-solnotfound-telegram-bot-client-secret}"
BOT_SERVICE_ACCOUNT="service-account-$BOT_CLIENT_ID"

"$KCADM" config credentials \
  --server "$SERVER" \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USERNAME:-admin}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD:-admin}"

"$KCADM" update "realms/$REALM" \
  -s displayName=Planazo \
  -s displayNameHtml=Planazo \
  -s loginTheme=planazo

# Los tokens que emite Keycloak llevan el issuer derivado de la URL usada para
# pedirlos. Fijar la URL publica del realm hace que el token de la funcion del
# bot, que la pide desde la red interna de Compose, sea aceptado por el backend
# (que valida SECURITY_JWT_ISSUER_URI contra la URL publica).
"$KCADM" update "realms/$REALM" \
  -s "attributes.frontendUrl=$PUBLIC_URL"

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

# Rol de la cuenta de servicio del bot. Es compuesto por USER, asi que la cuenta
# tiene los mismos permisos que un usuario normal.
ensure_realm_role() {
  role="$1"
  description="$2"

  if ! "$KCADM" get "roles/$role" -r "$REALM" --fields name --format csv --noquotes >/dev/null 2>&1; then
    "$KCADM" create roles -r "$REALM" \
      -s "name=$role" \
      -s "description=$description" \
      -s composite=true
  fi

  "$KCADM" add-roles -r "$REALM" --rolename "$role" --composites "$USER_ROLE"
}

ensure_user alumno alumno alumno@planazo.local
ensure_user admin admin admin@planazo.local

"$KCADM" add-roles -r "$REALM" --uusername admin --rolename ADMIN

ensure_realm_role "$BOT_ROLE" \
  "Rol de la cuenta de servicio de la funcion serverless del bot de Telegram. Compuesto por USER; habilita los endpoints /users/telegram/** junto con el API token del bot."

# El secreto del cliente confidencial lo usa la funcion del bot para pedir su
# access token con client_credentials.
"$KCADM" update "clients/$BOT_CLIENT_ID" -r "$REALM" -s "secret=$BOT_CLIENT_SECRET"

# Crear la cuenta de servicio del cliente (si el import del realm ya la creo,
# esta llamada no hace nada) y asignarle el rol del bot.
"$KCADM" get "clients/$BOT_CLIENT_ID/service-account-user" -r "$REALM" >/dev/null
"$KCADM" add-roles -r "$REALM" --uusername "$BOT_SERVICE_ACCOUNT" --rolename "$BOT_ROLE"


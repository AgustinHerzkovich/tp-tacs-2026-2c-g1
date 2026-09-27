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

  "$KCADM" add-roles -r "$REALM" --rname "$role" --rolename "$USER_ROLE"
}

ensure_user alumno alumno alumno@planazo.local
ensure_user admin admin admin@planazo.local

"$KCADM" add-roles -r "$REALM" --uusername admin --rolename ADMIN

ensure_realm_role "$BOT_ROLE" \
  "Rol de la cuenta de servicio de la funcion serverless del bot de Telegram. Compuesto por USER; habilita los endpoints /users/telegram/** junto con el API token del bot."

# El import del realm solo corre si el realm no existe, asi que en un volumen
# viejo el cliente puede faltar. La API de admin identifica al cliente por su
# UUID interno, no por el clientId.
bot_client_uuid=$("$KCADM" get clients -r "$REALM" -q "clientId=$BOT_CLIENT_ID" --fields id --format csv --noquotes)
if [ -z "$bot_client_uuid" ]; then
  "$KCADM" create clients -r "$REALM" \
    -s "clientId=$BOT_CLIENT_ID" \
    -s enabled=true \
    -s publicClient=false \
    -s clientAuthenticatorType=client-secret \
    -s standardFlowEnabled=false \
    -s implicitFlowEnabled=false \
    -s directAccessGrantsEnabled=false \
    -s serviceAccountsEnabled=true
  bot_client_uuid=$("$KCADM" get clients -r "$REALM" -q "clientId=$BOT_CLIENT_ID" --fields id --format csv --noquotes)
fi

# El secreto del cliente confidencial lo usa la funcion del bot para pedir su
# access token con client_credentials.
"$KCADM" update "clients/$bot_client_uuid" -r "$REALM" -s "secret=$BOT_CLIENT_SECRET"

# Crear la cuenta de servicio del cliente (si el import del realm ya la creo,
# esta llamada no hace nada) y asignarle el rol del bot.
bot_service_account_id=$("$KCADM" get "clients/$bot_client_uuid/service-account-user" -r "$REALM" --fields id --format csv --noquotes)
"$KCADM" add-roles -r "$REALM" --uid "$bot_service_account_id" --rolename "$BOT_ROLE"

# El cliente tiene fullScopeAllowed=false, asi que el token solo incluye los roles
# de realm mapeados explicitamente al cliente. Sin este mapping el JWT no lleva
# TELEGRAM_BOT y el backend responde 403.
bot_role_id=$("$KCADM" get "roles/$BOT_ROLE" -r "$REALM" --fields id --format csv --noquotes)
"$KCADM" create "clients/$bot_client_uuid/scope-mappings/realm" -r "$REALM" \
  -b "[{\"id\":\"$bot_role_id\",\"name\":\"$BOT_ROLE\"}]"


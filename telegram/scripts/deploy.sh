#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

: "${GCP_PROJECT_ID:?Falta GCP_PROJECT_ID (copia .env.example a .env y completalo)}"
: "${TELEGRAM_BOT_TOKEN:?Falta TELEGRAM_BOT_TOKEN (copia .env.example a .env y completalo)}"
: "${TELEGRAM_API_TOKEN:?Falta TELEGRAM_API_TOKEN (el backend lo exige junto con el JWT del bot)}"
: "${KEYCLOAK_URL:?Falta KEYCLOAK_URL (URL publica de Keycloak)}"
: "${KEYCLOAK_REALM:?Falta KEYCLOAK_REALM}"
: "${KEYCLOAK_TELEGRAM_BOT_CLIENT_ID:?Falta KEYCLOAK_TELEGRAM_BOT_CLIENT_ID}"
: "${KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET:?Falta KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET}"
: "${BACKEND_URL:?Falta BACKEND_URL (URL publica del backend)}"
GCP_REGION="${GCP_REGION:-us-central1}"
FUNCTION_NAME="${FUNCTION_NAME:-telegram-webhook}"
SECRET_NAME="${SECRET_NAME:-telegram-bot-token}"
CLIENT_SECRET_NAME="${CLIENT_SECRET_NAME:-telegram-bot-keycloak-client-secret}"
API_TOKEN_SECRET_NAME="${API_TOKEN_SECRET_NAME:-telegram-bot-api-token}"

echo "==> Proyecto: $GCP_PROJECT_ID | Region: $GCP_REGION | Funcion: $FUNCTION_NAME"

gcloud config set project "$GCP_PROJECT_ID" >/dev/null

echo "==> Habilitando APIs necesarias"
gcloud services enable \
  cloudfunctions.googleapis.com \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  --project "$GCP_PROJECT_ID"

sync_secret() {
  secret="$1"
  value="$2"

  if gcloud secrets describe "$secret" --project "$GCP_PROJECT_ID" >/dev/null 2>&1; then
    printf '%s' "$value" | gcloud secrets versions add "$secret" \
      --project "$GCP_PROJECT_ID" --data-file=-
  else
    printf '%s' "$value" | gcloud secrets create "$secret" \
      --project "$GCP_PROJECT_ID" --replication-policy=automatic --data-file=-
  fi
}

echo "==> Sincronizando secrets del bot en Secret Manager"
sync_secret "$SECRET_NAME" "$TELEGRAM_BOT_TOKEN"
sync_secret "$CLIENT_SECRET_NAME" "$KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET"
sync_secret "$API_TOKEN_SECRET_NAME" "$TELEGRAM_API_TOKEN"

echo "==> Dando permiso al service account de Cloud Functions para leer los secrets"
PROJECT_NUMBER=$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
for secret in "$SECRET_NAME" "$CLIENT_SECRET_NAME" "$API_TOKEN_SECRET_NAME"; do
  gcloud secrets add-iam-policy-binding "$secret" \
    --project "$GCP_PROJECT_ID" \
    --member="serviceAccount:${RUNTIME_SA}" \
    --role="roles/secretmanager.secretAccessor" >/dev/null
done

echo "==> Deployando la Cloud Function ($FUNCTION_NAME)"
gcloud functions deploy "$FUNCTION_NAME" \
  --gen2 \
  --runtime=nodejs20 \
  --region="$GCP_REGION" \
  --project="$GCP_PROJECT_ID" \
  --source=. \
  --entry-point=webhook \
  --trigger-http \
  --allow-unauthenticated \
  --set-env-vars="BACKEND_URL=${BACKEND_URL},KEYCLOAK_URL=${KEYCLOAK_URL},KEYCLOAK_REALM=${KEYCLOAK_REALM},KEYCLOAK_TELEGRAM_BOT_CLIENT_ID=${KEYCLOAK_TELEGRAM_BOT_CLIENT_ID}" \
  --set-secrets="TELEGRAM_BOT_TOKEN=${SECRET_NAME}:latest,KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET=${CLIENT_SECRET_NAME}:latest,TELEGRAM_API_TOKEN=${API_TOKEN_SECRET_NAME}:latest"

FUNCTION_URL=$(gcloud functions describe "$FUNCTION_NAME" \
  --gen2 --region="$GCP_REGION" --project="$GCP_PROJECT_ID" \
  --format='value(serviceConfig.uri)')

echo "==> Funcion deployada en: $FUNCTION_URL"

echo "==> Registrando el webhook en Telegram"
curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -d "url=${FUNCTION_URL}"

echo
echo "==> Listo."

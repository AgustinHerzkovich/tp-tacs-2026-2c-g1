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
GCP_REGION="${GCP_REGION:-us-central1}"
FUNCTION_NAME="${FUNCTION_NAME:-telegram-webhook}"
SECRET_NAME="${SECRET_NAME:-telegram-bot-token}"

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

echo "==> Sincronizando secret del token del bot en Secret Manager"
if gcloud secrets describe "$SECRET_NAME" --project "$GCP_PROJECT_ID" >/dev/null 2>&1; then
  printf '%s' "$TELEGRAM_BOT_TOKEN" | gcloud secrets versions add "$SECRET_NAME" \
    --project "$GCP_PROJECT_ID" --data-file=-
else
  printf '%s' "$TELEGRAM_BOT_TOKEN" | gcloud secrets create "$SECRET_NAME" \
    --project "$GCP_PROJECT_ID" --replication-policy=automatic --data-file=-
fi

echo "==> Dando permiso al service account de Cloud Functions para leer el secret"
PROJECT_NUMBER=$(gcloud projects describe "$GCP_PROJECT_ID" --format='value(projectNumber)')
RUNTIME_SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
gcloud secrets add-iam-policy-binding "$SECRET_NAME" \
  --project "$GCP_PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/secretmanager.secretAccessor" >/dev/null

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
  --set-secrets="TELEGRAM_BOT_TOKEN=${SECRET_NAME}:latest"

FUNCTION_URL=$(gcloud functions describe "$FUNCTION_NAME" \
  --gen2 --region="$GCP_REGION" --project="$GCP_PROJECT_ID" \
  --format='value(serviceConfig.uri)')

echo "==> Funcion deployada en: $FUNCTION_URL"

echo "==> Registrando el webhook en Telegram"
curl -s -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -d "url=${FUNCTION_URL}"

echo
echo "==> Listo."

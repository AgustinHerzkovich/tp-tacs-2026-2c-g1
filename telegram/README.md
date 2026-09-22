# telegram

Bot de Telegram implementado como Cloud Function HTTP (Google Cloud Functions gen2 / Cloud Run functions).

## Desarrollo local

```bash
npm install
npm start
```

Esto compila TypeScript (`tsc`) y levanta el Functions Framework en `http://localhost:8080`,
apuntando a la función exportada `webhook`.

Para simular un update de Telegram:

```bash
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -d '{"update_id":1,"message":{"message_id":1,"text":"hola"}}'
```

## Deploy a GCP

Requiere:

- [gcloud CLI](https://cloud.google.com/sdk/docs/install) instalado y autenticado (`gcloud auth login`).
- Un proyecto de GCP existente con facturación habilitada (el script habilita las APIs necesarias
  automáticamente, pero el proyecto tiene que existir).

Pasos:

1. Copiar `.env.example` a `.env` y completar `GCP_PROJECT_ID` y `TELEGRAM_BOT_TOKEN`
   (`GCP_REGION`, `FUNCTION_NAME` y `SECRET_NAME` ya tienen valores por defecto razonables).
2. Ejecutar:

   ```bash
   npm run deploy
   ```

`scripts/deploy.sh` hace todo el stack de punto a punto, de forma idempotente (se puede volver a
correr sin romper nada si ya existe algo):

1. Habilita las APIs de GCP necesarias (Cloud Functions, Cloud Build, Cloud Run, Artifact Registry,
   Secret Manager).
2. Crea (o actualiza) el secret `SECRET_NAME` en Secret Manager con el `TELEGRAM_BOT_TOKEN`, en vez
   de subir el token como variable de entorno en texto plano.
3. Le da permiso al service account de la función para leer ese secret.
4. Deploya la Cloud Function (`gen2`, `nodejs20`, HTTP trigger, `--entry-point=webhook`), inyectando
   el token desde el secret.
5. Toma la URL pública que devuelve `gcloud` y llama a `setWebhook` de Telegram para registrarla
   automáticamente.

En Windows, correr el script desde Git Bash (ya se usa en el resto del proyecto) o WSL, ya que es un
script `bash`.

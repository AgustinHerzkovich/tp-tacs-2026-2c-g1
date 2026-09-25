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

## Autenticación contra el backend

La función no se saltea la autenticación del backend: usa la misma que cualquier usuario y, además,
un segundo factor. Todas las llamadas pasan por `backendFetch` (`src/utils/auth.ts`), que envía:

- **JWT de Keycloak**: la función pide un access token con `client_credentials` al cliente
  confidencial `solnotfoundTelegramBot`. Ese cliente tiene una cuenta de servicio con el rol de realm
  `TELEGRAM_BOT`, que es compuesto por `USER`, así que el token tiene los mismos permisos que un
  usuario normal y además habilita los endpoints `/users/telegram/**`. El token se cachea en el
  contexto de la función hasta su vencimiento y se renueva solo.
- **API token del bot**: header `X-Api-Token` con el valor de `TELEGRAM_API_TOKEN`.

El backend exige **las dos** credenciales: el filtro de API token rechaza con `401` toda llamada que
no lo envíe (aunque el JWT sea válido) y la autorización exige el rol `TELEGRAM_BOT` del JWT (un
usuario común recibe `403`). Ninguna de las dos alcanza por sí sola.

Variables de entorno:

| Variable | Descripción |
| --- | --- |
| `BACKEND_URL` | URL del backend. |
| `TELEGRAM_API_TOKEN` | API token que el backend valida en `X-Api-Token`. |
| `KEYCLOAK_URL` | URL de Keycloak (dentro de Compose, `http://keycloak:8080`). |
| `KEYCLOAK_REALM` | Realm, `solnotfound` por defecto. |
| `KEYCLOAK_TELEGRAM_BOT_CLIENT_ID` | `solnotfoundTelegramBot` por defecto. |
| `KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET` | Client secret de ese cliente. |

Los valores por defecto de desarrollo están en el `.env.example` de la raíz. En Compose el rol, el
cliente y el client secret se crean automáticamente con el import del realm y
`keycloak/configure-local.sh`.

## Deploy a GCP

Requiere:

- [gcloud CLI](https://cloud.google.com/sdk/docs/install) instalado y autenticado (`gcloud auth login`).
- Un proyecto de GCP existente con facturación habilitada (el script habilita las APIs necesarias
  automáticamente, pero el proyecto tiene que existir).
- El cliente `solnotfoundTelegramBot` creado en el Keycloak de destino, con su client secret en
  `KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET` y el rol `TELEGRAM_BOT` asignado a su cuenta de servicio.

Pasos:

1. Copiar `.env.example` a `.env` y completar `GCP_PROJECT_ID`, `TELEGRAM_BOT_TOKEN`,
   `TELEGRAM_API_TOKEN`, `BACKEND_URL`, `KEYCLOAK_URL`, `KEYCLOAK_REALM` y
   `KEYCLOAK_TELEGRAM_BOT_CLIENT_ID`/`KEYCLOAK_TELEGRAM_BOT_CLIENT_SECRET` (`GCP_REGION`,
   `FUNCTION_NAME` y `SECRET_NAME` ya tienen valores por defecto razonables).
2. Ejecutar:

   ```bash
   npm run deploy
   ```

`scripts/deploy.sh` hace todo el stack de punto a punto, de forma idempotente (se puede volver a
correr sin romper nada si ya existe algo):

1. Habilita las APIs de GCP necesarias (Cloud Functions, Cloud Build, Cloud Run, Artifact Registry,
   Secret Manager).
2. Crea (o actualiza) en Secret Manager el secret `SECRET_NAME` con el `TELEGRAM_BOT_TOKEN`,
   `CLIENT_SECRET_NAME` con el client secret de Keycloak y `API_TOKEN_SECRET_NAME` con el API token,
   en vez de subirlos como variables de entorno en texto plano.
3. Le da permiso al service account de la función para leer esos secrets.
4. Deploya la Cloud Function (`gen2`, `nodejs20`, HTTP trigger, `--entry-point=webhook`), inyectando
   los secretos desde Secret Manager y las URLs por variable de entorno.
5. Toma la URL pública que devuelve `gcloud` y llama a `setWebhook` de Telegram para registrarla
   automáticamente.

El `--allow-unauthenticated` es necesario porque Telegram no envía credenciales al webhook; la
autenticación la aplica el backend, que exige JWT y API token en cada llamada.

En Windows, correr el script desde Git Bash (ya se usa en el resto del proyecto) o WSL, ya que es un
script `bash`.

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

## Comandos

| Comando | Qué hace |
| --- | --- |
| `/start` | Identifica el chat contra el backend. Si el chat ya está vinculado a una cuenta, saluda y muestra el teclado; si no, manda el link de login del frontend. |
| `/login` | Reenvía el link de login para vincular el chat a una cuenta. |
| `/seeActivities` | Lista las actividades en las que el usuario es participante, de la más próxima a la más lejana, con el estado completo que devuelve el backend. |
| `/myActivities` | Lista las actividades que organiza el usuario y avisa cuáles están en votación de reprogramación. |

Salvo `/start` y `/login`, todos exigen un chat identificado: si el usuario todavía no ejecutó
`/start`, el bot se lo pide.

Los listados se arman recorriendo las páginas de `GET /activities` y filtrando en el bot por
`organizerId` y por `participants[].userId` (ver la limitación de identidad más abajo). Cada actividad
se muestra con su estado, disponibilidad, cantidad de participantes, límites de clima, anticipación
del chequeo y ventana de reprogramación. Los mensajes largos se parten en varios envíos porque
Telegram corta todo lo que pase los 4096 caracteres.

### Limitación de identidad

El backend toma la identidad de negocio del claim `sub` del JWT, así que la cuenta de servicio del
bot solo puede actuar en nombre propio. El bot puede leer el catálogo completo de actividades, pero no
usar los endpoints donde el usuario es el actor:

- `GET /activities/participants/me` y `GET /activities/organizers/me` devolverían las actividades
  del bot, o sea listas vacías.
- `GET /activities/{id}/weather` responde `403`, porque exige que quien consulta sea participante.
- `GET /votations` y `PUT /votations/{id}/votes/me` registrarían la votación a nombre del bot.

Por eso los comandos son de solo lectura. Quedan pendientes para cuando el backend exponga una ruta
que resuelva el chat al usuario: votar, consultar el clima de una actividad, abandonar o cancelar.
Tampoco hay endpoint que guarde los cambios de clima, anticipación o ventana de reprogramación de una
actividad ya creada, así que la configuración de actividades no se expone desde el bot.

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

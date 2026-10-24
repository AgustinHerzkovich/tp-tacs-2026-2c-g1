# Proveedor de clima legacy (Entrega 4)

El servicio de Humedad, Datos y Pronósticos S.A. llegó como un binario compilado
(`bin/bin/tacs-<sistema>-<arquitectura>`) y una documentación incompleta. Este documento reúne lo
que se descubrió ejecutándolo, cómo se lo opera y por qué se lo consume como se lo consume.

## Puesta en marcha

### Local (`docker compose`)

Los binarios no están en el repositorio: traen embebido el cliente OAuth de Google de la cátedra, y
GitHub (con razón) rechaza publicarlo. Antes de levantar el stack se copian los que entregó la
cátedra a `bin/bin/` (`tacs-linux-amd64`, o `tacs-linux-arm64` en procesadores ARM con
`TARGETARCH=arm64`); la carpeta `bin/` está ignorada por Git.

El servicio `legacy-weather` se construye con `legacy-weather/Dockerfile` y se levanta junto con el
resto: `docker compose up --build --wait`. La primera vez no tiene sesión y espera un inicio de
sesión con Google durante 5 minutos:

```bash
./legacy-weather/login.sh
```

El script muestra el link de Google; se inicia sesión con la cuenta institucional
(`@frba.utn.edu.ar`), el navegador termina en una dirección `http://localhost:<puerto>/?state=...`
que no carga (es esperado) y esa dirección se pega en el script, que la entrega dentro del
contenedor. La sesión queda en el volumen `legacy-weather-session` y sobrevive a reinicios y a
`docker compose down`; solo `docker compose down --volumes` obliga a iniciar sesión de nuevo.

El token de API que imprime el servicio lo copia el contenedor al volumen `legacy-weather-token`,
que el backend monta en solo lectura. No hay que copiarlo a mano ni guardarlo en ningún archivo.

### Nube (GCP)

No hay forma de iniciar sesión de manera interactiva en Cloud Run, así que se reutiliza la sesión de
un inicio de sesión local. La primera vez, en este orden:

1. **Iniciar sesión localmente** (paso anterior).
2. **Crear los secretos vacíos** con Terraform:

   ```bash
   terraform -chdir=terraform apply -target=google_secret_manager_secret.application
   ```

3. **Cargar la sesión y el token** desde los volúmenes locales:

   ```bash
   docker compose exec -T legacy-weather cat /data/tacs/token.json \
     | gcloud secrets versions add planazo-prod-legacy-weather-session --data-file=-
   docker compose exec -T legacy-weather cat /token/api-token \
     | gcloud secrets versions add planazo-prod-legacy-weather-token --data-file=-
   ```

   En Git Bash (Windows) hay que anteponer `MSYS_NO_PATHCONV=1` a cada comando: si no, Git Bash
   convierte `/data/...` en una ruta de Windows y el secreto queda vacío.

   Tiene que ser antes del paso 5: el servicio legacy monta la última versión de la sesión y el
   backend lee la última versión del token, y sin versiones no arrancan.

4. **Subir el binario y publicar la primera imagen.** Cloud Build no recibe el binario con el
   código: lo baja de un bucket privado que crea Terraform.

   ```bash
   terraform -chdir=terraform apply \
     -target=google_storage_bucket.legacy_weather_binary \
     -target=google_storage_bucket_iam_member.cloud_build_reads_legacy_binary
   gcloud storage cp bin/bin/tacs-linux-amd64 gs://planazo-tacs-g1-2026-planazo-prod-legacy-binary/
   ```

   ```bash
   gcloud builds submit . --config=cloudbuild/legacy-weather.yaml \
     --substitutions=COMMIT_SHA=inicial,_REGION=us-central1,_REPOSITORY=planazo-prod-containers
   ```

5. **Aplicar Terraform** con la imagen. Conviene agregar a `terraform.tfvars`
   `legacy_weather_image = "us-central1-docker.pkg.dev/planazo-tacs-g1-2026/planazo-prod-containers/legacy-weather:inicial"`
   y aplicar solo lo que cambió:

   ```bash
   terraform -chdir=terraform apply \
     -target=google_service_account.legacy_weather \
     -target=google_secret_manager_secret_iam_member.legacy_weather_session \
     -target=google_secret_manager_secret_iam_member.backend_legacy_weather_token \
     -target=google_artifact_registry_repository_iam_member.runtime_readers \
     -target=google_service_account_iam_member.github_deployer_runtime_accounts \
     -target=google_cloud_run_v2_service.legacy_weather \
     -target=google_cloud_run_v2_service_iam_member.backend_invokes_legacy_weather \
     -target=google_cloud_run_v2_service.backend \
     -target=google_cloud_scheduler_job.scheduled_tasks
   ```

   Esto crea el servicio privado y le da al backend `LEGACY_WEATHER_URL`,
   `LEGACY_WEATHER_AUDIENCE`, `LEGACY_WEATHER_TOKEN` y `WEATHER_MONITOR_CRON=-`, además del job de
   Cloud Scheduler que toma la foto de los proveedores cada 5 minutos.

6. **Mergear el código.** `deploy.yml` despliega el backend y el frontend, y como cambió
   `legacy-weather/`, vuelve a construir la imagen del legacy (con el binario del bucket) y
   actualiza el servicio. Si la cátedra entrega un binario nuevo, se sube al bucket y se corre
   `deploy.yml` a mano con el componente `legacy-weather`.

Si la sesión de Google deja de valer, se repiten los pasos 1 y 3 y se despliega una revisión nueva
del servicio para que tome la última versión:

```bash
gcloud run services update planazo-prod-legacy-weather --region=us-central1 \
  --update-env-vars=SESSION_REFRESHED_AT=$(date +%s)
```

### La sesión

- **Qué es:** `token.json` es una sesión OAuth de Google, con un access token que dura una hora y un
  refresh token. Al arrancar, el binario renueva el access token y reescribe el archivo; por eso en
  la nube recibe una copia escribible del secreto. En las pruebas, la sesión sobrevivió a 9 caídas
  y a varios reinicios del contenedor sin volver a iniciar sesión.
- **Cuánto dura:** lo que dure el refresh token, y eso lo decide Google. Según sus reglas generales,
  se invalida si se revoca el acceso desde la cuenta, si no se usa durante 6 meses o, si la app
  OAuth de la cátedra está en modo de prueba, a los 7 días. No pudimos medirlo ni ver cómo está
  configurada esa app.
- **Sin sesión:** el binario no escucha y espera un inicio de sesión durante 5 minutos; después
  termina y la política de reinicio lo levanta con un link nuevo. El watchdog no lo interrumpe
  mientras espera. Si todavía no hay token, el backend ni lo llama (`The legacy service has not
  signed in yet`); si quedó el de una sesión anterior, la llamada falla. En los dos casos responde
  el proveedor principal y las estadísticas marcan al legacy como caído, porque su `/healthz` no
  responde mientras espera el inicio de sesión. Distinto es si el binario está vivo pero rechaza
  el token (`UNAUTHENTICATED`): también responde el proveedor principal, pero ese rechazo se trata
  como un pedido inválido y no como una falla del servicio, así que el circuit breaker no se abre
  y el legacy sigue figurando OK; los rechazos se ven en el desglose de errores
  (`REJECTED_REQUEST`).
- **Cómo se nota que venció:** el legacy figura caído en las estadísticas y sus logs muestran
  `SIGN IN` en lugar de `session restored`. En local se corre de nuevo `./legacy-weather/login.sh`;
  en la nube se repiten los pasos 1 y 3 y se despliega una revisión nueva (comando de arriba).

### Cuenta institucional utilizada

- `aherzkovich@frba.utn.edu.ar`

## Fallas descubiertas

Se detectaron ejecutando el binario en un contenedor y enviándole pedidos durante unos 30 minutos,
de a uno y en ráfagas de 40 simultáneos, mientras se consultaban sus health checks cada 2 segundos.
Los porcentajes son de esas corridas.

| Falla | Cómo se detectó | Mitigación |
| --- | --- | --- |
| Escucha solo en `127.0.0.1:9977`: ningún otro contenedor puede alcanzarlo | Lo imprime al arrancar; las conexiones desde otro contenedor se rechazan | `socat` dentro del mismo contenedor publica el puerto 9978 hacia la red de Compose (`legacy-weather/entrypoint.sh`) |
| El inicio de sesión espera la redirección de Google en `http://localhost:<puerto aleatorio>`, que el navegador no alcanza dentro de un contenedor | Primer arranque | `legacy-weather/login.sh` entrega la dirección de redirección dentro del contenedor |
| Si nadie inicia sesión en 5 minutos, el proceso termina con error | Logs: `timed out waiting for the callback` | La política de reinicio lo vuelve a levantar con un link nuevo |
| Mientras espera el inicio de sesión no escucha, así que el watchdog lo mataba a los ~40 s y el link cambiaba antes de poder usarlo | Logs: un `SIGN IN` nuevo cada 40 s; el callback de un link anterior no llega a ningún proceso | El watchdog solo vigila después de que `/healthz` respondió una vez; el link dura sus 5 minutos |
| Ignora los argumentos (`--help`) y no se configura por variables de entorno | Ejecución con `--help` | Todo se resuelve alrededor del binario, sin tocarlo |
| Se cae solo, con el mensaje `I don't want to do this anymore` o `Tell my goroutines I love them` / `server crashed` | 3 caídas en 17 minutos; el contenedor reinició | `restart: unless-stopped` en Compose; en Cloud Run el contenedor se reinicia solo. La sesión persiste, así que vuelve sin intervención |
| No responde nunca a algunos pedidos (~21 %) | Pedidos cortados a los 15 y 30 segundos sin respuesta | Timeout de lectura de 1,5 s y reintentos |
| Responde 200 con una página HTML `502 Bad Gateway` (~6 %) | Al parsear la respuesta | Validación: el cuerpo se parsea estrictamente; un error se reintenta |
| Responde 200 con JSON roto: coma de más, fecha sin comillas, respuesta cortada a la mitad (~14 %) | Al parsear la respuesta | Parser JSON estricto (Jackson sin tolerancias); se reintenta |
| Los errores vienen con HTTP 200 y el detalle en el cuerpo (`"status": "ERROR"`) | Pedidos inválidos a propósito | Se lee el `status` del sobre, nunca solo el código HTTP |
| Inventa un pronóstico para cualquier fecha, incluso pasadas o a un año | Pedidos con fechas fuera de rango | El backend aplica la misma ventana de 16 días que con el proveedor principal antes de consultarlo |
| `/healthz` y `/readyz` dicen OK aunque `/rpc` esté colgado; a veces responden `503 unresponsive` | Consultados en paralelo a los pedidos | Solo se usan para saber si el proceso vive (watchdog, sondas de Cloud Run, estado "caído"); nunca como prueba de que funciona |
| Entra solo en un modo `Unresponsive - every endpoint answers 503` del que no sale por sí mismo; el proceso sigue vivo | Logs del binario; ocurrió 2 veces en 15 minutos, una justo al empezar la prueba de carga | El watchdog del entrypoint lo reinicia tras 3 chequeos fallidos (~30 s); mientras tanto los 503 abren el circuit breaker y responde el proveedor principal |
| Responde siempre lo mismo para el mismo lugar y hora | Pedidos repetidos | Caché de una hora por lugar y hora |
| Reporta telemetría de uso a `tacs.dale.ar` y baja su configuración de ahí | Inspección del binario | Se le permite salir a Internet (Compose y Cloud Run lo permiten por defecto) |
| Una línea en blanco al final del API token (frecuente al cargar un secreto con `echo`) hace inválido el header `Authorization` | Revisión del código: el cliente HTTP rechaza el header con `IllegalArgumentException` | El token se recorta al leerlo, y cualquier falla inesperada del cliente termina en el fallback |
| Con el contenedor detenido, resolver su nombre (`legacy-weather`) tarda 8 a 9 s en fallar, y el timeout de conexión del cliente HTTP no cubre esa espera | Prueba de carga con el legacy caído: p99 de 4,7 s; medido con `getent hosts` dentro del backend | Presupuesto total por intento (conexión + lectura = 2 s) que incluye la resolución; un servicio inalcanzable no se reintenta |
| Al detener el contenedor, `docker compose stop` quedaba colgado: el `sh` del entrypoint corre como PID 1 y no reenvía señales | Al detenerlo para la prueba de carga | `init: true` en Compose (un init mínimo reenvía las señales) |

Con todo eso, cerca del 42 % de los intentos individuales fallan; con 3 intentos, alrededor del 7 %
de las consultas no consigue respuesta y se resuelve con el fallback.

## Prueba de carga

Escenario `legacy` de `loadtest/run.sh`: antes de empezar, cada usuario de prueba elige el proveedor
legacy; los targets consultan el clima (3 veces el de la actividad propia y una vez el de una
actividad del seed), el listado y el detalle. Etapas de 60 s a 10, 50 y 100 requests por segundo,
con el backend recién reiniciado (caché vacía) y la cuota por usuario desactivada
(`LEGACY_WEATHER_USER_REQUESTS_PER_MINUTE=100000`) para medir el resto de los mecanismos.

```bash
WEATHER_PROVIDER=in-memory LEGACY_WEATHER_USER_REQUESTS_PER_MINUTE=100000 docker compose up -d --wait
BASE_URL='http://[::1]:8080' ./loadtest/run.sh legacy 10/s 50/s 100/s
```

Para el escenario con el legacy caído, antes se corre `docker compose stop legacy-weather`.

Resultados (20/10/2026, Docker Desktop en Windows):

| Escenario | Etapa | Respuestas 200 | p50 | p95 | p99 | Resultado |
| --- | --- | --- | --- | --- | --- | --- |
| Legacy funcionando | 10/s | 100 % de 600 | 6 ms | 17 ms | 723 ms | PASS |
| Legacy funcionando | 50/s | 100 % de 3000 | 3 ms | 7 ms | 9 ms | PASS |
| Legacy funcionando | 100/s | 100 % de 6000 | 2 ms | 6 ms | 7 ms | PASS |
| Legacy caído | 10/s | 100 % de 600 | 7 ms | 212 ms | 3728 ms | PASS |
| Legacy caído | 50/s | 100 % de 3000 | 4 ms | 7 ms | 9 ms | PASS |
| Legacy caído | 100/s | 100 % de 6000 | 3 ms | 6 ms | 7 ms | PASS |

Lo que registraron las estadísticas del proveedor legacy:

- **Legacy funcionando:** el binario entró en modo `Unresponsive` 7 s antes de empezar, así que la
  prueba cubrió también esa falla. Hubo 58 llamadas (7 correctas; 43 `HTTP_ERROR` por los 503 y 8
  `MALFORMED_RESPONSE`), el circuit breaker se abrió (403 consultas rechazadas sin llamar) y 429
  consultas se resolvieron con el fallback. El watchdog lo reinició a los 30 s, el circuit breaker
  volvió a cerrarse y el resto de la prueba se sirvió desde la caché: 96,6 % de aciertos.
- **Legacy caído:** 71 llamadas, todas fallidas (33 `UNREACHABLE`, 38 `TIMEOUT`); el circuit breaker
  quedó abierto (12.716 consultas rechazadas sin llamar), el estado del proveedor quedó en "caído" y
  las 12.800 consultas se resolvieron con el fallback.

La latencia alta de la primera etapa con el legacy caído son las consultas anteriores a que se abra
el circuit breaker: cada una espera hasta el presupuesto de 2 s por intento. Una vez abierto, el
fallback responde en milisegundos. La primera versión, sin presupuesto total y reintentando un
servicio inalcanzable, tenía p95 de 653 ms y p99 de 4,7 s en esa etapa, y no pasaba el umbral.

## Consumo desde el backend

Código: `backend/src/main/java/com/solnotfound/adapters/legacy/` y
`backend/src/main/java/com/solnotfound/service/WeatherGateway.java`.

Una consulta pasa, de afuera hacia adentro, por:

1. **Límite por usuario** (`WeatherGateway`): 30 consultas por minuto por usuario hacia el legacy.
2. **Caché** (`LegacyWeatherProvider`): lugar redondeado a 2 decimales más la hora.
3. **Circuit breaker** sobre la consulta ya reintentada.
4. **Reintentos** con backoff exponencial y jitter.
5. **Rate limiter** de la plataforma hacia el servicio.
6. **Bulkhead** (contrapresión): máximo de llamadas simultáneas.
7. **Una llamada** con timeouts y validación completa (`LegacyWeatherClient`).

Si cualquier paso no consigue un pronóstico válido, responde el proveedor principal y la respuesta
lo indica (`fallback: true`); la pantalla de la actividad muestra el aviso. El chequeo periódico de
cada actividad usa el proveedor de su organizador, con el mismo fallback.

## Decisiones (ADRs)

### ADR 1 — Correr el binario sin modificarlo, con un adaptador en el mismo contenedor

- **Contexto:** el binario escucha solo en loopback, inicia sesión con una redirección a localhost
  y no acepta configuración.
- **Decisión:** una imagen Debian mínima con el binario, `socat` para publicar el puerto y un
  `entrypoint.sh` que maneja la sesión, copia el token y vigila la salud.
- **Alternativas descartadas:** `network_mode: host` (no funciona igual en Docker Desktop ni en
  Cloud Run) y un proxy en otro contenedor (no puede alcanzar el loopback de otro contenedor).

### ADR 2 — Mantenerlo vivo con la plataforma, no con un supervisor propio

- **Decisión:** si el proceso termina, termina el contenedor y lo reinicia la plataforma
  (`restart: unless-stopped` en Compose, Cloud Run en la nube). Un watchdog en el entrypoint mata
  el proceso si `/healthz` falla 3 veces seguidas, porque Docker no reinicia contenedores
  "unhealthy"; en Cloud Run lo hace la sonda de liveness. El watchdog empieza a contar recién
  cuando `/healthz` respondió una vez, para no matar al binario mientras espera un inicio de
  sesión.
- **Por qué:** el reinicio es la operación que la plataforma ya sabe hacer bien, y la sesión
  persiste fuera del proceso.

### ADR 3 — Timeouts

- **Decisión:** 500 ms para conectar y 1,5 s de lectura, dentro de un presupuesto total de 2 s por
  intento que también cubre la resolución del nombre.
- **Por qué:** todas las respuestas válidas medidas tardaron entre 3 ms y 1 s; las que no llegan
  en ese tiempo no llegaron nunca (se esperaron hasta 30 s). Esperar más solo retiene recursos.
  El presupuesto total hizo falta porque, con el contenedor detenido, la resolución del nombre
  tardaba 8 a 9 s y ningún timeout del cliente HTTP la cortaba.

### ADR 4 — Reintentos: 3 intentos, backoff exponencial con jitter

- **Decisión:** 3 intentos con esperas de 100 ms y 200 ms, ±50 % aleatorio. Se reintentan las
  fallas transitorias (cuelgues, respuestas rotas, errores del servicio). No se reintentan un pedido
  que el servicio rechaza por inválido ni un servicio inalcanzable: el proceso tarda segundos en
  volver y reintentar a los 100 ms solo demora el fallback. Ambas fallas del proveedor siguen
  contando para el circuit breaker (`LegacyRetryPredicate` y `LegacyProviderFaultPredicate`).
- **Por qué:** con ~42 % de fallas por intento, 3 intentos dejan ~7 % de consultas fallidas y un
  peor caso de ~5 s. El jitter evita que los reintentos de muchas consultas lleguen juntos.

### ADR 5 — Circuit breaker alrededor de la consulta reintentada

- **Decisión:** ventana de las últimas 20 consultas (mínimo 10), se abre con 50 % de fallas, queda
  abierto 30 s y prueba con 3 consultas antes de cerrarse.
- **Por qué:** al medir consultas ya reintentadas, la tasa normal de falla es ~7 %; el 50 % solo se
  alcanza cuando el servicio está realmente caído. Medir intentos sueltos (~42 % de falla normal)
  lo abriría por error con frecuencia. Mientras está abierto no se envían llamadas que van a
  fallar y el fallback responde al instante. Las consultas que rechazan el bulkhead o el rate
  limiter se ignoran (`LegacyRefusalPredicate`): no llegaron al servicio, y contarlas como éxitos
  diluiría las fallas justo cuando el servicio está colgado.

### ADR 6 — Caché por lugar y hora, solo de respuestas válidas

- **Decisión:** Caffeine en memoria, una hora de vida, hasta 10.000 entradas. Nunca se guarda una
  falla.
- **Por qué:** el servicio responde lo mismo para el mismo lugar y hora, y el pronóstico horario no
  cambia dentro de esa hora. Reduce al mínimo las llamadas a un proveedor que falla seguido.

### ADR 7 — Rate limiting hacia el proveedor y por usuario

- **Decisión:** como máximo 20 llamadas por segundo de cada instancia al servicio (lo que no
  consigue permiso en 100 ms va al fallback) y 30 consultas por minuto por usuario.
- **Por qué:** protege al servicio de la plataforma y a la plataforma de un usuario que lo
  monopolice. Al superar el límite el usuario no recibe un error: lo atiende el proveedor principal.
- **Detalle:** la cuota del usuario se descuenta solo cuando la consulta va a llegar al servicio.
  Un pronóstico que ya está en caché no le cuesta nada al proveedor, así que tampoco al usuario.
  La primera versión cobraba también los aciertos de caché y, en la prueba de carga, casi todas
  las consultas terminaban en el fallback por cuota aunque el legacy estuviera sano.

### ADR 8 — Bulkhead como contrapresión

- **Decisión:** máximo 10 llamadas simultáneas; las demás esperan como mucho 100 ms y van al
  fallback.
- **Por qué:** una llamada colgada ocupa su lugar hasta el timeout. Sin límite, una racha de
  cuelgues acumularía hilos del backend esperando.

### ADR 9 — Fallback al proveedor principal, informado al usuario

- **Decisión:** si el legacy no puede responder, responde Open-Meteo y la respuesta lo marca; la
  pantalla muestra el aviso. Un rango de alternativas se resuelve completo con el mismo proveedor.
- **Por qué:** la aplicación nunca queda sin clima por culpa del proveedor elegido, y el usuario no
  es engañado sobre de dónde vienen los datos.
- **Estadísticas best-effort:** los eventos del proveedor se registran en medio de la consulta,
  muchas veces justo antes del fallback. Si no se pueden guardar, se registra un warning y la
  consulta sigue; las estadísticas nunca dejan a la aplicación sin clima. Los rechazos y fallbacks,
  que con el legacy caído ocurren en cada consulta, se cuentan en memoria y se guardan con cada
  foto de los proveedores: el camino del fallback no escribe en la base.

### ADR 10 — Topología en la nube: servicio privado en Cloud Run

- **Decisión:** un servicio de Cloud Run sin invocador público; solo la cuenta de servicio del
  backend tiene `roles/run.invoker`. El backend presenta un token de identidad en
  `X-Serverless-Authorization`, porque `Authorization` lleva el token del legacy. Una única
  instancia como máximo.
- **Alternativas descartadas:** ingress interno con un conector VPC (costo y complejidad para un
  único consumidor) y exponerlo público con el token como única protección.


# Prueba de carga

La prueba de carga usa [Vegeta](https://github.com/tsenart/vegeta) mediante el script versionado
`loadtest/run.sh`. El script saca tokens reales de Keycloak, arma los targets, ejecuta la prueba por
etapas de tasa creciente y decide si cada etapa pasa o no según umbrales fijos. La ejecución
presentada en la Entrega 3 está en [Ejecución registrada](#ejecución-registrada-entrega-3); para
nuevas corridas ver [Registro](#registro).

## Alcance y precauciones

- Por defecto se corre **contra el stack local** (`docker compose`). El cliente de Keycloak que
  habilita el login por usuario y contraseña (`planazo-loadtest`) y los usuarios
  `loadtest01`..`loadtest10` los crea únicamente `keycloak/configure-local.sh`. No están en
  `realm-export.json` y no existen en GCP.
- Si `BASE_URL` o `KEYCLOAK_URL` no apuntan a `localhost`, el script se niega a correr salvo que se
  pasen `ALLOW_REMOTE=1` y `LOADTEST_PASSWORD`. Ver [Prueba contra GCP](#prueba-contra-gcp).
- Usar `WEATHER_PROVIDER=in-memory` para no trasladar la carga a Open-Meteo.
- Se prueba el backend directo (`http://localhost:8080`). Para medir también el proxy de Next.js se
  puede apuntar `BASE_URL` a `http://localhost:3000/api`, pero así se mezclan dos servicios en la
  misma medición.
- El escenario `mixed` escribe en MongoDB (crea actividades y suma participantes). Hay que usar un
  ambiente descartable.

## Requisitos

- Docker con Docker Compose.
- `vegeta` (probado con 12.13), `jq`, `curl` y `bash` (en Windows: Git Bash o WSL).

## Preparación

Desde la raíz del repositorio:

```bash
WEATHER_PROVIDER=in-memory APP_SEED_ENABLED=true docker compose up --build --wait
curl --fail http://localhost:8080/healthcheck
```

`--wait` espera también a que `keycloak-config` termine de crear el cliente y los usuarios de la
prueba. Para crear más usuarios, levantar el stack con `LOADTEST_USERS=<n>` y pasar el mismo valor
al script:

```bash
LOADTEST_USERS=20 WEATHER_PROVIDER=in-memory APP_SEED_ENABLED=true docker compose up --build --wait
LOADTEST_USERS=20 ./loadtest/run.sh read
```

## Ejecución

```bash
./loadtest/run.sh smoke            # GET /healthcheck, sin autenticación
./loadtest/run.sh read             # lecturas autenticadas
./loadtest/run.sh mixed            # ~80% lecturas y ~20% escrituras
./loadtest/run.sh read 20/s 40/s   # otras etapas
```

Por defecto cada escenario corre tres etapas (`10/s`, `50/s`, `100/s`) de 60 segundos. El script se
detiene en la primera etapa que no cumple los umbrales y en ese caso sale con código distinto de 0.
Cada etapa pide tokens nuevos porque el access token dura 5 minutos.

Variables de entorno:

| Variable | Default | Uso |
| --- | --- | --- |
| `BASE_URL` | `http://localhost:8080` | API contra la que se corre la prueba. |
| `KEYCLOAK_URL` | `http://localhost:8090` | Keycloak (el issuer tiene que coincidir con el del backend). |
| `LOADTEST_USERS` | `10` | Cantidad de usuarios `loadtestNN` a usar. |
| `LOADTEST_USER_PREFIX` | `loadtest` | Prefijo de los usuarios (`<prefijo>01`, `<prefijo>02`, ...). |
| `LOADTEST_PASSWORD` | igual al usuario | Contraseña de todos los usuarios de prueba. Obligatoria fuera de local. |
| `ALLOW_REMOTE` | vacío | `1` para permitir un destino que no es `localhost`. |
| `INCLUDE_WEATHER` | `true` local, `false` remoto | Incluir `GET /activities/{id}/weather`. |
| `RATES` | `10/s 50/s 100/s` (remoto: `5/s 10/s 20/s 50/s`) | Etapas si no se pasan como argumentos. |
| `DURATION` | `60s` | Duración de cada etapa. |
| `TIMEOUT` | `10s` | Timeout por request. |
| `MIN_OK_RATIO` | `0.99` | Proporción mínima de respuestas 2xx/3xx. |
| `MAX_P95_MS` | `500` | Latencia p95 máxima, en milisegundos. |

## Escenarios

Antes de `read` y `mixed`, el script arma los datos de base: cada usuario crea una actividad propia
y se toman hasta 50 ids de `GET /activities` (incluye las actividades del seed).

| Escenario | Requests por usuario |
| --- | --- |
| `smoke` | `GET /healthcheck` |
| `read` | `GET /activities` (paginado, con filtros por tipo/estado y por ciudad/disponibilidad), `GET /activities/{id}`, `GET /activities/{id}/weather` sobre su propia actividad, `GET /activities/organizers/me`, `GET /activities/participants/me`, `GET /votations`, `GET /notifications` |
| `mixed` | Todo lo de `read`, más `POST /activities` (multipart, sin imágenes) y `PUT /activities/{id}/participants/me` sobre la actividad de otro usuario |

Los targets se reparten entre los usuarios y Vegeta los recorre en round-robin, así que cada request
viaja con el JWT de un usuario distinto, igual que en uso real.

## Criterios

Una etapa pasa si se cumplen todas estas condiciones:

- al menos 99% de respuestas 2xx/3xx;
- ninguna respuesta `5xx` y ningún error de conexión o timeout (código `0` en Vegeta);
- latencia p95 menor a 500 ms.

Son una línea base del equipo para el stack local, no una garantía productiva.

## Resultados

Cada ejecución guarda sus resultados en `loadtest/results/<fecha>-<escenario>/<tasa>/`. Esa carpeta
está en `.gitignore` y no se versiona salvo que se acuerde como evidencia de una entrega.

| Archivo | Contenido |
| --- | --- |
| `targets.jsonl` | Targets usados, **con tokens válidos** (no compartirlo). |
| `results.bin` | Resultados crudos de Vegeta. |
| `report.txt`, `report.json` | Reporte de texto y en JSON. |
| `histogram.txt` | Histograma de latencias. |
| `plot.html` | Gráfico interactivo de latencia en el tiempo. |

Para volver a analizar una corrida: `vegeta report -type=text <ruta>/results.bin`. Las carpetas de
resultados se crean con permisos solo para el usuario actual, porque `targets.jsonl` contiene tokens.

## Ejecución registrada (Entrega 3)

**Todas las etapas pasaron los umbrales, sin errores ni respuestas `5xx`.**

| Dato | Valor |
| --- | --- |
| Fecha | 29/09/2026 |
| Commit | `f47bfbc` (`develop` al crear `release/Entrega_03`), más la corrección de CRLF de `run.sh` |
| Host | AMD Ryzen 5 8400F (6 núcleos / 12 hilos), 15,6 GB de RAM, Windows 11 Pro |
| Docker | Docker Desktop 29.8 (WSL 2), 12 CPUs y 8 GB asignados |
| Stack | `WEATHER_PROVIDER=in-memory APP_SEED_ENABLED=true docker compose up --build --wait` |
| Vegeta | v12.13.0, 10 usuarios, 60 s por etapa, timeout 10 s |
| Destino | `BASE_URL=http://[::1]:8080` (ver nota de Windows abajo) |

| Escenario | Etapa | Requests | Throughput | Éxito | Códigos | p50 | p95 | p99 |
| --- | --- | ---: | ---: | ---: | --- | ---: | ---: | ---: |
| `smoke` | 10/s | 600 | 10,0/s | 100 % | 200 | 2 ms | 3 ms | 4 ms |
| `smoke` | 50/s | 3.000 | 50,0/s | 100 % | 200 | 1 ms | 2 ms | 3 ms |
| `smoke` | 100/s | 5.999 | 100,0/s | 100 % | 200 | 1 ms | 1 ms | 2 ms |
| `read` | 10/s | 600 | 10,0/s | 100 % | 200 | 4 ms | 10 ms | 11 ms |
| `read` | 50/s | 3.000 | 50,0/s | 100 % | 200 | 3 ms | 8 ms | 10 ms |
| `read` | 100/s | 5.999 | 100,0/s | 100 % | 200 | 3 ms | 8 ms | 10 ms |
| `mixed` | 10/s | 600 | 10,0/s | 100 % | 546×200, 54×201 | 4 ms | 8 ms | 10 ms |
| `mixed` | 50/s | 3.000 | 50,0/s | 100 % | 2.728×200, 272×201 | 4 ms | 8 ms | 9 ms |
| `mixed` | 100/s | 6.000 | 100,0/s | 100 % | 5.455×200, 545×201 | 4 ms | 8 ms | 9 ms |
| `read` (extra) | 200/s | 11.998 | 200,0/s | 100 % | 200 | 4 ms | 8 ms | 10 ms |
| `read` (extra) | 400/s | 24.000 | 400,0/s | 100 % | 200 | 4 ms | 9 ms | 10 ms |
| `read` (extra) | 800/s | 47.999 | 800,0/s | 100 % | 200 | 4 ms | 9 ms | 24 ms |

Las etapas extra de `read` (`./loadtest/run.sh read 200/s 400/s 800/s`) se corrieron para buscar
el límite. A 800 req/s todavía se cumplen los umbrales con amplio margen; no se siguió subiendo,
así que la capacidad real del stack local es mayor que la medida.

Consumo máximo observado con `docker stats` durante todas las corridas:

| Contenedor | CPU máx. | Memoria máx. |
| --- | ---: | ---: |
| backend | 456 % (≈4,6 núcleos) | 477 MiB |
| mongodb | 245 % | 434 MiB |
| keycloak | 38 % | 640 MiB |
| frontend, minio, keycloak-postgres, telegram | < 20 % | < 150 MiB |

El backend y MongoDB son los que absorben la carga. Keycloak solo interviene al emitir los tokens
al comienzo de cada etapa, porque el backend valida los JWT localmente con el JWKS. En los logs del
backend no hubo errores asociados a las requests de la prueba.

### Notas para correrla en Windows

- **Otro programa en el puerto 8080.** En el equipo de la medición, un proceso ajeno
  (`ApplicationWebServer.exe`) escuchaba en `0.0.0.0:8080` por IPv4, y Vegeta resuelve `localhost`
  a `127.0.0.1`, así que recibía `404` de ese proceso. `curl` usa IPv6 primero y por eso el
  healthcheck respondía bien. Se evitó apuntando a `BASE_URL=http://[::1]:8080`. Si pasa lo mismo,
  revisar el puerto con `netstat -ano | findstr :8080`.
- **`jq.exe` y CRLF.** El `jq` nativo de Windows termina sus líneas con CRLF y los ids quedaban con
  un `\r` que Vegeta rechazaba (`invalid control character in URL`). `run.sh` lo elimina desde esta
  entrega.

## Registro

Para cada ejecución que se presente, registrar:

- commit y fecha;
- CPU, memoria y sistema operativo del host, y los recursos asignados a Docker;
- escenario, etapas, duración y versión de Vegeta;
- requests, throughput real, porcentaje de éxito, códigos HTTP y latencias p50/p95/p99 (salen del
  resumen que imprime el script);
- uso máximo de CPU y memoria (`docker stats` en otra terminal mientras corre la prueba) y errores
  en `docker compose logs backend`.

## Limpieza

Después de la prueba, comprobar que `curl --fail http://localhost:8080/healthcheck` sigue
respondiendo.

`loadtest/cleanup.js` borra solo lo que creó la prueba:
- las actividades cuyo título empieza con `Load test ` **y** que tienen la descripción fija del
  script;
- las votaciones, notificaciones y eventos de estadísticas de esas actividades;
- los usuarios organizadores de esas actividades que no queden referenciados en ningún otro
  documento.

Por defecto corre en modo de prueba y solo informa cuántos documentos borraría:

```bash
# Local (mongosh dentro del contenedor)
docker compose cp loadtest/cleanup.js mongodb:/tmp/cleanup.js
docker compose exec -e DB_NAME=mi_base_de_datos mongodb \
  mongosh -u admin -p password123 --quiet /tmp/cleanup.js
# Revisar lo que informa y aplicar
docker compose exec -e DB_NAME=mi_base_de_datos -e APPLY=1 mongodb \
  mongosh -u admin -p password123 --quiet /tmp/cleanup.js

# Atlas (mongosh local; la URI sale del secreto de la aplicación)
DB_NAME=<base> mongosh "<uri de Atlas>" loadtest/cleanup.js
APPLY=1 DB_NAME=<base> mongosh "<uri de Atlas>" loadtest/cleanup.js
```

En el ambiente local también se puede usar `docker compose down -v`, que detiene el stack y
**borra todos los volúmenes**. Usarlo solo en el ambiente de la prueba.

## Prueba contra GCP

En GCP el cuello de botella no es la app sino la infraestructura del despliegue (ver `terraform/`):

- **Atlas M0:** tier compartido con topes de operaciones por segundo, conexiones y transferencia.
- **Backend en Cloud Run:** hasta 2 instancias de 1 vCPU, con escalado desde 0 (*cold starts*).
- **Keycloak:** 1 instancia, sobre Cloud SQL `db-f1-micro`.

Por eso el límite va a ser mucho menor que en local. El resultado describe ese ambiente y no la
capacidad de la aplicación.

### Antes

1. **Coordinar con el equipo.** Acordar una ventana que no coincida con demos ni correcciones:
   Atlas M0 puede quedar limitado para todos mientras dura la prueba.
2. **Crear en el realm de GCP un cliente y usuarios temporales**, por la consola de administración
   o con `kcadm.sh`. `configure-local.sh` no se aplica en la nube. El cliente necesita:
   - público, con *Direct access grants* habilitado y *Standard flow* deshabilitado;
   - los client scopes por defecto `basic`, `profile`, `email`, `roles`, `web-origins` y `acr` (sin
     `basic` el token no trae `sub`);
   - un mapper *Audience* que incluya `solnotfoundBackend` en el access token (sin él, el backend
     responde 401).

   Los usuarios (`loadtest01`..`loadtestNN`, o el prefijo que se elija) llevan todos una
   **contraseña aleatoria y larga**, nunca igual al usuario. Esa contraseña no se commitea.
3. **Lanzar la carga desde una VM en la misma región que Cloud Run** (por ejemplo `e2-standard-2`),
   no desde una conexión hogareña: el ancho de banda y la latencia de internet contaminan la
   medición.
4. **Verificar sin generar carga:** que `/healthcheck` responda y que el endpoint de token devuelva
   un `access_token` con `sub` y `aud` correctos.

### Durante

```bash
ALLOW_REMOTE=1 \
BASE_URL=https://<backend> \
KEYCLOAK_URL=https://<keycloak> \
LOADTEST_PASSWORD='<contraseña temporal>' \
./loadtest/run.sh read
```

- **Calentar:** antes de medir, correr unos minutos de `smoke` o `read 2/s` para que Cloud Run tenga
  instancias activas. Si no, se mide el *cold start*.
- **Empezar con `read`:** las etapas por defecto son `5/s 10/s 20/s 50/s`. `/weather` queda
  excluido para no cargar Open-Meteo.
- **Usar `mixed` solo en una corrida corta y con tasa baja:** sus actividades aparecen en Explorar
  para los usuarios reales e inflan las estadísticas del panel de administración.
- **Distinguir los `429` de los `5xx`:** un `429` significa que Cloud Run llegó al máximo de
  instancias; un `5xx`, que falló la app.
- **Observar:** en Cloud Run, instancias, latencia, CPU, 429 y 5xx; en Cloud Logging, excepciones; en
  Atlas, operaciones, conexiones y avisos de *throttling*.

### Después

1. Ejecutar `loadtest/cleanup.js` contra Atlas: primero en modo de prueba, después con `APPLY=1`.
2. Borrar del realm el cliente y los usuarios temporales.
3. Comprobar `/healthcheck` y el login desde el frontend.
4. Borrar la VM de carga y revertir cualquier cambio temporal de infraestructura (por ejemplo,
   `min_instance_count`).
5. Borrar o guardar en un lugar privado `loadtest/results/`: los tokens expiran a los 5 minutos,
   pero no dejan de ser credenciales.

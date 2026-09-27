# Prueba de carga

La prueba de carga usa [Vegeta](https://github.com/tsenart/vegeta) mediante el script versionado
`loadtest/run.sh`. El script saca tokens reales de Keycloak, arma los targets, ejecuta la prueba por
etapas de tasa creciente y decide si cada etapa pasa o no según umbrales fijos. No incluye resultados
históricos: en cada entrega hay que correrla en un ambiente limpio y registrar la ejecución (ver
[Registro](#registro)).

## Alcance y precauciones

- Correrla **solo contra el stack local** (`docker compose`). El cliente de Keycloak que habilita el
  login por usuario y contraseña (`planazo-loadtest`) y los usuarios `loadtest01`..`loadtest10` los
  crea únicamente `keycloak/configure-local.sh`. No están en `realm-export.json` y no existen en GCP.
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
al script.

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
| `RATES` | `10/s 50/s 100/s` | Etapas si no se pasan como argumentos. |
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
| `targets.jsonl` | Targets usados, con los tokens (no compartirlo). |
| `results.bin` | Resultados crudos de Vegeta. |
| `report.txt`, `report.json` | Reporte de texto y en JSON. |
| `histogram.txt` | Histograma de latencias. |
| `plot.html` | Gráfico interactivo de latencia en el tiempo. |

Para volver a analizar una corrida: `vegeta report -type=text <ruta>/results.bin`.

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
respondiendo. `docker compose down -v` detiene el stack y **borra los volúmenes** (incluidas las
actividades creadas por la prueba). Usarlo solo en el ambiente de la prueba.

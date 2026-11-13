# Observabilidad de la búsqueda de actividades (Entrega 5)

Se instrumentó con OpenTelemetry **la búsqueda de actividades**: desde que el usuario busca en
Explorar hasta que el backend consulta MongoDB y el resultado vuelve a la pantalla. Las trazas se
ven en Jaeger y las métricas en un dashboard de Grafana. Todo corre en local con Docker Compose;
en la nube no se despliega nada de esto.

## Puesta en marcha

Un solo comando, desde la raíz del repositorio:

```bash
docker compose up --build --wait
```

| Herramienta | Dirección | Acceso |
| --- | --- | --- |
| Aplicación | <http://localhost:3000> | `alumno` / `alumno` (usuario de desarrollo) |
| Grafana | <http://localhost:3001> | Sin login para ver; `admin` / `admin` para editar (solo desarrollo) |
| Jaeger | <http://localhost:16686> | Sin login |
| Prometheus | <http://localhost:9090> | Sin login |
| API del backend | <http://localhost:8080> | Token de Keycloak |

El dashboard **Planazo - Búsqueda de actividades** es la página de inicio de Grafana.

Para tener actividades que buscar, la primera vez conviene levantar con el seed:
`APP_SEED_ENABLED=true docker compose up --build --wait`.

## Recorrido de las señales

```
Navegador (planazo-frontend-web) ──OTLP/HTTP──┐
Servidor Next.js (planazo-frontend) ─OTLP/HTTP─┼─► OpenTelemetry Collector ──OTLP──► Jaeger (trazas)
Backend + agente Java (planazo-backend) ─OTLP──┘          │
                                                           └─ :8889/metrics ◄── Prometheus ◄── Grafana
```

- **Trazas:**
  1. Cada componente exporta sus spans por OTLP/HTTP al Collector (`observability/collector/config.yaml`).
  2. El navegador los manda directo al puerto 4318 publicado, con CORS habilitado solo para
     `http://localhost:3000`.
  3. El Collector borra las query strings (`url.query`, y lo que viene después de `?` en URLs y nombres
     de span), para que el texto buscado no quede guardado.
  4. Marca toda la telemetría con `deployment.environment.name=local`.
  5. Después exporta a Jaeger por OTLP gRPC.
- **Métricas:**
  1. El backend exporta sus métricas por OTLP cada 5 s.
  2. El Collector las expone en formato Prometheus en `:8889`.
  3. Prometheus las recolecta cada 5 s (`observability/prometheus/prometheus.yml`).
  4. Grafana consulta a Prometheus. El datasource y el dashboard están provisionados como código
     en `observability/grafana/`.
- **Contexto entre componentes:** cada salto propaga el header W3C `traceparent`, así que los spans
  del navegador, de Next.js y del backend quedan en **una sola traza**.

## Qué se instrumentó y cómo

| Etapa | Span | Instrumentación |
| --- | --- | --- |
| Búsqueda en Explorar | `activities.search` (planazo-frontend-web) | **Manual** (`lib/telemetry/searchTrace.ts`, usado por `hooks/useActivities.ts`). Va desde que se pide la búsqueda hasta que el resultado queda en el estado para mostrarse |
| Pedido del navegador a la API de Next | `HTTP GET /api/activities` (CLIENT) | **Manual** (`lib/api.ts`); inyecta `traceparent` |
| Ruta de API de Next (proxy) | `GET /api/activities`, `executing api route (pages) /api/activities` | **Automática** (Next.js) |
| Pedido de Next al backend | `fetch GET http://backend:8080/activities` | **Automática** (`@vercel/otel`, `src/instrumentation.ts`) |
| Recepción en el backend | `GET /activities` (SERVER) | **Automática** (agente Java, Tomcat y Spring MVC) |
| Operación de negocio | `activities.search` (planazo-backend) | **Manual** (`observability/ActivitySearchTelemetry.java`) |
| Accesos a MongoDB | `find activities` (la página), un `find users` (participantes y organizadores de la página) y, solo si hace falta, el `count` del total (`aggregate activities`) | **Automática** (agente Java, driver de MongoDB) |
| Demora controlada (opcional) | `activities.search.simulated_delay` | **Manual**, solo si está activada |

**Por qué el pedido del navegador es manual:** en el navegador el contexto activo se pierde después
de un `await`, y `request()` espera el token de Keycloak antes de hacer el `fetch`. La
auto-instrumentación de `fetch` crearía su span después de ese `await`, en una traza aparte. Por eso
el span se abre al principio de `request()`, antes de esperar nada, y el `traceparent` se inyecta a
mano. Solo se hace cuando hay una búsqueda en curso; el resto de las llamadas no se toca.

### Atributos de los spans

Sirven para interpretar el recorrido sin datos personales: **nunca** se registra el texto buscado,
la ciudad, identificadores, tokens ni usuarios.

| Atributo | Dónde | Valor |
| --- | --- | --- |
| `search.filter.type` | Ambos spans `activities.search` | Tipo de actividad o `any` |
| `search.filter.text`, `.city`, `.dates`, `.availability`, `.status` (y `.ids` en el backend) | Ambos | `true` si se usó ese filtro |
| `search.page`, `search.page_size` | Ambos | Página pedida |
| `search.results` | Ambos | Actividades devueltas en la página |
| `search.total` | Backend | Total que coincide con la búsqueda |
| `search.total_source` | Backend | `count` si se contó en MongoDB, `cache` si se reutilizó uno reciente; no aparece si la página alcanzó para saberlo |
| `search.cancelled` | Navegador | La búsqueda se reemplazó antes de terminar |
| `error.type` + estado ERROR | Cualquiera que falle | Clase del error |

### API, SDK y agente

- **API de OpenTelemetry** (`io.opentelemetry:opentelemetry-api`, `@opentelemetry/api`): las
  interfaces con las que el código crea spans y métricas (`Tracer`, `Meter`, `Span`). Sola no
  registra nada: sin un SDK detrás, todo es un no-op. Por eso los tests y la nube funcionan igual.
- **SDK:** la implementación que procesa los spans y métricas y los exporta. En el backend lo trae
  el agente; en el navegador es `@opentelemetry/sdk-trace-web` (`lib/telemetry/browserTracing.ts`);
  en Next.js lo registra `@vercel/otel`.
- **Agente Java** (instrumentación automática): un `-javaagent` que modifica el bytecode al arrancar
  para crear spans y métricas de Tomcat, Spring MVC y el driver de MongoDB sin tocar el código. También
  instala su SDK como implementación de la API, por eso los spans manuales quedan en la misma traza.
  Se descarga en `backend/Dockerfile`, pero solo se carga si `JAVA_TOOL_OPTIONS` lo indica, y eso
  pasa únicamente en `docker-compose.yaml`.

## Métricas

| Métrica (Prometheus) | Tipo y unidad | Qué representa y cuándo se registra | Atributos | Origen |
| --- | --- | --- | --- | --- |
| `http_server_request_duration_seconds` (`_count`, `_bucket`, `_sum`) | Histograma, segundos | Duración de cada solicitud HTTP que atiende el backend, registrada al responder. `_count` es el **contador de solicitudes** | `http_route`, `http_request_method`, `http_response_status_code`, `error_type` | Automática (agente) |
| `planazo_activities_search_completed_total` | Contador, `{search}` | Búsquedas que terminaron bien y devolvieron una página, registradas al final de la operación de negocio. Una solicitud que falla no suma | `search_filter_type` (tipo o `any`) | Manual |
| `planazo_activities_search_db_duration_seconds` | Histograma, segundos (buckets de 1 ms a 5 s) | Duración de cada acceso a MongoDB propio de la búsqueda, registrada al terminar el acceso, aunque falle | `db_operation_name` (`find`: la página; `find_people`: participantes y organizadores; `count`: el total, solo cuando hace falta), `db_collection_name` | Manual |

**Solicitud recibida vs. búsqueda completada:** el histograma HTTP cuenta toda solicitud a
`GET /activities`, termine como termine; el contador de búsquedas solo las que el backend completó.
La diferencia entre ambas son las búsquedas que fallaron. Todos los atributos toman un conjunto chico
de valores fijos.

## Dashboard

`observability/grafana/dashboards/busqueda-actividades.json`. Todas las consultas HTTP filtran
`http_route="/activities", http_request_method="GET"`, y el intervalo se elige con el selector de
tiempo de Grafana.

| Panel | Consulta | Unidad |
| --- | --- | --- |
| Solicitudes por segundo | `sum(rate(http_server_request_duration_seconds_count{…}[$__rate_interval]))` | req/s |
| % de solicitudes fallidas | `100 * (sum(rate(…_count{…, http_response_status_code=~"5.."}[…])) or vector(0)) / sum(rate(…_count{…}[…]))` (los 4xx, aparte; `or vector(0)` muestra 0 % cuando no hubo errores en vez de dejar el panel vacío) | % |
| Tiempo de respuesta p95 | `histogram_quantile(0.95, sum by (le) (rate(http_server_request_duration_seconds_bucket{…}[…])))` | s |
| Búsquedas completadas | `sum(rate(planazo_activities_search_completed_total[…]))` | búsquedas/s |
| Acceso a la base p95 | `histogram_quantile(0.95, sum by (le, db_operation_name) (rate(planazo_activities_search_db_duration_seconds_bucket[…])))` | s |

- `rate(x[$__rate_interval])` calcula cuánto creció un contador por segundo dentro de la ventana.
- **El p95** es el valor por debajo del cual quedó el 95 % de las observaciones del intervalo: solo el
  5 % de las solicitudes tardó más. A diferencia del promedio, no lo esconden muchas respuestas
  rápidas. `histogram_quantile` lo estima a partir de los buckets del histograma (cuántas
  observaciones cayeron por debajo de cada límite), interpolando dentro del bucket. Por eso su
  precisión depende de los límites de los buckets.

## Demostrar una ejecución fallida

- **Error del servidor:** `docker compose stop mongodb` y buscar en Explorar.
  - El span de MongoDB del backend termina en error a los ~30 s, que es el timeout del driver para
    encontrar un servidor.
  - El error sube al span `activities.search` del backend, la solicitud responde 500 y el span del
    navegador queda en ERROR.
  - Se vuelve con `docker compose start mongodb`.
- **Error de validación:** una búsqueda con `dateFrom` posterior a `dateTo` (por ejemplo, con
  `curl` al backend). El span `activities.search` del backend queda en ERROR con
  `error.type=InvalidActivityException`, la solicitud responde 400 y no se llega a MongoDB.

## Demora controlada

`OBSERVABILITY_SEARCH_DELAY` agrega una espera a cada búsqueda, dentro del span
`activities.search.simulated_delay`, para estudiar una degradación provocada:

```bash
OBSERVABILITY_SEARCH_DELAY=200ms docker compose up -d backend
```

Para desactivarla: `OBSERVABILITY_SEARCH_DELAY=0ms docker compose up -d backend`, o simplemente
`docker compose up -d backend`. Por defecto vale `0ms`.

## Pruebas de carga

### Cómo reproducirlas

```bash
# 1. Stack con el clima simulado. La búsqueda no consulta el clima, pero así ninguna prueba
#    toca al proveedor real.
WEATHER_PROVIDER=in-memory docker compose up --build --wait

# 2. Una sola vez: llevar la colección a 50.000 actividades. Clona las existentes; si la base
#    está vacía, levantar antes con APP_SEED_ENABLED=true. SEARCH_VOLUME=0 borra los clones.
docker compose exec -T -e SEARCH_VOLUME=50000 mongodb mongosh --quiet \
  "mongodb://admin:password123@localhost:27017/mi_base_de_datos?authSource=admin" \
  --eval "$(cat loadtest/search-volume.js)"

# 3. Los dos escenarios, 3 minutos cada uno, con 30 s de pausa entre ellos.
DURATION=3m ./loadtest/run.sh search 5c 50c
```

`loadtest/run.sh search` (Vegeta) usa 10 usuarios de prueba con 5 búsquedas cada uno, 50 pedidos
distintos a `GET /activities`:
- sin filtros;
- por texto, con un término de un conjunto fijo;
- por ciudad y disponibilidad;
- por tipo y estado, página 2;
- por texto y tipo.

Cada etapa escribe en `loadtest/results/<fecha>-search/<etapa>/window.txt` su intervalo UTC, para
elegirlo en Grafana y en Jaeger. El escenario no crea datos, así que las dos etapas buscan
exactamente sobre los mismos documentos.

### Condiciones

| | Referencia (`5c`) | Alta concurrencia (`50c`) |
| --- | --- | --- |
| Carga | 5 usuarios concurrentes (modelo cerrado: cada uno manda el pedido siguiente al recibir la respuesta) | 50 usuarios concurrentes |
| Duración | 3 min | 3 min |
| Intervalo (UTC, 10/11/2026) | 02:41:03 a 02:44:04 | 02:44:49 a 02:47:49 |

- **Datos:** iguales en ambas etapas, 50.000 actividades (69 originales y 49.931 clones).
- **Herramienta:** Vegeta v12.13.0, ejecutado en el host contra `http://[::1]:8080`.
- **Recursos:** Docker Desktop 29.9.3 en Windows 11, con 12 CPU y 7,6 GiB de RAM para la VM de
  Docker.
  - El backend y MongoDB **no tienen límites** de CPU ni de memoria.
  - Los contenedores de observabilidad tienen límite de memoria: 256 a 512 MB.
- **Configuración:** la misma en las dos etapas, y la demora controlada apagada.

### Resultados antes de la mejora

![Dashboard durante las dos etapas](img/observabilidad/dashboard-carga.png)

| Métrica | `5c` | `50c` | Fuente |
| --- | --- | --- | --- |
| Solicitudes procesadas | 14.176 (78,7/s) | 14.347 (79,5/s) | Vegeta |
| Errores | 0 % | 0 % | Vegeta y dashboard |
| Tiempo de respuesta p50 / p95 / p99 (cliente) | 54 / 143 / 192 ms | 472 / 1.724 / 2.020 ms | Vegeta |
| p95 del backend (`http.server.request.duration`) | 0,21 s | 2,13 s | Prometheus, ventana de 2 min |
| p95 del `count` / `find` (`planazo.activities.search.db.duration`) | 0,195 s / 0,022 s | 2,119 s / 0,315 s | Prometheus |
| Promedio del `count` / `find` | 46 ms / 5 ms | 449 ms / 107 ms | Prometheus (`_sum / _count`) |
| CPU de MongoDB / backend (promedio) | 455 % / 101 % | 866 % / 114 % | `docker stats` cada 15 s |

Trazas representativas: una con 5 usuarios (112 ms) y una de las lentas con 50 (1.659 ms).

![Traza de referencia con 5 usuarios](img/observabilidad/traza-referencia-5c.png)
![Traza lenta con 50 usuarios](img/observabilidad/traza-lenta-50c.png)

| Etapa de la traza | `5c` (112 ms) | `50c` (1.659 ms) |
| --- | --- | --- |
| `count` (span `aggregate activities`) | 105,6 ms (94 %) | 1.532 ms (92 %) |
| `find activities` | 0,5 ms | 41 ms |
| 12 o más `find users` (participantes de cada actividad) | ~0,2 ms cada uno | 0,2 a 20 ms cada uno |
| Resto (HTTP, mapeo y respuesta) | ~3 ms | ~10 ms |

La traza del recorrido completo, desde el navegador, y la de una ejecución fallida con MongoDB
detenido:

![Recorrido completo: navegador, Next.js, backend y MongoDB](img/observabilidad/traza-recorrido-completo.png)
![Ejecución fallida: MongoDB detenido](img/observabilidad/traza-error-mongo.png)

### Análisis (antes de la mejora)

**Qué cambia con más concurrencia.**
- Diez veces más usuarios **no procesan más solicitudes**: el throughput queda en unas 79 por
  segundo.
- Lo que crece es el tiempo de respuesta: el p50 se multiplica por 9 y el p95 por 12.
- No hay errores.
- Es la forma típica de una **saturación**: un recurso ya trabaja al máximo y el resto de los
  pedidos espera su turno. Cierra con la ley de Little: 50 usuarios / 81 req/s = 0,62 s, igual al
  tiempo medio medido por el backend (0,61 s).

**Qué etapa explica las operaciones lentas: el `count` de la búsqueda.**
- Su p95 sigue casi exactamente al p95 HTTP (2,12 s contra 2,13 s).
- En promedio es el 73 % del tiempo de cada solicitud con 50 usuarios.
- En las trazas lentas ocupa más del 90 %.
- El `find` paginado también empeora (de 5 a 107 ms en promedio), pero pesa mucho menos.

**Por qué el `count`.** `ActivityRepository.page()` calcula en cada página el total exacto de
coincidencias, y para eso MongoDB tiene que recorrer **todos** los documentos que coinciden, no
solo los 12 de la página. Con `explain` sobre los 50.000 documentos:

| Búsqueda | Plan | Documentos examinados |
| --- | --- | --- |
| Sin filtros | COLLSCAN | 50.000 |
| Por texto (regex que no distingue mayúsculas) | COLLSCAN: no puede usar índices | 50.000 |
| Ciudad + disponibilidad (`$expr` sobre el tamaño de `participants`) | IXSCAN por `status` + filtro | 27.047 |
| Tipo + estado | IXSCAN por `status` | 18.723 |

A unas 80 búsquedas por segundo son más de dos millones de documentos leídos por segundo. La CPU de
MongoDB pasa de 4,5 a 8,7 núcleos (de los 12 de la VM), mientras el backend se mantiene en
alrededor de 1. El cuello de botella está en la base, no en la aplicación.

**Qué aportan las trazas que las métricas no muestran.**
- Las métricas dicen *cuánto* y *cuándo*: el p95 sube, el `count` sube, el throughput no.
- Las trazas dicen *dónde dentro de cada solicitud*: el `count` corre antes que el `find`, y dentro
  de la búsqueda no hay otra etapa (la aplicación, el proxy de Next) que agregue tiempo.
- Además muestran algo que el histograma no mide: un **patrón N+1**. Después del `find`, cada
  actividad de la página dispara un `find users` para resolver sus participantes
  (`@DocumentReference(lazy = true)` en `Activity`). Con poca carga cuesta menos de 1 ms y bajo
  carga llega a 20 ms por consulta. Hoy no es la causa principal, pero crece con el tamaño de la
  página y con la carga de la base.

**Qué sostiene el diagnóstico y qué no se puede concluir.**
- Lo sostienen cuatro evidencias independientes que coinciden:
  - el throughput constante con latencia creciente;
  - el p95 del `count` igual al HTTP;
  - las trazas con más del 90 % en el `count`;
  - la CPU de Mongo creciendo mientras la del backend no.
- La degradación es **real y no inducida**: la demora controlada estuvo apagada.
- **Lo que no se puede concluir:**
  - Los números absolutos dependen de esta máquina: la VM comparte los 12 núcleos con el host y con
    Vegeta, y no hay límites de CPU por contenedor. En otra máquina el punto de saturación va a ser
    otro.
  - No se midió con datos reales: los clones tienen pocos valores distintos de ciudad y de título,
    y eso afecta la selectividad de los filtros.
  - El p95 del dashboard se estima con los buckets del histograma (±1 bucket) y con ventanas de
    rate, así que suaviza las transiciones entre etapas.
  - No se instrumentó el servidor de MongoDB por dentro (no lo pide el enunciado): se sabe que el
    `count` tarda, pero el reparto entre CPU, caché y locks solo se infiere de `docker stats` y de
    `explain`.

### Mejora implementada y verificación

Se aplicaron las tres correcciones que surgieron del análisis, sin cambiar el contrato de la API: la
respuesta sigue trayendo `totalElements` y `totalPages`, así que el frontend no cambió.

1. **Dejar de contar el total en cada página** (`ActivityRepository.searchPage`):
   - **Página corta:** si la página vino incompleta, el total sale de la propia página, sin
     contar. Es la lógica de `PageableExecutionUtils` de Spring.
   - **Caché del total:** si hace falta contar, se reutiliza el total de la misma combinación de
     filtros calculado en los últimos 30 s (`activity.search.total-cache-ttl`). Cualquier escritura
     sobre actividades (crear, unirse, salir) la vacía.
   - **Costo:** con varias instancias del backend, otra instancia puede mostrar un total de hasta
     30 s de antigüedad. Para paginar un listado es aceptable.
   - **En la traza:** el span `activities.search` registra con `search.total_source` (`count` o
     `cache`) de dónde salió el total.
2. **Corregir el N+1:**
   - la búsqueda lee los documentos crudos y junta los ids de participantes y organizadores de
     toda la página;
   - los trae con **un solo** `find` con `$in`, que en el histograma aparece como la operación
     `find_people`;
   - después arma las actividades con esos usuarios, en lugar de resolver cada referencia lazy por
     separado.
3. **Índices** `status_date_time` y `type_status_date_time`:
   - las búsquedas por estado, o por tipo y estado, leen la página en orden directamente del
     índice, sin una etapa `SORT` (`MongoIndexTest` lo verifica);
   - los filtros por texto y ciudad (subcadena que no distingue mayúsculas) y el de disponibilidad
     (`$expr`) **no se pueden indexar** así como están.

Se repitió exactamente el mismo experimento: mismos 50.000 documentos, mismo
`DURATION=3m ./loadtest/run.sh search 5c 50c` y misma máquina. Intervalos: 03:15:15 a 03:18:16 y
03:19:05 a 03:22:05 UTC del 10/11/2026.

![Dashboard después de la mejora](img/observabilidad/dashboard-carga-mejora.png)

| Métrica | `5c` antes | `5c` después | `50c` antes | `50c` después |
| --- | --- | --- | --- | --- |
| Solicitudes procesadas por segundo | 78,7 | **421** | 79,5 | **576** |
| Tiempo de respuesta p50 / p95 / p99 | 54 / 143 / 192 ms | **9 / 29 / 42 ms** | 472 / 1.724 / 2.020 ms | **62 / 230 / 347 ms** |
| Errores | 0 % | 0 % | 0 % | 0 % |
| `count` ejecutados por segundo | ~79 | **0,7** | ~80 | **4,1** |
| Consultas a `users` por búsqueda | 12 o más | **1** | 12 o más | **1** |
| CPU de MongoDB / backend | 455 % / 101 % | 328 % / 180 % | 866 % / 114 % | 603 % / 181 % |

**Se confirman las cinco predicciones:**
- el `count` casi desaparece;
- Mongo usa menos CPU aunque atiende 7 veces más pedidos;
- con 50 usuarios el throughput sube de 79 a 576 por segundo;
- el p95 baja de 1,7 s a 0,23 s, y la etapa pasa los umbrales del proyecto (p95 < 500 ms);
- el `find` pasa a ser la etapa dominante: explica el 79 % del tiempo medio con 50 usuarios
  (61 de 77 ms) y su p95 (229 ms) coincide con el p95 HTTP (239 ms).

![Traza lenta con 50 usuarios, después de la mejora](img/observabilidad/traza-lenta-50c-mejora.png)

Una de las trazas lentas de la nueva corrida (215 ms) tiene `search.total_source=cache`: ya no hay
`count`, el `find` paginado ocupa casi todo el tiempo y los participantes se resuelven con un solo
`find users` de 1,3 ms.

**Lo que queda.**
- **Nuevo límite:** sigue siendo MongoDB (6 núcleos con 50 usuarios), ahora en el `find` de las
  búsquedas por texto, ciudad y disponibilidad, que no pueden usar índices.
- **Siguiente paso:** la misma metodología lo guiaría. Por ejemplo, un índice de texto o una
  búsqueda por prefijo con collation, o un campo `full` actualizado al unirse o salir, en lugar
  del `$expr`.
- **Mejora menor:** cuando vence la caché, varios pedidos simultáneos con el mismo filtro pueden
  contar a la vez. Por eso hubo 4,1 `count` por segundo con 50 usuarios, cuando con unos 50
  filtros distintos y 30 s de caché se esperarían menos de 2. Calcular el total con
  `Cache.get(clave, función)` haría que esperen al primero.

### Demora controlada

No hizo falta para observar la degradación, pero está disponible
(ver [Demora controlada](#demora-controlada)). Con `OBSERVABILITY_SEARCH_DELAY=200ms` cada búsqueda
suma un span `activities.search.simulated_delay` de 200 ms. Esa degradación se distingue de una
saturación real:
- en la traza, el tiempo aparece en ese span y no en MongoDB;
- en el dashboard, el p95 de la base no cambia;
- en la CPU, Mongo no sube.

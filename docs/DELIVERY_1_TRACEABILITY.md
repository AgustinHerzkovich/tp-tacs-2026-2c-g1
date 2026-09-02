# Entrega 1 - Trazabilidad de requisitos

## Alcance exigido

El enunciado pide para la Entrega 1 el esqueleto web, modelo de actividades, participantes y reglas
climaticas en memoria, rutas REST, documentacion de API, ejecucion en Docker y documentacion del uso
de IA. La base NoSQL y la UI corresponden a la Entrega 2; cloud corresponde a la Entrega 3.

## User stories

| US | Capacidad | Estado | Implementacion principal | Verificacion automatizada |
| --- | --- | --- | --- | --- |
| 1 | Crear actividad | Cumplida | `POST /activities`, `ActivityService` | `ActivityServiceTest`, `ActivityControllerTest` |
| 2 | Condiciones climaticas | Cumplida | `WeatherCondition`, validacion del request | `WeatherConditionTest`, `CreateActivityRequestValidationTest` |
| 3 | Ventana de anticipacion | Cumplida | `Activity.isTimeToCheckWeatherConditions` | `ActivityIsTimeToCheckWeatherConditionsTest` |
| 4 | Rango de reprogramacion | Cumplida | `ReprogramationRange` | `ReprogramationRangeTest` |
| 5 | Buscar con filtros | Cumplida en memoria | `GET /activities`, `ActivityService.search` | `ActivitySearchControllerTest`, `ActivityServiceTest` |
| 6 | Sumarse o bajarse | Cumplida | `/activities/{id}/participants/me` | `ActivityParticipantTest`, `ActivityParticipationControllerTest` |
| 7 | Clima actual y pronostico | Cumplida | `GET /activities/{id}/weather` | `OpenMeteoWeatherAdapterTest`, `ActivityServiceTest` |
| 8 | Aviso por mal clima | Cumplida | `ActivityAnticipationCheckScheduler` | `ActivityAnticipationCheckSchedulerTest` |
| 9 | Votacion automatica o manual | Cumplida | scheduler y `PUT /votations/{id}/options` | `ActivityAnticipationCheckSchedulerTest`, `VotationServiceTest` |
| 10 | Votar y ver parcial | Cumplida | `PUT /votations/{id}/votes/me`, `GET /votations` | `VotationServiceTest`, `VotationControllerTest` |
| 11 | Resolver votacion | Cumplida | `VotationClosingScheduler` | `VotationClosingSchedulerTest` |
| 12 | Ver actividades y votaciones propias | Cumplida | `/activities/organizers/me`, `/activities/participants/me`, `/votations` | tests de controllers y servicios |
| 13 | Notificaciones de inicio, reprogramacion y cancelacion | Cumplida | schedulers, listener y `/notifications` | tests de notificaciones y transiciones |
| 14 | Estadisticas administrativas | Cumplida en memoria | `GET /statistics`, eventos estadisticos | `StatisticsServiceTest` |

## Requisitos no funcionales

- Maven Wrapper y Java 21 disponibles.
- Dockerfile y `docker-compose.yaml` permiten iniciar la aplicacion en un container.
- OpenAPI y Swagger UI documentan las rutas REST.
- Los casos de uso centrales tienen tests sin depender de Open-Meteo real.
- Open-Meteo usa cache, timeout, retry, circuit breaker y manejo de indisponibilidad.
- JWT permite distinguir usuarios; estadisticas requiere rol administrador.
- `mvnw verify` ejecuta tests, Spotless, Checkstyle y SpotBugs.
- README documenta ejecucion, arquitectura, decisiones y uso de IA.

## Limites conocidos de la Entrega 1

- Los repositorios son volatiles; reiniciar elimina actividades, votaciones, notificaciones y
  eventos estadisticos.
- El filtrado de actividades se ejecuta en memoria. Se migrara a queries del repositorio al agregar
  MongoDB en la Entrega 2.
- Firebase Auth no esta integrado. Se valida JWT HMAC local y existe un usuario de desarrollo para
  rutas publicas.
- No existe UI ni load test versionado todavia; ambos deben planificarse para las siguientes
  entregas conforme al enunciado general.
- Los schedulers no se disparan desde la API. Sus flujos temporales y climaticos se verifican con
  tests automatizados; en una prueba manual requieren configurar fechas e intervalos apropiados.

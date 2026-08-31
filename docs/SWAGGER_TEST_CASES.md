# Casos de prueba manuales en Swagger

## Preparacion

1. Iniciar con `docker compose up --build` o `./mvnw spring-boot:run`.
2. Abrir `http://localhost:8080/swagger-ui.html`.
3. Para pruebas deterministicas sin red, iniciar con `WEATHER_PROVIDER=in-memory`.
4. Ejecutar los casos en orden porque los repositorios son volatiles.
5. Reemplazar `{activityId}`, `{votationId}` y `{notificationId}` con IDs obtenidos previamente.

Las rutas generales usan `development-user` si no se envia JWT. El boton **Authorize** acepta un
Bearer JWT; es obligatorio para estadisticas con autoridad `ROLE_ADMIN`.

## US1 a US4 - Crear actividad y reglas

**Request:** `POST /activities`

```json
{
  "title": "Partido en la plaza",
  "description": "Partido amistoso",
  "type": "OUTDOOR",
  "location": {"city": "Buenos Aires", "latitude": null, "longitude": null},
  "dateTime": "2026-09-10T18:00:00",
  "minParticipants": 2,
  "maxParticipants": 10,
  "weatherConditions": {
    "maxRainProbability": 30,
    "minTemperature": 10,
    "maxTemperature": 30,
    "maxWindSpeed": 25.0
  },
  "anticipationWindow": 24,
  "reprogramationRange": {
    "maxDays": 3,
    "initialHour": "10:00:00",
    "finalHour": "20:00:00"
  }
}
```

**Esperado:** `201`, header `Location` y estado `CONFIRMED`. Guardar el `id`.

**Caso negativo:** repetir con `minParticipants: 11` y `maxParticipants: 10`.

**Esperado:** `400 ProblemDetail`.

## US5 - Buscar actividades

**Requests:**

- `GET /activities?type=OUTDOOR&city=Buenos Aires&availability=true`
- `GET /activities?dateFrom=2026-09-01T00:00:00&dateTo=2026-09-30T23:59:59`

**Esperado:** `200` y la actividad creada.

**Caso negativo:** enviar `dateFrom` posterior a `dateTo`.

**Esperado:** `400 ProblemDetail`.

## US6 - Participacion

1. Ejecutar `PUT /activities/{activityId}/participants/me`.
2. Consultar `GET /activities/{activityId}` y verificar `currentParticipants: 1`.
3. Ejecutar nuevamente el PUT y verificar que sea idempotente.
4. Ejecutar `DELETE /activities/{activityId}/participants/me` dos veces.

**Esperado:** respuestas `200` y conteos `1`, `1`, `0`, `0`.

## US7 - Clima de una actividad

1. Sumarse con `PUT /activities/{activityId}/participants/me`.
2. Ejecutar `GET /activities/{activityId}/weather`.

**Esperado:** `200` con clima actual y pronostico de la fecha de la actividad.

Para comprobar acceso, usar un JWT cuyo `sub` no pertenezca a la actividad.

**Esperado:** `403 ProblemDetail`.

## US8 y US9 - Mal clima y apertura de votacion

Este flujo depende de `ActivityAnticipationCheckScheduler`: la fecha debe estar dentro de
`anticipationWindow` y el proveedor debe devolver clima desfavorable. No existe un endpoint para
forzar el scheduler. La verificacion deterministica se realiza en
`ActivityAnticipationCheckSchedulerTest`.

Prueba manual integrada:

1. Crear una actividad cercana con condiciones estrictas.
2. Esperar la ejecucion horaria del scheduler.
3. Consultar `GET /votations` y `GET /notifications`.
4. Verificar votacion `ACTIVE` y notificacion de mal clima.

Si no hay alternativas favorables, se espera actividad `CANCELLED` y ninguna votacion vacia.

## US9 - Modificar alternativas manualmente

Con un `{votationId}` activo y autenticado como organizador, ejecutar
`PUT /votations/{votationId}/options`:

```json
{
  "dates": ["2026-09-11T18:00:00", "2026-09-12T18:00:00"]
}
```

**Esperado:** `200` si ambas fechas estan dentro del rango y tienen buen clima; `400` con
`invalidOptionDates` en caso contrario.

## US10 - Votar y ver resultado parcial

1. Ejecutar `PUT /votations/{votationId}/votes/me` con un body JSON string:

```json
"2026-09-11T18:00:00"
```

2. Ejecutar `GET /votations`.

**Esperado:** `200`; la opcion refleja el voto. Cambiar la fecha mueve el voto y repetirla es
idempotente.

## US11 - Cierre de votacion

Actualizar configuracion con `PUT /votations/{votationId}/settings`:

```json
{"minQuorum": 0.5, "duration": "PT5M"}
```

El cierre lo realiza `VotationClosingScheduler`. Alcanzado el quorum, la alternativa mas votada
reprograma la actividad; sin quorum o ganador, la cancela. La prueba deterministica esta en
`VotationClosingSchedulerTest`.

## US12 - Recursos propios

- `GET /activities/organizers/me`: contiene actividades creadas por el usuario.
- `GET /activities/participants/me`: contiene actividades a las que se sumo.
- `GET /votations`: contiene votaciones relacionadas y su estado.

**Esperado:** `200` en los tres casos.

## US13 - Notificaciones

1. Ejecutar `GET /notifications`.
2. Tomar un ID y ejecutar `PATCH /notifications/{notificationId}/read`.
3. Volver a listar y verificar `read: true`.

Las notificaciones de inicio, reprogramacion y cancelacion son generadas por schedulers y
transiciones; sus casos deterministas estan en los tests automatizados.

## US14 - Estadisticas administrativas

1. Autorizar Swagger con un JWT que tenga autoridad `ROLE_ADMIN`.
2. Ejecutar `GET /statistics`.
3. Ejecutar `GET /statistics?from=2026-08-01T00:00:00Z&to=2026-09-30T00:00:00Z`.

**Esperado:** `200` con actividades y requests meteorologicos registrados desde el ultimo inicio.

**Seguridad:** sin JWT se espera `401`; con JWT sin `ROLE_ADMIN`, `403`.

**Caso negativo:** usar `from` posterior a `to`.

**Esperado:** `400 ProblemDetail`.

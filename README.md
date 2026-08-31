# tp-tacs-2026-2c-g1

## Cómo levantar la aplicación

La aplicación queda disponible en `http://localhost:8080`, la documentación interactiva en
`http://localhost:8080/swagger-ui.html` y la especificación OpenAPI en
`http://localhost:8080/v3/api-docs`.

Ejemplo de request para generar una actividad mediante `POST /activities`:

```json
{
    "title": "Asado en la plaza",
    "description": "Junta con amigos, llevar sillas",
    "type": "OUTDOOR",
    "location": {
      "city": "Buenos Aires",
      "latitude": null,
      "longitude": null
    },
    "dateTime": "2026-08-25T18:00:00",
    "minParticipants": 4,
    "maxParticipants": 15,
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

### Con Docker

Se requiere Docker con Docker Compose. Desde la raíz del proyecto, ejecutar:

```bash
docker compose up --build
```

Para detener la aplicación, ejecutar `docker compose down`.

### Con Maven

Se requiere Java 21. El proyecto incluye Maven Wrapper, por lo que no es necesario instalar
Maven. Desde la raíz del proyecto, ejecutar:

```bash
# Windows
./mvnw.cmd spring-boot:run

# Linux/macOS
./mvnw spring-boot:run
```

Actualmente los datos se almacenan en memoria y se pierden al reiniciar la aplicación.

## Alcance de la Entrega 1

Esta entrega implementa el modelo de actividades, participantes, reglas climáticas, votaciones,
notificaciones y estadísticas mediante repositorios en memoria. Las rutas REST están documentadas
con OpenAPI. La persistencia NoSQL y la interfaz de usuario corresponden a la Entrega 2, mientras
que el despliegue portable en cloud corresponde a la Entrega 3.

La matriz de trazabilidad entre user stories, implementación y pruebas está disponible en
[`docs/DELIVERY_1_TRACEABILITY.md`](docs/DELIVERY_1_TRACEABILITY.md). Los casos manuales para
Swagger están en [`docs/SWAGGER_TEST_CASES.md`](docs/SWAGGER_TEST_CASES.md).

## API y autenticación

Swagger UI permite explorar y ejecutar las rutas REST. Las rutas generales permanecen públicas en
esta etapa y, cuando no se presenta identidad, utilizan `development-user`. El endpoint
`/statistics` exige un JWT con autoridad `ROLE_ADMIN`.

El backend valida JWT HMAC-SHA256 mediante Spring Security. La clave local predeterminada sirve
solo para desarrollo y puede reemplazarse con `SECURITY_JWT_SECRET`. Firebase Auth no está
integrado actualmente.

## Decisiones de diseño

Estas decisiones cubren aspectos no definidos de forma exhaustiva por el enunciado:

- **Repositorios intercambiables y almacenamiento en memoria:** los servicios dependen de
  interfaces de repositorio. La Entrega 1 usa implementaciones con `ConcurrentHashMap`; en la
  Entrega 2 podrán reemplazarse por MongoDB sin cambiar los casos de uso.
- **Backend sin sesión HTTP:** la identidad se obtiene del `subject` de un JWT y no se mantiene
  estado de sesión en el backend. En desarrollo se admite `development-user` para facilitar las
  pruebas de esta entrega.
- **Monitoreo periódico configurable:** Spring Scheduler evalúa clima, cierre de votaciones,
  finalización y avisos de inicio con periodicidades configurables.
- **Organizador como participante:** el organizador puede sumarse y bajarse como participante. Para
  el quórum, solo se cuenta una vez aunque también figure entre los participantes.
- **Votación sin opciones favorables:** si no se encuentran alternativas dentro del rango con clima
  aceptable, no se abre una votación vacía y la actividad se cancela.
- **Quórum global:** el quórum mínimo se aplica a la participación total de la votación. Alcanzado el
  quórum, gana la alternativa más votada; si no hay ganadora o no se alcanza el quórum, la actividad
  se cancela.
- **Opciones manuales validadas:** el organizador puede reemplazar las alternativas mientras la
  votación está activa, pero todas deben pertenecer al rango permitido y tener clima aceptable.
- **Consumo responsable del clima:** Open-Meteo se encapsula detrás de `IWeatherAdapter`; se usan
  cachés acotadas, timeout, retry y circuit breaker. La indisponibilidad no se interpreta como clima
  favorable.
- **Estadísticas mediante eventos:** las métricas históricas se registran como eventos inmutables en
  memoria. Se cuentan las llamadas HTTP reales a Open-Meteo, no los accesos resueltos por caché. El
  diseño permite migrar la colección de eventos a MongoDB.
- **Rangos estadísticos inclusivos:** `from` y `to` incluyen ambos extremos; sin parámetros se
  consultan los últimos siete días. Una cancelación climática incluye mal clima y ausencia de
  alternativas favorables.

No se adoptaron las decisiones antiguas de CDC/Stream ETL ni de filtros ejecutados por una base de
datos porque no existe persistencia en la Entrega 1. Tampoco se documenta Firebase Auth como una
decisión vigente: la implementación actual usa JWT HMAC local. Esas alternativas deberán evaluarse
nuevamente cuando exista una necesidad concreta de volumen, persistencia o proveedor de identidad.

## Servicio meteorológico

La aplicación usa [Open-Meteo](https://open-meteo.com/) para clima actual y pronóstico horario.
Consulta temperatura en °C, probabilidad de precipitación en porcentaje y viento en km/h. Cuando
una ubicación no tiene coordenadas, usa la primera coincidencia de Open-Meteo Geocoding.

Open-Meteo se selecciona por defecto y no requiere API key para el uso no comercial de este TP. Las
coordenadas, condiciones actuales y pronósticos se almacenan en cachés acotadas. Las llamadas tienen
timeouts, retry corto y circuit breaker. Si el proveedor no responde y no existe una entrada vigente
en caché, los endpoints meteorológicos responden `503` y los procesos automáticos se reintentan en la
próxima ejecución; nunca se interpreta la falta de datos como buen clima.

Los TTL y límites de entradas pueden ajustarse con las propiedades
`weather.cache.<geocoding|current|forecast>.ttl` y
`weather.cache.<geocoding|current|forecast>.maximum-size`. Los valores predeterminados son 24 horas
y 1000 entradas para geocodificación, 15 minutos y 5000 entradas para condiciones actuales, y una
hora y 10000 entradas para pronósticos.

Para desarrollo sin red puede habilitarse el adapter determinístico en memoria:

```bash
WEATHER_PROVIDER=in-memory ./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
$env:WEATHER_PROVIDER="in-memory"
.\mvnw.cmd spring-boot:run
```

Los datos meteorológicos provienen de Open-Meteo y están sujetos a su licencia
[CC BY 4.0](https://open-meteo.com/en/licence). Los pronósticos son estimaciones y no deben usarse
como única fuente para decisiones de seguridad.

## Calidad de código

El proyecto incluye Maven Wrapper. Para aplicar el formato, comprobarlo y ejecutar la verificación
completa:

```bash
# Windows
./mvnw.cmd spotless:apply
./mvnw.cmd spotless:check
./mvnw.cmd verify

# Linux/macOS
./mvnw spotless:apply
./mvnw spotless:check
./mvnw verify
```

`verify` ejecuta los tests y las validaciones de Spotless, Checkstyle y SpotBugs.

### Pre-commit

El repositorio incluye un hook que localiza un JDK 21 instalado y ejecuta `clean verify` antes de
cada commit. Para activarlo una sola vez por clonación:

```bash
git config core.hooksPath .githooks
```

El error `class file version 65.0 ... up to 61.0` indica que el código fue compilado con Java 21,
pero se intentó ejecutar con Java 17. El hook evita esa mezcla configurando Java 21 antes de Maven.

## Git flow

![Diagrama de Git flow](src/main/resources/static/gitflow.png)

## Uso de inteligencia artificial

Durante el desarrollo utilizamos asistentes de IA generativa, principalmente ChatGPT y Claude,
como herramientas de apoyo. Su uso se concentró en las siguientes tareas:

- Generación y adaptación de código repetitivo o *boilerplate*.
- Propuesta de casos de prueba y revisión de la cobertura de tests.
- Revisión de las user stories para detectar requisitos, casos límite o validaciones que pudieran
  haberse omitido.
- Consulta de alternativas y opiniones para decisiones de diseño e implementación.
- Apoyo en la redacción y revisión de documentación técnica, incluyendo la creación y
  homogeneización de la documentación Javadoc del código.
- Apoyo en la aplicación y verificación del formato y de las herramientas de calidad del proyecto,
  como Spotless, Checkstyle y SpotBugs.
- Análisis de errores de compilación, tests y conflictos de integración.

Las respuestas de estas herramientas se tomaron como sugerencias y no como resultados definitivos.
El equipo revisó las propuestas, las adaptó al diseño y las convenciones del proyecto, y validó los
cambios mediante revisión del código, ejecución de tests y el proceso de verificación de Maven.

# tp-tacs-2026-2c-g1

## Cómo levantar la aplicación

La aplicación queda disponible en `http://localhost:8080` y su API de actividades en
`http://localhost:8080/activities`.

Ejemplo de request para generar actividad
POST http://localhost:8080/activities
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

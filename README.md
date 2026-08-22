# tp-tacs-2026-2c-g1

## Cómo levantar la aplicación

La aplicación queda disponible en `http://localhost:8080` y su API de actividades en
`http://localhost:8080/activities`.

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

## Git flow

![Diagrama de Git flow](src/main/resources/static/gitflow.png)

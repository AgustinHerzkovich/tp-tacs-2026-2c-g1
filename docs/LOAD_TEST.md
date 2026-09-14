# Prueba de carga

Este documento define un escenario reproducible para comprobar que la aplicación soporta carga sin
usar Open-Meteo ni Nominatim como objetivos involuntarios. No presenta resultados históricos: cada
entrega debe ejecutar el escenario en un ambiente limpio y registrar hardware, commit, fecha,
parámetros y salida de la herramienta para que el resultado sea comparable.

## Alcance y precauciones

- Ejecutar únicamente contra una instancia local o un ambiente propio autorizado.
- Usar `WEATHER_PROVIDER=in-memory` para evitar requests al proveedor meteorológico público.
- El endpoint elegido, `GET /healthcheck`, mide disponibilidad y capacidad HTTP básica sin requerir
  credenciales ni contaminar datos. Los endpoints de negocio requieren obtener previamente un JWT de
  Keycloak y deben probarse como un escenario separado.
- Empezar con baja concurrencia y aumentarla gradualmente. No ejecutar este procedimiento contra el
  despliegue compartido sin coordinarlo con el equipo.

## Preparación

Desde la raíz del repositorio:

```bash
WEATHER_PROVIDER=in-memory docker compose up --build --wait
docker compose ps
```

En PowerShell:

```powershell
$env:WEATHER_PROVIDER="in-memory"
docker compose up --build --wait
docker compose ps
```

Comprobar antes de medir:

```bash
curl --fail http://localhost:8080/healthcheck
```

## Escenario con Vegeta

Con [Vegeta](https://github.com/tsenart/vegeta) instalado, crear un archivo local `targets.txt` con
esta línea:

```text
GET http://localhost:8080/healthcheck
```

Enviar 50 requests por segundo durante 60 segundos y generar un reporte:

```bash
vegeta attack -targets=targets.txt -rate=50/s -duration=60s | vegeta report
```

Repetir con `100/s` y `200/s` solo si la etapa anterior mantiene el servicio saludable. Para guardar
e inspeccionar la distribución de latencias:

```bash
vegeta attack -targets=targets.txt -rate=100/s -duration=60s -output=results.bin
vegeta report results.bin
vegeta report -type=hist results.bin
```

`targets.txt` y `results.bin` son artefactos locales y no deben versionarse salvo que se acuerde
explícitamente como evidencia de una entrega.

## Criterios y registro

Como criterio inicial para el escenario local de salud se espera disponibilidad de al menos 99%, sin
respuestas `5xx`, y latencia p95 inferior a 500 ms. Estos umbrales son una línea base del equipo, no
una garantía productiva; deben revisarse con infraestructura y objetivos reales del despliegue.

Registrar junto con cada ejecución:

- Commit y fecha.
- CPU, memoria y sistema operativo del host, además de recursos asignados a Docker.
- Tasa, duración, endpoint y herramienta/version.
- Requests totales, throughput real, disponibilidad, códigos HTTP y latencias p50, p95 y p99.
- Uso máximo de CPU/memoria y errores de `docker compose logs backend frontend`.

Después de la prueba verificar que `curl --fail http://localhost:8080/healthcheck` continúa
respondiendo y detener el stack con `docker compose down`.

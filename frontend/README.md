# Frontend

Cliente web de `tp-tacs-2026-2c-g1` (Next.js + React + Tailwind CSS + shadcn/ui). Ver el
[README de la raíz](../README.md) para la estructura general del repositorio y
[AGENTS.md](../AGENTS.md) para las convenciones del proyecto (estructura page/component/hook,
integración con el backend, etc.).

## Cómo levantar la aplicación

Se requiere Node.js 22. Desde la carpeta `frontend/`:

```bash
npm ci
npm run dev
```

La app queda disponible en `http://localhost:3000`.

## Variables de entorno

Copiá `.env.example` a `.env.local` y ajustá si hace falta:

```bash
cp .env.example .env.local
```

- `BACKEND_URL`: origen del backend Spring Boot (por defecto `http://localhost:8080`). Solo lo usan,
  del lado del servidor, las rutas proxy en `src/pages/api/**` — nunca se llama al backend
  directamente desde el cliente.
- `NEXT_PUBLIC_KEYCLOAK_URL`: origen público de Keycloak (por defecto `http://localhost:8090`).
- `NEXT_PUBLIC_KEYCLOAK_REALM`: realm OIDC (`solnotfound`).
- `NEXT_PUBLIC_KEYCLOAK_CLIENT_ID`: cliente público SPA (`solnotfoundFrontend`); no lleva secreto.

El frontend usa Authorization Code con PKCE. Keycloak solo se contacta cuando el usuario presiona
el botón de ingreso o cuando se comprueba una sesión SSO existente después de recargar. Los tokens
permanecen en memoria dentro de `keycloak-js` y Redux solo conserva `sub`, nombre y roles. Las
llamadas autenticadas deben usar `authFetch`, que renueva el access token y lo envía al proxy como
Bearer.

Para construir, levantar el sistema completo y esperar automáticamente a que esté disponible,
ejecutá `docker compose up --build --wait` desde la raíz. En ese modo el proxy del frontend usa
`http://backend:8080` dentro de la red de Compose. Compose considera saludable al frontend cuando
`/api/healthcheck` responde correctamente a través de Next.js y del backend.

## Calidad de código

```bash
npm test
npm run lint
npm run typecheck
npm run build
```

Los tests unitarios y de componentes usan Vitest y Testing Library. Para los E2E con Playwright,
levantar el stack con seed desde la raíz y luego ejecutar desde `frontend/`:

```bash
# Raíz del repositorio
APP_SEED_ENABLED=true docker compose up --build --wait

# frontend/
npx playwright install chromium
npm run test:e2e
```

Las cuentas locales predeterminadas son `alumno/alumno` y `admin/admin`. Las variables
`E2E_USER_USERNAME`, `E2E_USER_PASSWORD`, `E2E_ADMIN_USERNAME`, `E2E_ADMIN_PASSWORD` y
`E2E_BASE_URL` permiten reemplazarlas. Ver el [README raíz](../README.md#calidad-de-código) para el
procedimiento completo y las consideraciones de seguridad.

## Estado actual

Explorar, Mis Actividades, detalle, clima, votaciones, participantes, notificaciones, creación y
estadísticas consumen la API real mediante los proxies de `src/pages/api`. La ruta
`/estadisticas` y su acceso en el header sólo se muestran a usuarios con rol de realm `ADMIN`; el
backend vuelve a validar el mismo rol.

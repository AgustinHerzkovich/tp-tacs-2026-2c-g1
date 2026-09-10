# Frontend

Cliente web de `tp-tacs-2026-2c-g1` (Next.js + React + Tailwind CSS + shadcn/ui). Ver el
[README de la raíz](../README.md) para la estructura general del repositorio y
[AGENTS.md](AGENTS.md) para las convenciones del proyecto (estructura page/component/hook,
integración con el backend, etc.).

## Cómo levantar la aplicación

Se requiere Node.js. Desde la carpeta `frontend/`:

```bash
npm install
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
el botón de ingreso; los tokens permanecen en memoria dentro de `keycloak-js` y Redux solo conserva
`sub`, nombre y roles. Las llamadas autenticadas deben usar `authFetch`, que renueva el access token
y lo envía al proxy como Bearer.

Para probar contra el backend real, levantalo por separado (ver [backend/README.md](../backend/README.md)).

## Calidad de código

```bash
npm run lint
npm run build
```

## Estado actual

Las pantallas todavía usan datos mock de `src/data/mockData.ts`, pero la autenticación ya está
delegada a Keycloak y la capa proxy de `src/pages/api` reenvía sus JWT al backend. Conectar los datos
reales requiere mapear `ActivityResponse` al modelo de UI y usar `authFetch` en los hooks.

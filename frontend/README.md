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

Para probar contra el backend real, levantalo por separado (ver [backend/README.md](../backend/README.md)).

## Calidad de código

```bash
npm run lint
npm run build
```

## Estado actual

Las pantallas (Explorar, Mis Actividades, detalle de actividad con votación, wizard de creación y
notificaciones) están implementadas con datos mock en `src/data/mockData.js`. La capa de proxy hacia
el backend (`src/pages/api`) ya existe y refleja las rutas reales del backend; conectar las
pantallas a datos reales requiere mapear la forma del `ActivityResponse` del backend al modelo que
usa hoy la UI, más el flujo de autenticación (JWT) que todavía no está implementado en el frontend.

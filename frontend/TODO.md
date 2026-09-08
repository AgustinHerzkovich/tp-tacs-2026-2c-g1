# Frontend — pendientes

Vamos tomando estos ítems de a uno. Decisiones ya tomadas quedan anotadas junto al ítem.

## Hecho

- [x] **Migrar todo el proyecto a TypeScript**, con tipado estricto (`strict: true` +
      `noUncheckedIndexedAccess`) y tipos propios para los DTOs del backend en
      `src/types/backend.ts` (`ActivityResponse`, `VotationDTO`, `NotificationResponse`, etc.) y
      para el modelo de la UI mock en `src/types/domain.ts`. shadcn/ui regenerado en `.tsx` nativo.
      `npx tsc --noEmit`, `npm run lint` y `npm run build` pasan limpios.
- [x] **Layout "mobile-only" en cualquier viewport**: columna centrada de `max-w-[430px]` en
      `src/app/layout.tsx`, sin marco de teléfono decorativo.
- [x] **Redux Toolkit para el estado del cliente logueado**: store en `src/store/` (`session` slice
      con el usuario actual), `StoreProvider` client-side por sesión (patrón recomendado para App
      Router).
- [x] **Login mockeado**: pantalla `/login` con un roster de usuarios mock (`src/data/mockUsers.ts`,
      shape `{id, name}` idéntico al `User`/`UserDTO` real del backend — que no genera ningún seed
      propio, así que el roster es inventado pero fiel a la forma). `useAuth` (login/logout) +
      `useRequireAuth` protegen Explorar, Mis Actividades, Detalle y el wizard — sin sesión, redirigen
      a `/login`. Tocar el avatar del header cierra sesión. Sin JWT real — eso sigue pendiente abajo.
- [x] **Persistencia de la sesión entre refrescos**: `src/store/session/sessionPersistence.ts`
      espeja login/logout en `localStorage` vía middleware de Redux; `StoreProvider` la restaura en un
      `useEffect` (client-only) al montar, marcando `session.hydrated` — `useRequireAuth` espera ese
      flag antes de decidir si redirige, para que alguien con sesión guardada no rebote a `/login`
      durante el primer render (evita el mismatch de hidratación de Next: server y cliente arrancan
      siempre iguales — deslogueado, no-hidratado — y recién después se corrige con lo que haya en
      `localStorage`).

## En curso ahora

_(nada activo — decime cuál sigue)_

## Datos reales (reemplazar mocks)

- [ ] Reemplazar `src/data/mockData.ts` por fetches reales a `/api/*` en `useActivities` /
      `useNotifications`, sin tocar los componentes que consumen esos hooks.
- [ ] Mapear el `ActivityResponse` real del backend (fecha, ubicación, clima, estado, participantes)
      al modelo que hoy usa la UI — o adaptar la UI al shape real, lo que tenga más sentido una vez
      que lo tengamos delante.
- [ ] Reemplazar el login mockeado por uno real contra el backend (JWT HMAC). Hoy el backend cae a
      `development-user` si no hay `Authentication`; ver cómo/cuándo metemos auth real y qué pasa con
      el roster mock (`src/data/mockUsers.ts`) una vez que exista.
- [ ] Wizard: publicar actividad real — `POST /api/activities` (multipart, con imágenes) en vez del
      estado local de `useWizardForm`.
- [ ] Votación real — `PUT /api/votations/:id/votes/me` en vez del estado local de `useVoting`.
- [ ] Sumarse/salir real — `PUT`/`DELETE /api/activities/:id/participants/me` en vez de
      `useJoinActivity`.
- [ ] Notificaciones reales + marcar como leída — `GET /api/notifications`,
      `PATCH /api/notifications/:id/read`.
- [ ] Buscador/filtros de Explorar contra `GET /api/activities` (`type`, `city`, `dateFrom`,
      `dateTo`, `availability`) en vez de filtrar el array mock en el cliente.

## UX / estados

- [ ] Estados de carga (skeletons) mientras llegan datos reales.
- [ ] Estados de error (fetch falla, 401/403, actividad inexistente).
- [ ] Estados vacíos (sin actividades, sin notificaciones, sin resultados de búsqueda).
- [ ] Validación de formulario en el wizard (título requerido, fecha futura, mín ≤ máx, etc.).
- [ ] Feedback tipo toast para acciones (votar, sumarse, publicar) en vez de solo cambiar el label
      del botón.

## Pulido

- [ ] Pasada de accesibilidad (foco visible, `aria-label`s, contraste de color).
- [ ] Favicon y metadata por página (`title`/`description` dinámicos por ruta).
- [ ] Definir si hace falta una pantalla de Perfil — el spec de Planazo no la pidió explícitamente,
      pero el avatar del header hace pensar que sí en algún momento.

## Calidad / infra

- [ ] Tests de componentes clave y hooks.
- [ ] Pipeline de CI para el frontend (lint + build en cada PR).
- [ ] Definir estrategia de deploy del frontend (Vercel / Docker junto al backend / otra).

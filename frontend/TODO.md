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
- [x] **Conexión real con el backend**: `src/data/mockData.ts` quedó reducido a las tablas de diseño
      (`STATUS_META`, `TYPE_META`, `SCENES`, etc.); Explorar, Mis Actividades, Detalle, Votación,
      Sumarse/Salir, Notificaciones y el wizard de creación pegan todos contra `/api/*`
      (`src/lib/api.ts`) en vez de datos mockeados. Verificado punta a punta contra el backend real en
      Docker + Mongo seedeado: login, feeds, detalle con clima real, voto real, join/leave real y
      publicación real de una actividad. Login sigue siendo mockeado (ver más abajo) y las cards de
      lista siguen siendo el modelo simplificado `ExploreActivity`/`MisActivity`
      (`src/lib/activityMapping.ts`), no el `ActivityResponse` completo — el detalle sí usa el DTO
      real (`useActivity`).
  - Bugs reales encontrados y arreglados en el camino: el organizador de una actividad no podía ver
    su propio clima (`ActivityService.verifyParticipant` sólo miraba `participants`, no
    `organizer` — backend); y las cards de Explorar/Mis Actividades mostraban avatares mock
    hardcodeados (`PEOPLE`) en vez de los participantes reales (`ExploreCard`/`MisCard`).

## En curso ahora

_(nada activo — decime cuál sigue)_

## Pendientes (relevados 2026-09-10)

Se dejaron *solo documentados*, sin implementar, para ir tomándolos de a uno.

### Errores y loading

- [ ] **Pantallas/pop-ups de error**: hoy no existe ningún estado de error dedicado. No hay
      `error.tsx`/`not-found.tsx` de Next en `src/app/**` (búsqueda confirmó que no existe ninguno), y
      cada hook (`useActivity`, `useActivities`, `useVoting`, etc.) expone un `error: string | null`
      que hoy en varios lados ni se renderiza, o se renderiza como texto plano suelto. Falta definir
      un componente/patrón único (¿modal, banner, toast?) para errores de fetch, 401/403 y "actividad
      inexistente" (`useActivity.notFound` ya existe pero no tiene una pantalla propia todavía).
- [ ] **Pantallas de loading**: no hay `loading.tsx` de Next ni skeletons — el estado `loading` de
      cada hook hoy se resuelve, en el mejor de los casos, con un texto tipo "Cargando…". Falta un
      componente de skeleton/spinner reusable para Explorar, Mis Actividades, Detalle y el wizard.

### UI

- [ ] Pasada general de ajuste de UI (a definir con más detalle qué específicamente — pendiente de
      que el usuario señale qué pantallas/elementos priorizar).

### Ubicación real (OpenStreetMap)

- [ ] Hoy el wizard sólo guarda `location.city` como texto libre; `latitude`/`longitude` van siempre
      en `null` (`CrearActividadPage.tsx`, función `buildCreateRequest`) — el campo "Buscar dirección
      o lugar…" no busca nada, es un input de texto suelto. Falta integrar un servicio de geocoding
      (Nominatim/OpenStreetMap) para que ese campo autocomplete direcciones reales y complete
      `latitude`/`longitude` en el request — hoy el backend igual funciona sin coordenadas (usa
      geocoding propio contra Open-Meteo si sólo hay ciudad), pero perdemos precisión y el mapa/pin de
      "Tocá para colocar el pin" del paso 2 no está conectado a nada real.

### Validación del wizard

- [ ] **Falta dar error al avanzar de paso**: `wizard.next` (botón "Continuar" de los pasos 1 a 3) no
      valida nada — hoy se puede avanzar con título vacío, sin fecha/hora, sin ubicación, etc. La
      única validación (`validate()` en `CrearActividadPage.tsx`) corre recién al hacer clic en
      "Publicar Actividad" en el paso 4, con un único mensaje genérico (no hay validación por campo).
      Falta: validar por paso antes de dejar avanzar, y mostrar el error asociado al campo puntual que
      falta (no sólo un banner al final).

### Participantes / unicidad

- [ ] **Revisar y verificar el flujo de "ya estoy unido a esta actividad"**: el backend ya es
      idempotente a nivel de entidad (`Activity.addParticipant` no agrega duplicados si el usuario ya
      está en `participants`, no tira error, sólo no hace nada — `Activity.java`), y el detalle de
      actividad (`ActivityDetailPage.tsx` + `useJoinActivity`) ya calcula `joined` mirando el
      `ActivityResponse.participants` real, así que el botón debería mostrar "¡Estás sumado! ✓" (que
      dispara `leave`, no `join`) en vez de dejar re-unirse. Falta confirmar que esto se sostiene en
      **todos** los puntos de entrada (Explorar → Detalle, Mis Actividades → Detalle, refresco de
      página estando ya unido, doble click rápido en "Sumarme") y no sólo en el flujo feliz ya probado
      una vez; si aparece algún caso real donde sí se puede re-unir o pedir join dos veces, documentar
      el caso puntual antes de tocar código.

## Verificación end-to-end

- [ ] **Verificar todo el funcionamiento punta a punta** de nuevo una vez que se vayan resolviendo los
      puntos de arriba (no sólo el happy path ya cubierto: login, feeds, detalle, voto, join/leave,
      creación — sino también los casos de error, timeouts del backend, actividades sin clima
      disponible, votaciones cerradas, actividades canceladas, etc.).
- [ ] **Casos de prueba de actividades y del flujo de creación**: no hay ningún test automatizado del
      lado frontend todavía (ver "Calidad / infra" más abajo) — falta al menos listar los casos que
      importan: creación válida, creación con campos faltantes por paso, fecha pasada, mín > máx,
      ciudad sin geocoding, publicación con y sin imágenes, y para actividades: ver/votar/unirse/salir
      en cada estado (`propuesta`, `confirmada`, `reprogramada`, `cancelada`, `finalizada`), con y sin
      cupo, siendo organizador vs. participante vs. ninguno de los dos.

## UX / estados

- [ ] Estados vacíos (sin actividades, sin notificaciones, sin resultados de búsqueda).
- [ ] Feedback tipo toast para acciones (votar, sumarse, publicar) en vez de solo cambiar el label
      del botón.

## Datos reales (pendiente)

- [ ] Reemplazar el login mockeado por uno real contra el backend (JWT HMAC). Hoy el backend cae a
      `development-user` si no hay `Authentication`; ver cómo/cuándo metemos auth real y qué pasa con
      el roster mock (`src/data/mockUsers.ts`) una vez que exista.
- [ ] Buscador/filtros de Explorar contra `GET /api/activities` (`type`, `city`, `dateFrom`,
      `dateTo`, `availability`) en vez de filtrar el array mock en el cliente.
- [ ] Marcar notificación como leída — sólo está implementado el listado (`GET /api/notifications`),
      falta conectar `PATCH /api/notifications/:id/read` a alguna acción de la UI.

## Pulido

- [ ] Pasada de accesibilidad (foco visible, `aria-label`s, contraste de color).
- [ ] Favicon y metadata por página (`title`/`description` dinámicos por ruta).
- [ ] Definir si hace falta una pantalla de Perfil — el spec de Planazo no la pidió explícitamente,
      pero el avatar del header hace pensar que sí en algún momento.

## Calidad / infra

- [ ] Tests de componentes clave y hooks (ver "casos de prueba" arriba para el listado de escenarios
      de actividades/creación).
- [ ] Pipeline de CI para el frontend (lint + build en cada PR).
- [ ] Definir estrategia de deploy del frontend (Vercel / Docker junto al backend / otra).

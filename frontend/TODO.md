# Frontend - pendientes

Backlog relevado contra las user stories y el estado real de la Entrega 2. Los items de esta lista
son trabajo pendiente; lo ya implementado se resume al final para evitar reabrir tareas obsoletas.

## Prioridad alta - funcionalidad faltante

- [x] **Completar los filtros de Explorar contra el backend.** El texto se conserva local porque el
      backend no expone filtro por título; tipo, ciudad, rango y disponibilidad se envían a la API.
      filtran el feed ya descargado en el cliente. Falta una UI para ciudad, rango de fechas y
      disponibilidad, y enviar `type`, `city`, `dateFrom`, `dateTo` y `availability` a
      `GET /api/activities`. Definir búsqueda por título: el backend no expone ese filtro, por lo que
      debe agregarse al contrato o documentarse que seguirá siendo local.
- [x] **Permitir al organizador administrar una votación activa.** La UI determina al organizador
      mediante `/activities/organizers/me` y permite reemplazar opciones, quórum y duración.
      `PUT /api/votations/:id/options` y `PUT /api/votations/:id/settings`, pero ninguna pantalla los
      utiliza. Falta distinguir organizador/participante (no agregar roles organizador y participante,
      se distinguen mediante el id del creador) y permitir reemplazar opciones manuales
      dentro del rango válido, además de editar quórum mínimo y duración, mostrando errores del
      backend cuando una fecha no tenga clima favorable o quede fuera del rango.
- [x] **Exponer la franja horaria de reprogramación en el wizard.** Se eligen y validan horas inicial
      y final, y se envían al backend.
      días máximos y `CrearActividadPage` envía siempre `09:00:00-21:00:00`. La US4 exige que el
      organizador configure también hora inicial y final; agregar controles y validación cruzada.
- [x] **Diferenciar correctamente las acciones del organizador en el detalle.** El organizador se
      identifica por su actividad y puede sumarse también como participante explícitamente.
      organizador puede sumarse/bajarse como participante desde la UI y mostrar acciones/textos
      específicos para evitar que se confunda "organizar" con "participar".
- [x] **Actualizar vistas inmediatamente después de mutaciones.** Voto y join/leave aplican/refrescan
      la respuesta; notificaciones comparten estado entre contador y drawer.
      bajarse o marcar una notificación como leída, revisar invalidación/refetch de Explorar, Mis
      Actividades, detalle y contador de notificaciones para que no requieran F5.

## Prioridad alta - robustez y estados

- [x] **Unificar errores de UI.** Se agregaron estados comunes de error con reintento y boundaries.
      red, validación, 401, 403, 404, proveedor meteorológico y almacenamiento de imágenes. Hoy se
      mezclan textos sueltos y errores sin acción de reintento.
- [x] **Agregar `error.tsx`, `not-found.tsx` y estados de carga consistentes.** Hay boundaries globales
      y estados comunes aplicados a los feeds principales y detalle.
      "Cargando..." por skeletons o indicadores accesibles en feeds, detalle, estadísticas y wizard;
      ofrecer reintento donde corresponda.
- [x] **Cubrir estados vacíos principales.** Explorar, actividades propias y notificaciones muestran
      estados dedicados; Mis Actividades conserva CTA de creación en el chrome.
      sin votaciones, sin notificaciones, sin resultados de búsqueda y estadísticas sin eventos.
- [x] **Manejar expiración y recuperación de sesión.** `check-sso` restaura sesión, se refresca el token
      periódicamente y se conserva la ruta solicitada al redirigir al login.
      recargar; falta verificar expiración durante uso prolongado, refresh fallido, logout desde otra
      pestaña y redirección al login conservando la ruta original.
- [x] **Evitar requests duplicados de notificaciones.** `HeaderLayout` mantiene una única instancia
      compartida por contador y drawer.
      `NotifDrawer` usa otra instancia de `useNotifications`; centralizar el estado o compartir un
      único hook/provider para no hacer dos GET ni desincronizar el contador.

## Mapas e imágenes

- [x] **Robustecer Nominatim/OpenStreetMap.** Agregar debounce, cancelación con `AbortController`,
      mensajes de sin resultados/error, timeout y rate limiting/cache server-side para respetar la
      política pública de Nominatim. Evaluar un servicio propio o proveedor contratado antes de
      producción.
- [x] **Mejorar selección de ubicación.** Permitir arrastrar el marcador, usar ubicación actual con
      permiso explícito, mostrar coordenadas/dirección seleccionada y validar límites de latitud y
      longitud. Verificar interacción táctil, teclado y lectores de pantalla.
- [x] **Completar validación de imágenes antes de publicar.** Mostrar errores para archivos
      rechazados (tipo, tamaño, más de cinco) en vez de ignorarlos; detectar duplicados, permitir
      reordenar para elegir portada y liberar todos los object URLs al descartar/publicar/salir.
- [x] **Estados de carga y error por imagen.** Mostrar placeholder si una URL presignada venció o no
      carga, y verificar renovación/refetch de URLs en feeds y galería.

## Responsive y UX

- [x] **Pasada visual sistemática mobile/desktop.** Probar 320, 375, 430, 768, 1024 y 1440 px en
      login, feeds, detalle, galería, mapa, wizard de cinco pasos, drawer y estadísticas. Revisar en
      especial que el chrome fijo no tape botones/contenido y que cards/textos no desborden.
- [x] **Revisar el detalle desktop.** Validar la grilla de clima/votación/descripción con y sin
      votación, descripción, participantes e imágenes; evitar espacios artificiales y mantener la
      acción principal visible.
- [x] **Feedback de acciones.** Agregar confirmación tipo toast para crear, votar, sumarse, bajarse y
      marcar como leída; deshabilitar doble click y comunicar claramente operaciones pendientes.
- [x] **Accesibilidad completa.** Revisar foco visible, orden de tabulación, etiquetas, mensajes con
      `aria-live`, contraste, navegación de galería/mapa por teclado, tamaños táctiles y reduced
      motion.
- [x] **Perfil/logout más claro.** El avatar actualmente cierra sesión inmediatamente; reemplazarlo
      por menú que muestre identidad/rol y pida confirmación o presente una acción explícita.
- [x] **Metadata y branding.** Agregar favicon y metadata por página; verificar títulos para login,
      actividad y estadísticas.
- [x] **Hacer que el botón de logout sea un poco más declarativo ya que actualmente no se sabe lo que hace
      hasta que se hace click en él.** Agregar un menú desplegable con la opción de cerrar sesión y mostrar
      el nombre del usuario.
- [x] **Aplicar filtros de búsqueda de actividad automáticamente sin necesidad de darle al botón de aplicar.**
- [x] **Los participantes de una actividad, desde el pov de otro participante aparecen como "I".** Debería
      mostrarse aunque sea las iniciales del nombre del participante, y al pararse encima con el cursor mostrar
      el nombre completo.

## Estadísticas admin

- [x] **Pulir filtros de rango.** Mostrar fechas formateadas en zona local, preservar el rango en la
      URL o al navegar, agregar presets útiles y reintento ante error. Verificar extremos inclusivos y
      días con cambios de horario.
- [x] **Mejorar la visualización.** Agregar comparaciones o gráficos sólo si aportan legibilidad;
      mantener cards y tablas accesibles para actividades creadas, reprogramadas, canceladas,
      suspendidas por clima y llamadas al proveedor.
- [x] **Probar autorización en profundidad.** Confirmar que usuarios sin `ADMIN` no vean el acceso,
      sean redirigidos si escriben `/estadisticas` y reciban 403 del backend incluso si llaman al
      proxy manualmente.

## Pruebas y calidad

- [x] **Agregar tests automatizados de componentes y hooks.** Vitest + Testing Library con
      137 tests en 21 archivos (todos verdes en `npm test`): auth/check-sso, filtros, wizard y su
      validación, imágenes multipart (object URLs), galería, voto, join/leave, notificaciones,
      estadísticas por rol/rango y async states.
- [x] **Agregar pruebas end-to-end.** Playwright configurado (`npm run test:e2e`) con proyectos
      mobile/desktop y specs en `e2e/`: login/refresh/logout, crear con y sin imágenes, explorar/
      filtros, detalle/galería/clima, join/leave, voto, notificaciones, estadísticas admin y rechazo
      para USER. Corren contra el stack completo (`docker compose up`) y hay job on-demand en CI;
      las que dependen de datos sembrados se auto-saltan si el stack no los tiene.
- [x] **Probar fallos reales.** `api.test.ts` cubre 400/401/403/404/409/413/500/503 y mensajes no-JSON;
      `authFetch.test.ts` cubre JWT vencido y refresh fallido; ErrorState/LoadingState y los hooks
      prueban slowness/caída (rechazos) con reintento. Faltaría verificar contra infraestructura
      real caída (Keycloak/Mongo/MinIO abajo), que hoy se simula con mocks.
- [x] **Agregar CI del frontend.** `.github/workflows/frontend-ci.yml`: en cada PR corre `npm ci`,
      `npm run lint`, `npm run typecheck`, `npm test` y `npm run build`; job E2E on-demand
      (`workflow_dispatch`) contra el stack de `docker compose`.
- [x] **Revisar warnings y tamaño del bundle.** Build de producción limpio (Turbopack, sin warnings
      propios; Next.js colecta telemetría anónima por defecto). Leaflet sigue client-only y es el
      bundle más pesado: ~196 KiB de JS (react-leaflet) + ~145 KiB de chunk de Leaflet/CSS, cargado
      sólo en el wizard (paso 2) y detalle. Medición reproducible con `npm run build:analyze`
      (usa `--webpack`, el analyzer de Turbopack no genera reporte). Dependencias grandes globales:
      framework ~185 KiB, polyfills ~110 KiB, main ~130 KiB.

## Infraestructura y despliegue

- [x] **Agregar healthcheck del frontend en Compose** y hacer que la disponibilidad del stack pueda
      verificarse automáticamente después de `docker compose up --build`.
- [ ] **Externalizar credenciales locales sensibles.** Los defaults de Keycloak, Mongo y MinIO son
      sólo para desarrollo; documentar variables obligatorias y eliminar defaults inseguros para un
      despliegue real.
- [ ] **Definir estrategia de producción.** Configurar hosts públicos, TLS, redirect URIs/web origins
      de Keycloak, `BACKEND_URL`, storage GCS/MinIO, política CORS/CSP y observabilidad del frontend.

## Implementado y verificado

- [x] TypeScript estricto, Redux Toolkit y proxy Next.js hacia el backend.
- [x] Autenticación Keycloak con Authorization Code + PKCE, roles y restauración SSO con `check-sso`;
      tokens únicamente en memoria.
- [x] Integración real de actividades, detalle, clima, join/leave, votación, notificaciones y creación
      multipart.
- [x] Wizard de cinco pasos con validación por paso: información, lugar/fecha, clima, imágenes y
      alertas.
- [x] Búsqueda y selección de ubicación con Nominatim + Leaflet/OpenStreetMap; envío de coordenadas.
- [x] Primera imagen en cards, galería deslizable en detalle y placeholder para actividades sin
      imágenes.
- [x] Estadísticas exclusivas para `ADMIN` con rango de fechas inclusivo y últimos siete días.
- [x] Notificaciones marcables como leídas.
- [x] Layout responsive inicial con feeds en grilla desktop y chrome inferior funcional en mobile y
      desktop.
- [x] Build Docker del frontend y stack completo en Docker Compose.

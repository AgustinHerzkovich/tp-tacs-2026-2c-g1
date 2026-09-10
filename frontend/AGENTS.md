# Frontend Guide

## Toolchain

- Next.js (App Router) + React, TypeScript (`strict: true`, `noUncheckedIndexedAccess`), Tailwind CSS v4, shadcn/ui (Radix-based, "Nova" preset), Redux Toolkit.
- `npm run dev` starts the dev server; `npm run lint` runs ESLint; `npx tsc --noEmit` type-checks; `npm run build` produces a production build. Run all three before calling something done — each has caught real bugs the others missed.
- Add shadcn components with `npx shadcn@latest add <component>` — it writes into `src/components/ui` as `.tsx` using this project's `components.json` config (`"tsx": true`). Don't hand-write files there.
- No `.js`/`.jsx` files under `src/` — everything is `.ts`/`.tsx`, including `src/pages/api/**`.

## Project shape: page / component / hook

- `src/app/**/page.tsx` — Next.js route files. Keep them thin: import a component from `src/components/pages` and render it. Route params (dynamic segments) are async in this Next version — `await params` in the page before passing values down.
- `src/components/pages/*Page.tsx` — the actual screen implementation (state wiring, layout composition). One per route; wizard steps live under `src/components/pages/wizard/`.
- `src/components/` (`activities/`, `common/`, `layout/`, `ui/`) — reusable presentational pieces. `ui/` is shadcn-managed (see above); everything else is ours.
- `src/hooks/` — state and business logic (`useActivities`, `useVoting`, `useWizardForm`, `useAuth`, `useRequireAuth`, etc.). Pages call hooks; components stay presentational and receive data/handlers as props. Redux lives behind hooks too (`useAuth` wraps `useAppSelector`/`useAppDispatch`) — components should never import `react-redux` directly.
- `src/app/(withBottomChrome)/(withHeader)/` — Explorar and Mis Actividades: get the top app bar (`Header`) *and* the persistent bottom chrome (tab bar + the "Crear Nueva Actividad" CTA, the latter shown only on `/mis-actividades`). `src/app/(withBottomChrome)/actividades/[id]/` — the detail page: bottom chrome only, no top bar (it uses its own hero image + back button). `src/app/crear/` and `src/app/login/` sit outside both groups on purpose — the wizard and the login screen are full-screen flows with none of that chrome. Route groups don't affect the URL; see each `layout.tsx`'s docblock before changing which screens share which chrome.
- `src/data/mockData.ts` — placeholder data for activities/notifications. `useActivities`/`useNotifications` are the only things that should import it directly; swap their bodies for real fetches without touching the pages/components that consume them.
- `src/types/backend.ts` — types mirroring the backend's DTOs field-for-field (see Backend integration below). `src/types/domain.ts` — UI-facing types for the mock-data-driven screens; this is where the eventual backend→UI mapping layer's output type should live.

## State: Redux

- `src/store/store.ts` exports `makeStore()` (a factory, not a singleton — required for the App Router so server requests never share state across users) plus `RootState`/`AppDispatch` types.
- `src/store/StoreProvider.tsx` is a client component that creates one store per session via `useState(() => makeStore())` — **not** `useRef`; this project has `reactCompiler: true` and the compiler's ref-in-render lint (`react-hooks/refs`) rejects the classic lazy-ref-init idiom.
- `src/store/hooks.ts` exports typed `useAppDispatch`/`useAppSelector`. Don't import the untyped ones from `react-redux` elsewhere.
- The `session` slice holds only non-sensitive Keycloak claims (`sub`, display name and realm roles) plus an `initialized` flag. `AuthBootstrap` initializes the adapter without an automatic SSO redirect, and `useRequireAuth()` waits for initialization before redirecting.
- Access and refresh tokens remain in the in-memory `keycloak-js` instance. Never persist them in Redux, `localStorage` or `sessionStorage`. Browser calls to `src/pages/api/**` must use `authFetch`, which refreshes and adds the bearer token.

## Backend integration

- The Spring Boot backend (`../backend`) is never called directly from client code. Every backend-bound request goes through a Next.js API route under `src/pages/api/**`, which proxies to `BACKEND_URL` (see `.env.example`) via the helpers in `src/lib/backendProxy.ts` (`proxyJson` for normal requests, `proxyRaw` for the multipart activity-creation endpoint).
- `src/pages/api` mirrors the backend's real routes 1:1 (`/activities`, `/votations/:id/votes/me`, `/notifications`, etc.) — check the backend's controllers under `backend/src/main/java/com/solnotfound/controller` and DTOs under `backend/src/main/java/com/solnotfound/dto` before adding a new proxy route or type, rather than guessing a shape.
- This is the Pages Router's `pages/api` convention coexisting with the App Router's `src/app`; that split is intentional, don't move these into `src/app/api`.

## Styling

- Design tokens (palette, fonts, radii) live in `src/app/globals.css` as CSS custom properties feeding a Tailwind v4 `@theme` block — change colors/fonts there, not per-component.
- Fonts are loaded via `next/font/google` in `src/app/layout.tsx` (Fredoka for display/headings, Nunito for body), exposed as `--font-display`/`--font-body`.
- The whole app renders inside a fixed `max-w-[430px]` centered column (set in `src/app/layout.tsx`) so it looks like a phone screen regardless of viewport — deliberate, not a bug; don't add a `container`/`max-w-*` override elsewhere that fights it.
- A dynamically-interpolated Tailwind class (e.g. `` `bg-[${someVar}]` ``) will never be generated — Tailwind's JIT only scans literal strings in source. For per-instance colors (like the wizard's gradient sliders), set a CSS custom property via inline `style` and consume it from a real rule in `globals.css` instead (see `[data-slot="slider-range"]`).

## Pending work

See [TODO.md](TODO.md) for the prioritized backlog (real backend data, auth, loading/error/empty states, tests, deploy).

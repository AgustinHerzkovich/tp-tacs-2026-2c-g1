# Planazo — Repository Guide for AI Assistants

This is the single source of instructions for AI coding assistants (Codex, Claude Code, Copilot, etc.)
working on this repository. `CLAUDE.md` imports this file; do not add per-folder `AGENTS.md` or
`CLAUDE.md` files — extend this one instead. Human-facing documentation lives in `README.md`,
`backend/README.md`, `frontend/README.md` and `docs/`.

## Project

Planazo is the TACS 2026-C2 coursework app ("404 Sol not found"): users organize outdoor activities,
the app monitors the weather forecast and, when it turns bad, opens a vote among participants to
reschedule or cancel. The statement is in `TP-TACS-2026C2.md` (not versioned).

| Path | Contents |
| --- | --- |
| `backend/` | Java 21 / Spring Boot 4.1 REST API (Maven module `server`). |
| `frontend/` | Next.js App Router + React + TypeScript UI. |
| `keycloak-import/`, `keycloak-theme/`, `keycloak/` | Realm export, login theme, cloud image and local configuration script. |
| `docker-compose.yaml` | Full local stack: frontend, backend, MongoDB, MinIO, Keycloak and its Postgres. |
| `terraform/`, `cloudbuild/`, `.github/workflows/` | GCP infrastructure, image builds, CI and deploys. |
| `docs/` | Traceability matrix, Swagger test cases, load test and Cloud Build notes. |

Run everything locally with `docker compose up --build --wait` from the repository root.

## Rules That Apply Everywhere

- **Language.** Everything the end user sees (UI text, error messages shown in the UI, Keycloak
  screens) is in Rioplatense Spanish (`vos`: "Iniciá sesión", "Probá de nuevo"). Code identifiers,
  code comments and Javadoc/TSDoc stay in English. User-facing docs (`README.md`) are in Spanish.
- **Never expose infrastructure to the end user.** UI text and error messages must not mention
  Keycloak, MongoDB, MinIO, Open-Meteo internals, internal API routes, stack traces or status-code
  details. Show a friendly generic message instead.
- **Secrets.** Never commit `.env`, `.env.local`, `*.tfvars`, Terraform state, API keys or
  credentials. Configuration goes through environment variables (see `.env.example` and
  `frontend/.env.example`). The `alumno/alumno` and `admin/admin` accounts are development-only.
- **Tests are part of the change.** A use case without tests is not done. Weather evaluation,
  alternative generation and votation resolution must stay testable without the external weather
  provider (use `IWeatherAdapter` fakes / the in-memory adapter).
- **Keep changes small and verified.** Do not report a change as finished without running the
  relevant gate below. Do not reformat or refactor unrelated code.
- **Git.** Git flow: branch from `develop` (`feature/*`, `hotfix/*`), open a PR, merge to `develop`;
  releases go to `main` and deliveries are tagged `Entrega_XX`. Do not commit or push unless a team
  member explicitly asks for it.

## Verification Gates

| Area | Command (run from the area's folder) |
| --- | --- |
| Backend, full | `./mvnw verify` (`mvnw.cmd` on Windows): tests + Spotless + Checkstyle + SpotBugs. |
| Backend, format | `./mvnw spotless:apply`, then `./mvnw spotless:check`. |
| Backend, focused test | `./mvnw -Dtest=ActivityServiceTest test` or `-Dtest=Class#method`. |
| Frontend | `npm run lint`, `npm run typecheck`, `npm test`, `npm run build`. |
| Frontend E2E | `npm run test:e2e` against the running stack (`APP_SEED_ENABLED=true docker compose up --build --wait`). |

CI mirrors these gates: `.github/workflows/backend-ci.yml` (Spotless → Checkstyle → SpotBugs →
tests) and `.github/workflows/frontend-ci.yml` (lint → typecheck → tests → build). The pre-commit
hook in `.githooks/` only runs `spotless:check` when backend Java files are staged.

## Backend (`backend/`)

- Java 21 is required. If `java -version` reports 17, point `JAVA_HOME` to a JDK 21 before running
  Maven (`class file version 65.0 ... up to 61.0` means a Java version mismatch).
- Layers: `controller` → `service` → `repository` interfaces (`I*Repository`) implemented with
  Spring Data MongoDB. Services depend on interfaces, never on Mongo types. Entities live in
  `entity/*`; request/response records in `dto/`; conversions in `mapper/`.
- External providers are adapters: weather behind `adapters/IWeatherAdapter` (Open-Meteo with
  caches, timeout, retry and circuit breaker, plus an in-memory adapter selected with
  `WEATHER_PROVIDER=in-memory`); images behind `storage/ImageStorage` (MinIO, GCS or no-op).
  Weather unavailability must never be interpreted as good weather.
- Identity comes only from the verified JWT `sub` claim (Spring Security resource server against
  Keycloak). `/statistics` requires the realm role `ADMIN`; re-check roles in the backend, never rely
  on the UI.
- Periodic work lives in `service/schedulers/` (weather check, votation closing, activity status)
  with cron expressions configurable in `application.properties`.
- Errors are returned as `ProblemDetail` from `exception/GlobalExceptionHandler`. Validation is
  split between Jakarta annotations on DTOs (field rules) and services (cross-field rules); keep
  both covered by tests.
- Tests: unit tests use the in-memory repositories under `src/test/.../repository` and do not start
  Spring or Docker. Persistence tests (`MongoPersistenceTest`, `StatisticsEventRepositoryTest`, etc.)
  use Testcontainers and need Docker.
- Every new non-trivial method needs Javadoc describing behavior, business rules, side effects,
  return value and exceptional cases. No redundant Javadoc on getters, setters, constructors or
  direct delegations.
- Spotless enforces Google Java Format and Unix line endings; SpotBugs runs with `Max` effort and
  `Low` threshold, so justify any `@SuppressFBWarnings`.
- The Docker image build runs `./mvnw clean verify` (excluding tests that need Docker), so quality
  failures also break `docker compose build`.

## Frontend (`frontend/`)

- Next.js App Router + React, TypeScript `strict` with `noUncheckedIndexedAccess`, Tailwind CSS v4,
  shadcn/ui and Redux Toolkit. Only `.ts`/`.tsx` files under `src/`.
- **This is not the Next.js from your training data.** The installed version has breaking changes;
  read the relevant guide in `frontend/node_modules/next/dist/docs/` before writing Next.js code and
  heed deprecation notices. Route params are async: `await params` in pages.
- Structure: `src/app/**/page.tsx` stays thin and renders a screen from `src/components/pages/`;
  reusable presentational pieces go in `src/components/{activities,common,layout,search}`; state and
  business logic go in `src/hooks/`. Components receive data and handlers as props and never import
  `react-redux` directly — use the typed hooks in `src/store/hooks.ts`.
- `src/components/ui/` is managed by shadcn: add components with `npx shadcn@latest add <name>`
  instead of hand-writing them.
- Route groups under `src/app/(withBottomChrome)/` decide which screens get the header and bottom
  navigation; read each `layout.tsx` docblock before moving screens between groups.
- Backend access: the browser never calls the Spring API directly. Every call goes through
  `src/lib/api.ts` → `authFetch` (adds a fresh bearer token) → a proxy route under `src/pages/api/**`
  that mirrors the backend route 1:1 via `src/lib/backendProxy.ts`. Keep `pages/api` in the Pages
  Router; do not move it to `src/app/api`. Check backend controllers and DTOs before adding a route
  or a type in `src/types/backend.ts`.
- Auth: `keycloak-js` with Authorization Code + PKCE. Access and refresh tokens stay in the
  in-memory adapter; never store them in Redux, `localStorage` or `sessionStorage`. The `session`
  slice holds only non-sensitive claims.
- Styling: design tokens live in `src/app/globals.css` (Tailwind `@theme`). Nunito is the UI font;
  the brand font (`.font-brand`, `.font-brand-title`) is only for logo, hero titles and card titles.
  Tailwind cannot generate dynamically interpolated classes — use a CSS custom property plus a real
  rule in `globals.css`.
- Tests: Vitest + Testing Library next to the code (`*.test.ts(x)`), Playwright specs in `e2e/`.
- `next dev`, when launched by an AI agent, regenerates `frontend/AGENTS.md` and `frontend/CLAUDE.md`
  with Next.js boilerplate. Both paths are git-ignored on purpose; do not commit them.

## Infrastructure

- Local: `docker compose up --build --wait`. `keycloak/configure-local.sh` runs after Keycloak starts
  and re-applies realm settings (theme, language, test users) on every start.
- Keycloak imports `keycloak-import/realm-export.json` only when the realm does not exist yet. In
  environments with a persistent database (GCP), later realm changes must be applied through the
  admin console or `kcadm.sh`, not only by editing the export.
- Cloud: Terraform in `terraform/` provisions GCP (Cloud Run, Cloud SQL, Artifact Registry, secrets,
  scheduler); `.github/workflows/deploy.yml` builds with `cloudbuild/*.yaml` and deploys only the
  services that changed. Never run `terraform apply` or deploy without the team asking for it.

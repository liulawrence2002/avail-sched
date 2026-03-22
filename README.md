# Goblin Scheduler

[![CI](https://github.com/liulawrence2002/avail-sched/actions/workflows/ci.yml/badge.svg)](https://github.com/liulawrence2002/avail-sched/actions/workflows/ci.yml)

Free-tier-friendly group availability scheduler with a goofy toggleable UI ("Goblin Mode" vs "Serious Mode").

## Current State

The application is a functional MVP with the core scheduling flow implemented end to end:

- Event creation with configurable duration, slot granularity, date range, and daily time windows
- Token-based access with no accounts required
- Availability voting with weighted preferences (Yes / Maybe / Snacks / No)
- Result scoring that ranks the top 5 time slots
- Host finalization plus ICS calendar export
- Basic analytics for event views and responses
- Simple per-IP rate limiting
- Theme toggle between Goblin and Serious modes

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React 18 + Vite 5 + TailwindCSS 3 + React Router 6 |
| Backend | Spring Boot 3.3 + Java 21 + Gradle |
| Database | PostgreSQL 16 + Flyway |
| Local Infra | Docker Compose |

## Repo Layout

```text
avail-sched/
|-- backend/   Spring Boot REST API
|-- frontend/  React SPA
|-- infra/     Docker Compose and local env examples
`-- README.md
```

## API Endpoints

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/events` | None | Create event |
| GET | `/api/events/{publicId}` | None | Get event details |
| POST | `/api/events/{publicId}/participants` | None | Join event |
| PUT | `/api/events/{publicId}/participants/{token}/availability` | Participant token | Save availability |
| GET | `/api/events/{publicId}/results` | None | Get scored results |
| POST | `/api/events/{publicId}/finalize?hostToken=...` | Host token | Finalize a slot |
| GET | `/api/events/{publicId}/final` | None | Get finalized slot |
| GET | `/api/events/{publicId}/final.ics` | None | Download ICS file |
| GET | `/api/host/{hostToken}` | Host token | Get event by host token |

## API Docs

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI spec: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

When the full Docker stack is running, the frontend container also proxies docs through:

- [http://localhost:3001/swagger-ui.html](http://localhost:3001/swagger-ui.html)
- [http://localhost:3001/api-docs](http://localhost:3001/api-docs)

## Database Schema

Flyway manages five tables in `backend/src/main/resources/db/migration/V1__init.sql`:

- `events` - event metadata plus public and host tokens
- `participants` - participant name and token per event
- `availability` - per-slot vote weights
- `final_selection` - the host-selected winning slot
- `event_stats` - view and response counters

## Scoring Algorithm

1. Generate candidate slots from the event date range, daily window, slot size, and timezone.
2. Expand each candidate slot into sub-slots based on the event duration.
3. Average each participant's weights across the required sub-slots.
4. Categorize the result as yes, maybe, snacks, or no.
5. Sum participant scores and return the top 5 slots by score.

## Local Setup

Two local workflows are supported. Pick one and stick to it for a session so you do not fight over port `8080`.

### Option A. Hybrid dev

Run Postgres in Docker, but keep the backend and frontend local.

1. Start Postgres only:

```bash
docker compose -f infra/docker-compose.yml up -d postgres
```

If you want to override the defaults, copy `infra/.env.example` to `infra/.env` and run:

```bash
docker compose --env-file infra/.env -f infra/docker-compose.yml up -d postgres
```

2. Run the backend:

```bash
cd backend
./gradlew bootRun
```

On Windows PowerShell:

```powershell
cd backend
.\gradlew.bat bootRun
```

3. Run the frontend:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs at `http://localhost:5173`. By default the frontend uses same-origin `/api`, and Vite proxies API, Swagger, and OpenAPI requests to `http://localhost:8080`.

### Option B. Full stack Docker

Run Postgres, backend, and frontend together:

```bash
docker compose -f infra/docker-compose.yml --profile fullstack up --build
```

Services:

- Frontend: http://localhost:3001
- Backend API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Frontend-routed API/docs: `http://localhost:3001/api`, `http://localhost:3001/api-docs`, `http://localhost:3001/swagger-ui.html`

## Environment

- Copy `infra/.env.example` to `infra/.env` and pass it with `--env-file infra/.env` if you want to override local DB or Docker full-stack URLs.
- Copy `backend/.env.example` values into your deploy platform environment.
- Copy `frontend/.env.example` to `frontend/.env.local` only if you need to override the default same-origin `/api` behavior.
- `APP_BASE_URL` should point at the frontend URL because the backend returns host links meant for the UI.
  - Hybrid local dev: `http://localhost:5173`
  - Full stack Docker: `http://localhost:3001`
- `BACKEND_PORT` controls the Docker backend host port and defaults to `8080`.
- `VITE_API_BASE_URL` is optional. Leave it as `/api` for same-origin local and Docker use, or point it at a separate backend origin for split frontend/backend deploys.

## Troubleshooting

- If `./gradlew bootRun` fails with `Port 8080 is already in use`, you likely started the full Docker stack. Stop the Docker backend or use the hybrid flow and start only `postgres`.
- If `docker compose` fails because `0.0.0.0:8080` is unavailable, a local backend is already listening on that port. Stop `bootRun` or set `BACKEND_PORT` in `infra/.env` and pass `--env-file infra/.env` when starting the full stack.
- If `docker compose` fails before it reads the service config on Windows, Docker Desktop is not fully available yet. Start Docker Desktop and wait until the engine is healthy.
- If generated host links point at the wrong site, set `APP_BASE_URL` before starting the backend process or container.
- If the frontend loads but API calls fail, confirm whether you are using the default same-origin `/api` path or an explicit `VITE_API_BASE_URL` override.

## Deploy

### Frontend on Vercel

1. Import the repo.
2. Set root directory to `frontend`.
3. Build command: `npm run build`
4. Output directory: `dist`
5. Set `VITE_API_BASE_URL` to your backend URL when deploying to a separate backend origin.

### Backend on Render

1. Create a PostgreSQL database on Neon or Supabase.
2. Create a Render web service from this repo with root directory `backend`.
3. Build command: `./gradlew build`
4. Start command: `./gradlew bootRun`
5. Set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`, and `APP_BASE_URL`.

### Backend on Fly.io

1. Provision a PostgreSQL-compatible database.
2. Deploy the `backend` directory as a Java app.
3. Set the same env vars as Render.

### Database on Neon

1. Create a free Postgres project.
2. Copy the pooled connection string into `SPRING_DATASOURCE_URL`.
3. Flyway runs automatically on startup.

## Key Architecture Decisions

- No ORM: JdbcTemplate with explicit SQL keeps the backend lean.
- No auth provider: secure random tokens gate host and participant access.
- Stateless API: no server-side sessions.
- In-memory result cache: short TTL and explicit eviction on vote updates.
- UTC storage: slot timestamps are stored as UTC instants and rendered in the event timezone.
- PostgreSQL only: the app targets a single, production-like database engine.

## Tests

Backend and frontend checks now cover both fast local feedback and Docker wiring:

- `cd backend && ./gradlew test` - unit and slice tests
- `cd backend && ./gradlew integrationTest` - PostgreSQL-backed event lifecycle integration test
- `cd frontend && npm run build` - production frontend build
- CI also runs a Docker Compose smoke test for the full stack (`--profile fullstack`)

## What Still Needs To Be Done

### High Priority

- [ ] Browser E2E tests - no Playwright/Cypress coverage for real browser flows yet
- [ ] Input sanitization - frontend rendering still relies mostly on React escaping and backend stripping
- [ ] HTTPS enforcement - still delegated to the deployment platform

### Medium Priority

- [ ] Edit event after creation
- [ ] Edit participant name
- [ ] Delete event
- [ ] Event expiry / cleanup
- [ ] Re-finalization guard
- [ ] Better loading and skeleton states
- [ ] Accessibility improvements for keyboard and screen reader use

### Low Priority

- [ ] Email / notification support
- [ ] Participant management page
- [ ] Bulk export
- [ ] Event search / directory
- [ ] Structured production logging beyond Spring defaults

## Free-Tier Notes

- No Stripe, Auth0, SendGrid, or hosted analytics dependencies are required.
- ICS downloads are generated server-side with no third-party calendar integration.
- Host and participant links are bearer secrets and should be shared carefully.

# Goblin Scheduler

Free-tier-friendly group availability scheduler with a goofy toggleable UI ("Goblin Mode" vs "Serious Mode").

## Current State (MVP)

The application is a **fully functional MVP**. All core scheduling workflows are implemented end-to-end:

- **Event creation** with configurable duration, slot granularity, date range, and daily time windows
- **Token-based access** — no login required; hosts and participants use secure random URL tokens
- **Availability voting** with weighted preferences (Yes / Maybe / Snacks / No)
- **Scoring engine** that ranks the top 5 time slots across all participants
- **Host finalization** of a chosen slot with ICS calendar file export
- **Analytics** tracking view count and response count per event
- **Rate limiting** (120 req/60s per IP per endpoint)
- **Theme toggle** between fun "Goblin Mode" and professional "Serious Mode" (persisted in localStorage)

## Tech Stack

| Layer        | Technology                                          |
|--------------|-----------------------------------------------------|
| Frontend     | React 18 + Vite 5 + TailwindCSS 3 + React Router 6 |
| Backend      | Spring Boot 3.3 + Java 21 + Gradle                  |
| Database     | PostgreSQL 16 + Flyway migrations                   |
| Local Infra  | Docker Compose (Postgres)                            |

## Repo Layout

```
avail-sched/
├── backend/          Spring Boot REST API
│   ├── src/main/java/com/goblin/scheduler/
│   │   ├── config/       AppConfig, AppProperties (CORS, cache, rate-limit)
│   │   ├── dto/          Request/response records (11 files)
│   │   ├── model/        Domain records: Event, Participant, AvailabilityRecord, FinalSelection, EventStats
│   │   ├── repo/         JdbcTemplate repositories (5 files)
│   │   ├── service/      EventService, SlotService, ScoringService, ResultCache
│   │   ├── util/         TokenGenerator (secure random tokens)
│   │   └── web/          EventController, ApiExceptionHandler, RateLimitFilter
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/V1__init.sql
│   └── src/test/         ScoringServiceTest, SlotServiceTest
├── frontend/         React SPA
│   └── src/
│       ├── pages/        LandingPage, CreatePage, EventPage, ResultsPage, HostPage
│       ├── components/   Card, TimeGrid (drag-to-select availability grid)
│       ├── api.js        Axios-free fetch client
│       ├── utils.js      Slot formatting, grid building
│       ├── useMode.js    Theme toggle hook
│       └── styles.css    Tailwind + custom theme styles
├── infra/
│   ├── docker-compose.yml   Local Postgres 16
│   └── .env.example
└── README.md
```

## API Endpoints

| Method | Path                                                | Auth        | Description                |
|--------|-----------------------------------------------------|-------------|----------------------------|
| POST   | `/api/events`                                       | None        | Create event               |
| GET    | `/api/events/{publicId}`                             | None        | Get event details          |
| POST   | `/api/events/{publicId}/participants`                | None        | Join event                 |
| PUT    | `/api/events/{publicId}/participants/{token}/availability` | Participant token | Save availability   |
| GET    | `/api/events/{publicId}/results`                     | None        | Get scored results (cached 30s) |
| POST   | `/api/events/{publicId}/finalize?hostToken=...`      | Host token  | Finalize a slot            |
| GET    | `/api/events/{publicId}/final`                       | None        | Get finalized slot         |
| GET    | `/api/events/{publicId}/final.ics`                   | None        | Download ICS calendar file |
| GET    | `/api/host/{hostToken}`                              | Host token  | Get event by host token    |

## Database Schema

Five tables managed by Flyway (`V1__init.sql`):

- **events** — event metadata (title, timezone, dates, slot config, public ID, host token)
- **participants** — display name + unique token per event
- **availability** — per-participant slot weights (1.0=yes, 0.6=maybe, 0.3=bribe, 0.0=no)
- **final_selection** — one finalized slot per event
- **event_stats** — view and response counters

## Scoring Algorithm

1. Generate all candidate slots from the event's date range, daily time window, and timezone
2. For each candidate slot, collect sub-slots based on duration (e.g., a 60-min event with 30-min granularity = 2 sub-slots)
3. Average each participant's weights across sub-slots to get a per-participant score
4. Categorize: yes (≥0.99), maybe (≥0.59), bribe (≥0.29), no (<0.29)
5. Sum scores across participants, return top 5 slots sorted by score descending

## Local Setup

### 1. Start Postgres

```bash
docker compose -f infra/docker-compose.yml up
```

### 2. Run backend

```bash
cd backend
./gradlew bootRun
```

On Windows PowerShell:

```powershell
cd backend
.\gradlew.bat bootRun
```

### 3. Run frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend defaults to `http://localhost:8080/api`. The dev server runs at `http://localhost:5173`.

## Environment

- Copy `infra/.env.example` to `infra/.env` to override local DB defaults.
- Copy `backend/.env.example` values into your deploy platform environment.
- Copy `frontend/.env.example` to `frontend/.env.local` for local frontend overrides.
- `APP_BASE_URL` should point at the frontend app URL (e.g., `http://localhost:5173` locally) because the backend returns host links meant for the UI.

## Deploy

### Frontend on Vercel

1. Import the repo.
2. Set root directory to `frontend`.
3. Build command: `npm run build`
4. Output directory: `dist`
5. Set `VITE_API_BASE_URL` to your backend URL (e.g., `https://goblin-api.onrender.com/api`).

### Backend on Render

1. Create a PostgreSQL database on Neon or Supabase (free tier).
2. Create a Render Web Service from this repo with root directory `backend`.
3. Build command: `./gradlew build`
4. Start command: `./gradlew bootRun`
5. Set env vars: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS`, `APP_BASE_URL`.

### Backend on Fly.io

1. Provision a free PostgreSQL-compatible database (e.g., Neon).
2. Deploy the `backend` directory as a Java app.
3. Set the same env vars as Render.

### Database on Neon

1. Create a free Postgres project.
2. Copy the pooled connection string into `SPRING_DATASOURCE_URL`.
3. Flyway runs automatically on startup.

## Key Architecture Decisions

- **No ORM** — JdbcTemplate with explicit SQL for a lean MVP
- **No auth provider** — secure random tokens (32-byte URL-safe Base64) for host/participant access
- **Stateless API** — no server-side sessions
- **In-memory result cache** — 30-second TTL per event, evicted on availability update
- **UTC storage** — all availability timestamps stored as UTC instants, converted to event timezone for display
- **PostgreSQL only** — no multi-DB abstractions; targets free-tier managed services

## Tests

Two unit test classes exist:

- `ScoringServiceTest` — validates weighted scoring and sub-slot aggregation
- `SlotServiceTest` — validates candidate slot generation and timezone handling

## What Still Needs to Be Done

### High Priority

- [ ] **Integration / E2E tests** — no controller, repository, or end-to-end tests exist; only two unit tests cover `ScoringService` and `SlotService`
- [ ] **Input sanitization** — display names and event titles are rendered as-is; add XSS protection on the frontend
- [ ] **Error boundaries** — React app has no error boundaries; unhandled errors crash the whole page
- [ ] **Mobile responsiveness** — the TimeGrid drag-to-select component likely needs touch event support and responsive layout tuning
- [ ] **HTTPS enforcement** — no TLS configuration or redirect-to-HTTPS logic (relies on deploy platform)

### Medium Priority

- [ ] **Edit event after creation** — hosts cannot modify title, dates, or time windows after event creation
- [ ] **Edit participant name** — participants cannot change their display name
- [ ] **Delete event** — no endpoint or UI for event deletion / expiry
- [ ] **Event expiry / TTL** — old events live forever in the database with no cleanup
- [ ] **Re-finalization guard** — hosts can overwrite a finalized slot with no confirmation or undo
- [ ] **Loading / skeleton states** — pages show raw loading text; no skeleton UI or spinners
- [ ] **Accessibility (a11y)** — no ARIA labels, keyboard navigation for the time grid, or screen reader support

### Low Priority / Nice to Have

- [ ] **Email / notification support** — no way to notify participants of finalization (by design for free tier, but a natural next step)
- [ ] **Participant list page** — results show who can/cannot attend, but there's no dedicated participant management view
- [ ] **Bulk export** — no way to export all availability data (CSV, etc.)
- [ ] **Event search / directory** — events are only accessible via direct link
- [ ] **Timezone auto-detection** — create form doesn't pre-select the user's local timezone
- [ ] **Copy-to-clipboard** — host/participant links on the create success page would benefit from a copy button
- [ ] **CI/CD pipeline** — no GitHub Actions or other CI configuration
- [ ] **Dockerfile for backend** — no containerized build for the Spring Boot app
- [ ] **Production logging** — no structured logging or log level configuration beyond Spring defaults
- [ ] **Metrics / observability** — no health check endpoint, Prometheus metrics, or APM integration

## Free-Tier Notes

- No Stripe, Auth0, SendGrid, or third-party analytics.
- ICS downloads are generated server-side with no calendar integration.
- Host/participant links are bearer secrets — share carefully.

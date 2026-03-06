# Goblin Scheduler

Free-tier-friendly group availability scheduler with a goofy toggleable UI.

## Repo layout

- `frontend` React + Vite + TailwindCSS
- `backend` Spring Boot 3 + Java 21 + Gradle + Flyway
- `infra` Postgres Docker Compose + env examples

## Key choices

- No auth provider: host and participant access use secure random URL tokens.
- PostgreSQL only: local via Docker Compose, production-ready for Neon/Supabase/Render/Fly.
- Backend uses `JdbcTemplate` for a lean MVP and explicit SQL.
- Results are computed on demand and cached in memory for 30 seconds per event.
- Analytics are first-party only: event views and response counts stored in DB.

## Local setup

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

The frontend defaults to `http://localhost:8080/api`.

## Environment

- Copy `infra/.env.example` to `infra/.env` if you want to override local DB defaults.
- Copy `backend/.env.example` values into your deploy platform environment.
- Copy `frontend/.env.example` to `frontend/.env.local` for local frontend overrides.
- `APP_BASE_URL` should point at the frontend app URL because the backend returns a host link meant for the UI, for example `http://localhost:5173` locally.

## Deploy

### Frontend on Vercel

1. Import the repo.
2. Set root directory to `frontend`.
3. Build command: `npm run build`
4. Output directory: `dist`
5. Set `VITE_API_BASE_URL` to your backend public URL, for example `https://goblin-api.onrender.com/api`

### Backend on Render

1. Create a PostgreSQL database on Neon or Supabase free tier.
2. Create a Render Web Service from this repo with root directory `backend`.
3. Build command: `./gradlew build`
4. Start command: `./gradlew bootRun`
5. Set env vars:
   - `SPRING_DATASOURCE_URL`
   - `SPRING_DATASOURCE_USERNAME`
   - `SPRING_DATASOURCE_PASSWORD`
   - `APP_CORS_ALLOWED_ORIGINS=https://your-vercel-app.vercel.app`
   - `APP_BASE_URL=https://your-vercel-app.vercel.app`

### Backend on Fly.io

1. Provision a free PostgreSQL-compatible database such as Neon.
2. Deploy the `backend` directory as a Java app.
3. Set the same env vars as Render.

### Database on Neon

1. Create a free Postgres project.
2. Copy the pooled connection string into `SPRING_DATASOURCE_URL`.
3. Flyway runs automatically on startup.

## Free-tier notes

- No Stripe, Auth0, SendGrid, or third-party analytics.
- ICS downloads are generated server-side with no calendar integration.
- Host/participant links are bearer secrets, so share carefully.

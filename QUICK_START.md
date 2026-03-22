# Quick Start

Use one of the two startup flows below. Running both at the same time will usually conflict on port `8080` unless you override Docker's `BACKEND_PORT`.

## What You Need

- Docker Desktop running
- Node.js 20+
- Java 21

## Option 1. Full Stack Docker

Best if you just want the app running.

From the repo root:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack up --build
```

If `8080` is already busy, copy `infra/.env.example` to `infra/.env`, set `BACKEND_PORT`, and run:

```powershell
docker compose --env-file infra/.env -f infra/docker-compose.yml --profile fullstack up --build
```

Open:

- Frontend: http://localhost:3001
- Backend health: http://localhost:8080/actuator/health by default
- Swagger UI: http://localhost:3001/swagger-ui.html

Stop everything:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down -v
```

## Option 2. Hybrid Dev

Best if you want to edit backend or frontend locally.

1. Start Postgres only:

```powershell
docker compose -f infra/docker-compose.yml up -d postgres
```

2. Start the backend in a new terminal:

```powershell
cd backend
.\gradlew.bat bootRun
```

3. Start the frontend in another new terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open:

- Frontend: http://localhost:5173
- Backend health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:5173/swagger-ui.html

Stop Postgres when done:

```powershell
docker compose -f infra/docker-compose.yml down
```

## Environment Notes

- `APP_BASE_URL` should match the frontend URL.
  - Full stack Docker: `http://localhost:3001`
  - Hybrid dev: `http://localhost:5173`
- `BACKEND_PORT` controls the Docker backend host port and defaults to `8080`.
- `VITE_API_BASE_URL` can stay unset for local work. The app defaults to `/api`.

## If Something Breaks

- `Port 8080 is already in use`: stop whichever backend is already using it, or set `BACKEND_PORT` in `infra/.env` and start Docker with `--env-file infra/.env`.
- Docker errors before startup: wait for Docker Desktop to finish starting.
- Frontend loads but API fails: make sure the backend is running on the expected port (`8080` by default, or your `BACKEND_PORT` override).

## More Detail

See [README.md](README.md) for the full setup, environment variables, deploy notes, and troubleshooting.

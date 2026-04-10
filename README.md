# Goblin Scheduler

[![CI](https://github.com/liulawrence2002/avail-sched/actions/workflows/ci.yml/badge.svg)](https://github.com/liulawrence2002/avail-sched/actions/workflows/ci.yml)

Goblin Scheduler is a group availability app for dinners, clubs, workshops, and lightweight teams. You create one shareable event page, guests mark availability with no accounts, and the host locks the final time and exports it to calendar.

This README is Docker-first and written for people who are new to Docker.

## Overview

The app has 3 parts:

- `frontend`: the website people visit
- `backend`: the API that stores events and calculates results
- `postgres`: the database that keeps your event data

When you run the full stack in Docker, Docker creates:

- containers: the running instances of the app
- images: the built app packages Docker starts from
- volumes: persistent storage for the database

For this project, the main Docker volume is:

- `infra_goblin_postgres_data`

That volume is important because it stores your local Postgres data. If you remove it, your local events are deleted.

## What The App Does

- Create an event with a date range, daily hours, timezone, and duration
- Share one public link with guests
- Let guests join with a name only and save availability
- Show ranked results based on weighted responses
- Let the host finalize one winning slot
- Export the final choice as an `.ics` calendar file

## Repo Layout

```text
avail-sched/
|-- backend/   Spring Boot REST API
|-- frontend/  React SPA
|-- infra/     Docker Compose config
|-- QUICK_START.md
`-- README.md
```

## Docker Resources In This Project

When the full Docker stack is running, you will usually see:

- container `goblin-frontend`
- container `goblin-backend`
- container `goblin-postgres`
- image `infra-frontend`
- image `infra-backend`
- image `postgres:16-alpine`
- volume `infra_goblin_postgres_data`

## Quick Docker Start

From the repo root:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack up --build -d
```

Open:

- Frontend: http://localhost:3001
- Backend API: http://localhost:8080
- Backend health: http://localhost:8080/actuator/health
- Swagger UI through frontend: http://localhost:3001/swagger-ui.html

## Safe Docker Commands

### Start or rebuild the latest app

Use this when you want Docker to rebuild the frontend and backend from your current code:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack up --build -d
```

### Stop the app but keep your database data

Use this most of the time:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down
```

This stops and removes the containers and network, but keeps your Postgres volume.

### Rebuild app containers and remove old local app images, but keep database data

Use this when you want a clean app rebuild without deleting local event data:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down --remove-orphans --rmi local
docker compose -f infra/docker-compose.yml --profile fullstack up --build -d
```

This is the safest "clean and rebuild" flow for this project.

### Remove everything, including database data

Only use this if you want a completely fresh database:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down -v --remove-orphans --rmi local
```

The `-v` flag deletes the Postgres volume and your local event data.

## Common Docker Checks

See running containers:

```powershell
docker ps
```

See all containers, including stopped ones:

```powershell
docker ps -a
```

See images:

```powershell
docker images
```

See volumes:

```powershell
docker volume ls
```

Watch logs:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack logs -f
```

Watch backend logs only:

```powershell
docker compose -f infra/docker-compose.yml logs -f backend
```

## If You Want To Save Resources

- Use `down` when you are done instead of leaving containers running.
- Use `down --remove-orphans --rmi local` before a rebuild if you want to clear old local app images.
- Do not use `down -v` unless you really want to delete your local database data.
- The `postgres:16-alpine` image is normal to keep around if you still use this project.

## Hybrid Dev Option

If you want to edit code locally but still use Docker for the database:

1. Start Postgres only:

```powershell
docker compose -f infra/docker-compose.yml up -d postgres
```

2. Start the backend locally:

```powershell
cd backend
.\gradlew.bat bootRun
```

3. Start the frontend locally:

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

- `APP_BASE_URL` should point at the frontend URL because the backend generates host links for the UI.
- Full stack Docker default frontend URL: `http://localhost:3001`
- Full stack Docker default backend URL: `http://localhost:8080`
- `BACKEND_PORT` changes the backend host port in Docker if `8080` is already in use.
- `VITE_API_BASE_URL` usually stays as `/api` for local Docker use.

If you want custom ports or database settings, copy `infra/.env.example` to `infra/.env` and start Docker with:

```powershell
docker compose --env-file infra/.env -f infra/docker-compose.yml --profile fullstack up --build -d
```

## Troubleshooting

- If Docker says port `8080` is busy, stop any local backend already using it or set `BACKEND_PORT` in `infra/.env`.
- If Docker Desktop is not fully started yet, wait until the engine is healthy and run the command again.
- If the frontend loads but API calls fail, make sure `goblin-backend` is healthy and listening on the expected port.
- If you accidentally deleted the volume with `down -v`, your local Postgres data is gone and Docker will create a fresh empty database next time.

## Tests

- Backend unit tests:

```powershell
cd backend
.\gradlew.bat test
```

- Backend integration test:

```powershell
cd backend
.\gradlew.bat integrationTest
```

- Frontend production build:

```powershell
cd frontend
npm run build
```

## Tech Stack

| Layer | Technology |
| --- | --- |
| Frontend | React 18 + Vite 5 + TailwindCSS 3 + React Router 6 |
| Backend | Spring Boot 3.3 + Java 21 + Gradle |
| Database | PostgreSQL 16 + Flyway |
| Local Infra | Docker Compose |

## API Summary

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/events` | Create event |
| `GET` | `/api/events/{publicId}` | Get public event details |
| `POST` | `/api/events/{publicId}/participants` | Join as participant |
| `GET` | `/api/events/{publicId}/participants/{token}/availability` | Load saved participant availability |
| `PUT` | `/api/events/{publicId}/participants/{token}/availability` | Save participant availability |
| `GET` | `/api/events/{publicId}/results` | Get ranked results |
| `POST` | `/api/events/{publicId}/finalize?hostToken=...` | Finalize winning slot |
| `GET` | `/api/events/{publicId}/final` | Get finalized slot |
| `GET` | `/api/events/{publicId}/final.ics` | Download ICS |
| `GET` | `/api/host/{hostToken}` | Get host workspace data |

## Need The Short Version?

See [QUICK_START.md](QUICK_START.md) for the smallest set of commands.

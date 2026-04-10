# Quick Start

This project can run fully in Docker. If you are new to Docker, use the commands below and avoid `-v` unless you intentionally want to delete your local database data.

## What Docker Runs Here

- `goblin-frontend`: the website
- `goblin-backend`: the API
- `goblin-postgres`: the database
- `infra_goblin_postgres_data`: the database volume that keeps your local data

## Start The Full App

From the repo root:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack up --build -d
```

Open:

- Frontend: http://localhost:3001
- Backend health: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:3001/swagger-ui.html

## Stop The App But Keep Your Data

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down
```

## Rebuild Cleanly But Keep Your Data

Use this if you want to remove old local app containers/images and start fresh from the latest code:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down --remove-orphans --rmi local
docker compose -f infra/docker-compose.yml --profile fullstack up --build -d
```

This keeps the Postgres volume.

## Delete Everything, Including Local Database Data

Only use this if you want a completely fresh database:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack down -v --remove-orphans --rmi local
```

The `-v` flag deletes `infra_goblin_postgres_data`.

## Useful Checks

See running containers:

```powershell
docker ps
```

See logs:

```powershell
docker compose -f infra/docker-compose.yml --profile fullstack logs -f
```

## Hybrid Dev Option

If you want Docker only for Postgres:

```powershell
docker compose -f infra/docker-compose.yml up -d postgres
```

Then run:

```powershell
cd backend
.\gradlew.bat bootRun
```

and:

```powershell
cd frontend
npm install
npm run dev
```

## More Detail

See [README.md](README.md) for the full Docker explanation, overview, troubleshooting, and API summary.

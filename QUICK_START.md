# Goblin Scheduler — Quick Start

## What You Need

- **Docker Desktop** (or Docker Engine) — for Postgres
- **Java 21** — for the backend
- **Node.js 20+** — for the frontend

---

## Start Everything

### 1. Start Postgres (Docker)

```powershell
cd infra
docker compose up -d
```

Wait ~5 seconds for it to be ready.

### 2. Start the Backend (Spring Boot)

```powershell
cd backend
./gradlew.bat bootRun --no-daemon
```

Wait for `Tomcat started on port 8080`.

### 3. Start the Frontend (Vite)

```powershell
cd frontend
npm run dev
```

Open **http://localhost:5173**

---

## Stop Everything

### Stop Frontend & Backend

Press `Ctrl + C` in each terminal window.

### Stop Postgres & Remove Data

```powershell
cd infra
docker compose down -v
```

- `docker compose down` — stops the container
- `-v` — removes the Postgres volume (deletes all data)

---

## URLs

| Service | URL |
|---------|-----|
| App | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Postgres | localhost:5432 |

---

## Troubleshooting

**Port already in use?**
- Find the process: `netstat -ano | findstr :8080` or `findstr :5173`
- Kill it: `taskkill /PID <PID> /F`

**Backend won't start?**
- Make sure Java 21 is installed: `java -version`
- Make sure Postgres is running: `docker ps`

**Frontend build fails?**
- Make sure Node 20+ is installed: `node -v`
- Run `npm install` in the `frontend` folder

**Database migrations stuck?**
- The app auto-runs Flyway migrations on startup. No manual steps needed.

---

## File Overview

```
avail-sched/
├── backend/          # Spring Boot + Gradle
│   ├── src/main/     # Java source code
│   └── build.gradle  # Dependencies
├── frontend/         # React + Vite
│   ├── src/          # JSX source code
│   └── package.json  # Dependencies
├── infra/            # Docker Compose
│   └── docker-compose.yml
└── QUICK_START.md   # This file
```

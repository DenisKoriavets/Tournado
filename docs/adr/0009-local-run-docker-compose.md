# ADR-0009: Local run - Docker Compose for one-command startup

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The course requires the project to start locally with 1-2 commands and to be demoed live from a clean terminal. The system has four parts in three ecosystems: React (Node), Spring Boot (Java), FastAPI (Python) and PostgreSQL, plus a fake mail inbox for e-mail login (ADR-0007). Team members use different operating systems (Windows, possibly macOS/Linux).

## Decision drivers

- One command to start everything, identical on every laptop.
- No need to install Java, Node, Python and PostgreSQL to just run the project.
- Fast feedback (hot reload) while developing a single part.
- Zero budget, no cloud.

## Considered options

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Docker Compose for the full stack + native dev mode** | `docker compose up --build` starts all services; same versions everywhere; developers can still run their own part natively with hot reload | Docker Desktop must be installed; first build is slow |
| B. Native install of every tool, run each service manually | No Docker | 4-5 terminals; version mismatches between laptops; breaks the 1-2 command requirement |
| C. Cloud dev environments / free hosting | No local setup | Free tiers are unreliable; violates "local run" constraint |
| D. Docker only for PostgreSQL, everything else native | Lighter | Still requires all toolchains on the demo laptop |

## Decision

**Option A.** A root `docker-compose.yml` with these services:

| Service | Image / build | Port (host) | Purpose |
| :--- | :--- | :--- | :--- |
| `db` | `postgres` (official) | 5432 | Database, named volume for data |
| `mail` | `axllent/mailpit` | 8025 (web UI) | Fake inbox for e-mail login links |
| `ai-service` | build `ai-service/` | 8000 | FastAPI, `AI_PROVIDER=mock` by default |
| `core` | build `core/` (multi-stage Maven → JRE) | 8080 | Spring Boot; Flyway migrates the DB on startup |
| `frontend` | build `frontend/` (Vite build → Nginx) | 3000 | Serves the SPA and proxies `/api`, `/ws` to `core` |

Rules:
- **Run:** `cp .env.example .env` (once) and `docker compose up --build`. The README lists exactly these commands and how to verify: `GET http://localhost:8080/api/health`, UI at `http://localhost:3000`.
- Services start in dependency order with health checks (`db` healthy → `core`).
- All configuration via environment variables; `.env.example` contains safe defaults so the project starts without any secret (AI in `mock` mode, Steam login works without a key).
- **Dev mode:** a developer can stop one service and run it natively (`./mvnw spring-boot:run`, `npm run dev`, `uvicorn …`) against the other containers.

## Consequences

**Positive**
- Identical environment for all five members and for the live demo.
- New members (or the teacher) can run the project without installing language toolchains.

**Negative**
- Docker Desktop is required and uses RAM; Java image builds take a few minutes the first time.
- Two ways of running (containers / native) must both be documented.

**Risks**
- Port conflicts on a laptop (5432 often used by a local PostgreSQL). Mitigation: ports configurable in `.env`.
- Windows line endings breaking shell scripts in containers. Mitigation: `.gitattributes` with `eol=lf` for scripts.

## Related

ADR-0001, ADR-0002, ADR-0003, ADR-0004, ADR-0006, ADR-0007.

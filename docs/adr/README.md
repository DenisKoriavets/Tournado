# Architecture Decision Records - Tournado

This folder records the key architectural decisions for the Tournado MVP. Each ADR explains the context, the alternatives we compared, what we chose and what it costs us. The prompt used to produce them is in [`prompts/4.2_adr_generation.md`](../../prompts/4.2_adr_generation.md).

Inputs: [`docs/requirements.md`](../requirements.md) (MVP scope, user stories, acceptance criteria) and [`docs/analysis/requirements-digest.md`](../analysis/requirements-digest.md) (entities, BR-xx rules, NFR-xx, GAP-xx, ASM-xx).

## Team constraints (apply to every ADR)

| Constraint | Value |
| :--- | :--- |
| Team | 5 students: Core Platform & Auth, Tournament Engine, Game Integrations & Match Flow, AI & Automation, Frontend |
| Deadline | End of the semester; the MVP is demoed live |
| Budget | 0 - no paid hosting, no paid APIs |
| Runtime | Local only, on the students' laptops; start with 1-2 commands |
| Experience | Team vote: core in Java / Spring Boot, AI module in Python; frontend owner chose React + TypeScript. All members are roughly familiar with these stacks |
| Scale of the MVP | One tournament of 8-32 teams at a time, at most 16 concurrent matches, tens to hundreds of viewers |

## Decisions

| # | Decision | Summary |
| :--- | :--- | :--- |
| [0001](0001-monorepo-structure.md) | Monorepo | One repository with `frontend/`, `core/`, `ai-service/`, `docs/`, `prompts/` and a root `docker-compose.yml` |
| [0002](0002-frontend-react-typescript-vite.md) | Frontend stack | React + TypeScript + Vite single-page app |
| [0003](0003-backend-spring-boot-modular-monolith.md) | Backend stack | Java 21 + Spring Boot 3 as a single modular monolith (no microservices) |
| [0004](0004-persistence-postgresql-jpa-flyway.md) | Persistence | PostgreSQL + Spring Data JPA + Flyway; UUID keys, UTC timestamps, screenshots on a local volume |
| [0005](0005-realtime-websocket-and-server-timers.md) | Real-time and timers | Spring WebSocket (STOMP, built-in broker); deadlines stored in PostgreSQL and enforced by the server; **no Redis** |
| [0006](0006-ai-service-python-fastapi.md) | AI module | Separate Python / FastAPI service called synchronously over HTTP; pluggable vision-model provider; failure = "not recognised" |
| [0007](0007-authentication-steam-openid-email-link.md) | Authentication | Steam OpenID 2.0 + e-mail one-time link; JWT in an HttpOnly cookie |
| [0008](0008-external-game-integrations.md) | Game integrations | Only Chess.com public API (polling); no Steam Web API, no Supercell API, no game-server control |
| [0009](0009-local-run-docker-compose.md) | Local run | `docker compose up --build` starts everything; native dev mode for hot reload |

## Key decision in one sentence (for the demo)

We deliberately keep the MVP to **three runtime services plus a database** (React, Spring Boot, Python AI, PostgreSQL) and dropped Redis from the initial sketch: at our scale one Spring instance with PostgreSQL covers real-time state, timers and the AI call, and timers stored as deadlines in PostgreSQL survive restarts better than Redis key expiry (see [ADR-0005](0005-realtime-websocket-and-server-timers.md)).

## Status values

`Proposed` - under discussion · `Accepted` - agreed by the team · `Superseded by NNNN` - replaced by a later ADR.

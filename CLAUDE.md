# Tournado — AI Assistant Engineering Guide & Project Harness (CLAUDE.md)

> **Repository:** `DenisKoriavets/Tournado`  
> **Course:** Generative AI in Software Development — Practical 4 (Technical Design & AI-Harness)  
> **Single Source of Truth:** `docs/contracts/` (`data-schema.md`, `api-contracts.md`, `error-format.md`), `docs/adr/`, `docs/analysis/requirements-digest.md`, `docs/requirements.md`

This document defines the strict engineering guidelines, architecture, operational commands, and coding invariants that all AI assistants (Claude, Cursor, Copilot, Antigravity) must strictly obey when analyzing, generating, or modifying code in this repository.

---

## 1. Project Overview & Scope

**Tournado** is a modular web platform designed to automate the full lifecycle of amateur and collegiate esports tournaments without administrative chaos.

* **Supported Disciplines:**
  * **Counter-Strike 2 (CS2):** 5v5 team tactical shooter (Pick/Ban map veto, dedicated server connect string).
  * **Brawl Stars:** 3v3 mobile session game (friendly room codes, captain match coordination).
  * **Chess.com:** 1v1 classic board game (game invite links, automatic result verification via public Chess.com API).
* **Core Philosophy (Human-in-the-Loop):**
  * Normal match reporting relies on mutual confirmation by both captains (instant progression).
  * In case of score dispute (Dispute), an internal AI Vision service processes scoreboard screenshot evidence and suggests a verdict, but the tournament organizer makes the final ruling in 1 click (US-30..US-34).

---

## 2. Monorepo Directory Layout (ADR-0001)

The repository is structured as a lightweight monorepo without complex build orchestrators:

```
Tournado/
├── .github/
│   └── copilot-instructions.md   # Instructions for GitHub Copilot
├── ai-service/                   # Python 3.12 + FastAPI (Multimodal AI & OCR arbitration)
│   ├── app/                      # Application code (main.py, routers, schemas, services)
│   ├── tests/                    # Unit & integration tests
│   ├── Dockerfile                # Container build
│   └── pyproject.toml            # Dependencies (FastAPI, Uvicorn, Pillow, Pytest)
├── core/                         # Java 21 + Spring Boot 3.3+ (Core backend modular monolith)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tournado/core/
│   │   │   │   ├── common/       # RFC 7807 ProblemDetail, time source, security helpers
│   │   │   │   ├── identity/     # Auth, users, external game handles (US-01, US-02)
│   │   │   │   ├── teams/        # Teams, rosters, invite links, roster lock (US-03..US-06, US-14)
│   │   │   │   ├── tournaments/  # Lifecycle, config, registration, check-in, CSV (US-07..US-13, US-15..US-17, US-35, US-36)
│   │   │   │   ├── bracket/      # Single Elimination generation, seeding, byes (US-18..US-21, US-28)
│   │   │   │   ├── matches/      # Match Room, Ready Check, veto bans, score entry (US-22..US-27)
│   │   │   │   ├── integrations/ # Chess.com public API client (US-29)
│   │   │   │   └── disputes/     # Dispute tickets, screenshot uploads, AI client (US-30..US-34)
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/ # Flyway SQL migrations (V1__initial_schema.sql)
│   │   └── test/                 # AssertJ + Mockito unit & MockMvc integration tests
│   ├── pom.xml                   # Maven build descriptor
│   └── Dockerfile                # Multi-stage build (Maven -> Eclipse Temurin 21 JRE)
├── frontend/                     # React 19 + TypeScript + Vite + Tailwind CSS
│   ├── src/
│   │   ├── api/                  # Typed fetch clients, React Query hooks, error interceptor
│   │   ├── components/           # Reusable UI components (shadcn/ui-inspired, Lucide icons)
│   │   ├── features/             # Feature slices aligned with core modules (auth, bracket, match-room)
│   │   ├── types/                # TypeScript interfaces strictly matching contracts
│   │   ├── App.tsx
│   │   └── main.tsx
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile                # Multi-stage build (Node build -> Nginx reverse proxy)
├── docs/                         # Formal engineering specifications & contracts
│   ├── adr/                      # Architecture Decision Records (ADR-0001..0009)
│   ├── analysis/                 # requirements-digest.md (Entities E1..E13, Rules BR-01..60)
│   ├── contracts/                # data-schema.md, api-contracts.md, error-format.md
│   └── requirements.md           # User Stories US-01..US-36, Acceptance Criteria (Gherkin)
├── prompts/                      # Preserved prompts used in coursework practices (4.1..4.4)
├── docker-compose.yml            # One-command local runtime orchestration (ADR-0009)
├── .env.example                  # Environment configuration defaults
├── .cursorrules                  # Cursor IDE configuration
├── CLAUDE.md                     # Master AI instruction harness (this file)
└── README.md                     # Quickstart, local run, and project documentation
```

---

## 3. Local Execution & Build Commands (ADR-0009)

AI assistants must provide and recommend commands strictly according to these standards:

### 3.1 Docker Compose (Primary Runtime — One Command)
* **Start Full Stack:** `docker compose up --build`
* **Stop Full Stack:** `docker compose down -v`
* **Check Running Containers:** `docker compose ps`
* **Service Ports (Host):**
  * Frontend Web: `http://localhost:3000`
  * Core Backend API: `http://localhost:8080/api/v1` (Health: `GET http://localhost:8080/api/health`)
  * AI Service API: `http://localhost:8000` (Health: `GET http://localhost:8000/api/v1/ai/health`)
  * Mailpit Fake Mailbox: `http://localhost:8025`
  * PostgreSQL Database: `localhost:5432` (db: `tournado`, user: `tournado_user`, pass: `tournado_pass`)

### 3.2 Backend Core (`core/`)
* **Run Unit & Integration Tests:** `mvn clean test`
* **Run Single Test Class:** `mvn test -Dtest=TournamentServiceTest`
* **Run Single Test Method:** `mvn test -Dtest=TournamentServiceTest#givenValidTournament_create_shouldPersistAndReturnDto`
* **Run Application Locally:** `mvn spring-boot:run`
* **Package JAR:** `mvn clean package -DskipTests`

### 3.3 Frontend (`frontend/`)
* **Install Dependencies:** `npm install`
* **Start Dev Server (Vite):** `npm run dev`
* **Type-check:** `npx tsc --noEmit`
* **Lint:** `npm run lint`
* **Production Build:** `npm run build`

### 3.4 AI Service (`ai-service/`)
* **Install Dependencies:** `pip install -e .` (or `poetry install` / `pip install -r requirements.txt`)
* **Run Tests:** `pytest`
* **Start Service Locally:** `uvicorn app.main:app --reload --port 8000`

---

## 4. Technical Contracts & Data Storage Rules (ADR-0004)

### 4.1 Relational Schema & Flyway
* **Single Source of Truth:** `docs/contracts/data-schema.md`.
* Every SQL migration must reside in `core/src/main/resources/db/migration/` (`V1__initial_schema.sql`, `V2__...`).
* **Primary Keys:** Exclusively application-generated or database-generated `UUID` (`id UUID PRIMARY KEY DEFAULT uuid_generate_v4()`). **NEVER use auto-incrementing `Long` or `Integer` IDs for public or domain entities** (prevents ID enumeration attacks, BR-15).
* **Timestamps:** Exclusively `TIMESTAMPTZ` in UTC (mapped to `java.time.Instant`). Never store local time without offset.
* **Enumerations:** Represented as `VARCHAR(32)` or `TEXT` with SQL `CHECK` constraints (e.g. `discipline IN ('CS2', 'BRAWL_STARS', 'CHESS')`). Do NOT use Postgres native enum types (ADR-0004).
* **Optimistic Locking:** Aggregate root entities (`TournamentEntity`, `MatchEntity`, `TeamEntity`) must have `@Version private Long version;` mapped to `version BIGINT NOT NULL DEFAULT 0` (NFR-05).
* **Roster Snapshots:** When rosters lock (US-14), freeze player details into `registration_roster_snapshots`. Do NOT query mutable player profile fields during tournament execution.

### 4.2 Module Boundaries in Backend (`core/`)
As established in ADR-0003, the backend is a package-by-feature modular monolith:
1. `identity` (Core Platform & Auth)
2. `teams` (Core Platform & Auth)
3. `tournaments` (Tournament Engine)
4. `bracket` (Tournament Engine)
5. `matches` (Game Integrations & Match Flow)
6. `integrations` (Game Integrations & Match Flow)
7. `disputes` (AI & Automation)
8. `common` (Cross-cutting infrastructure)

**STRICT MODULARITY RULE:**  
Modules must interact **ONLY** through public service interfaces or Spring Application Events (`ApplicationEventPublisher`).  
*Forbidden:* Directly injecting a repository from another module (e.g. `matches` injecting `TeamRepository` or `TournamentRepository`). Always use the corresponding service interface (e.g. `TeamQueryService`, `TournamentQueryService`).

---

## 5. API & Error Handling Contracts (ADR-0003, ADR-0005)

### 5.1 REST API Conventions
* **Base Path:** `/api/v1`
* **Date format:** ISO-8601 UTC strings (`YYYY-MM-DDTHH:mm:ss.sssZ`).
* **Endpoint Catalog:** Strictly adhere to `docs/contracts/api-contracts.md`. Do not invent custom paths or modify request/response shapes.

### 5.2 Realtime WebSocket Messaging (STOMP over SockJS)
* **Handshake Endpoint:** `/ws`
* **Broadcast Topics:**
  * Match Room live state: `/topic/matches/{matchId}/room`
  * Bracket tree updates: `/topic/tournaments/{tournamentId}/bracket`
  * Organizer dispute count: `/topic/tournaments/{tournamentId}/dashboard`
* **Client Action Destinations:**
  * Captain ready check: `/app/matches/{matchId}/ready`
  * Map veto ban: `/app/matches/{matchId}/ban`
* **Server-side Timers:** Match timers (`ready_deadline_at`, `ban_deadline_at`) live in PostgreSQL and are evaluated server-side. Do NOT rely on client-reported timer completions (ADR-0005).

### 5.3 Unified Error Format (RFC 7807 Problem Details)
All error responses from Spring Boot must adhere to `docs/contracts/error-format.md`:
```json
{
  "type": "https://tournado.ua/errors/{code}",
  "title": "Short problem summary",
  "status": 409,
  "detail": "Localized human-readable explanation of what went wrong",
  "instance": "/api/v1/teams",
  "code": "TEAM_NAME_ALREADY_EXISTS",
  "timestamp": "2026-10-28T14:36:22.091Z",
  "invalidParams": [
    {
      "name": "name",
      "reason": "Team with this name already exists in CS2"
    }
  ]
}
```
* **Application Error Codes:** Must be selected from the catalog in `docs/contracts/error-format.md` (`AUTH_UNAUTHORIZED`, `ROSTER_FULL`, `CHECKIN_NOT_OPEN`, `MAP_ALREADY_BANNED`, etc.).

---

## 6. Java & Spring Testing Standards (Mandatory)

Whenever generating or refactoring Java/Spring tests, the AI assistant **MUST** follow these non-negotiable rules:

1. **Naming Convention:** Strictly follow `givenX_y_shouldZ` where `y` is the action or method under test.  
   **NEVER use the word `when` in `y`** (e.g. `givenValidPayload_create_shouldReturn201AndPersist`, NOT `whenCreate`).
2. **Assertions & Full Object Comparison:**
   * Always use **AssertJ** (`assertThat(...)`).
   * **STRICTLY DO NOT** use JUnit assertions (`assertEquals`, `assertTrue`) or Hamcrest (`assertThat(x, is(y))`).
   * **Full Object Comparison:** Strictly **DO NOT** assert objects or entities field-by-field (`assertThat(x.getName())... assertThat(x.getId())...`). Compare against expected models/DTOs defined in `TestResources`:
     ```java
     assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
     ```
     *(Use `.ignoringFields(...)` only when strictly necessary for dynamic fields like random UUIDs or clock timestamps).*
3. **Visual Formatting:**
   * Logical test phases (Setup, Stubbing, Execution, Assertions) MUST be separated by a **single blank line**.
   * **NO phase comments:** Strictly do NOT write comment headers (`// Setup`, `// Stubbing`, `// Execution`, `// Assertions` are strictly forbidden).
   * No redundant blank lines inside test methods.
4. **Static Star Imports:** Always use static wildcard imports:
   * `import static org.assertj.core.api.Assertions.*;`
   * `import static org.mockito.Mockito.*;`
   * `import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;`
   * `import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;`
   * `import static com.tournado.core.<module>.<Module>TestResources.*;`
5. **No Redundant Verifications:** Do NOT add `verify(...)` calls on mock interactions that were merely stubbed with `when(...).thenReturn(...)`. Only verify side effects that are the genuine target of verification.
6. **Feature & MockMvc Tests:**
   * Validate HTTP status and headers directly via MockMvc: `.andExpect(status().isCreated())`.
   * Strictly **DO NOT** use `andExpect(jsonPath("$.field").value(...))` chains for field-by-field payload assertion.
   * Deserialize the response body into typed DTOs and assert via AssertJ recursive comparison.

---

## 7. Frontend Guidelines (`frontend/`)

* **Framework:** React 19 + TypeScript (Strict Mode enabled).
* **Styling:** Tailwind CSS with utility classes; follow accessible UI/UX patterns.
* **Server State:** Use `@tanstack/react-query` for API fetching, caching, and mutations.
* **Client-side Error Handling:** Global query/mutation error interceptor that parses RFC 7807 `ProblemDetail` payloads and displays the `detail` text in toast notifications.
* **Form Validation:** Zod schemas paired with React Hook Form. Schema validation must match backend DDL constraints (string lengths, required formats).
* **No Business Logic Duplication:** The UI provides responsive validation for UX, but never assumes business rule decisions (bracket advancement, tie breaking, timeout forfeits are strictly backend-authoritative).

---

## 8. AI Service Guidelines (`ai-service/`)

* **Framework:** Python 3.12 + FastAPI + Pydantic v2.
* **Primary Endpoint:** `POST /api/v1/ai/analyze-dispute` (`multipart/form-data`).
* **Mock Provider Mode:** Under local development or when `AI_PROVIDER=mock`, the service must return deterministic mock responses matching `docs/contracts/api-contracts.md` without requiring paid OpenAI/Gemini API keys.
* **Graceful Degradation:** If image OCR fails or confidence is low, return `status: "UNRECOGNIZED"` with HTTP 200 rather than raising 500 exceptions, allowing the human organizer to arbitrate manually.

---

## 9. Strict Engineering Prohibitions (The 10 Prohibitions for AI)

AI assistants operating in this repository must **NEVER**:

1. ❌ **Invent uncontracted database columns or fields:** Never add a database column, entity field, or DTO property without verifying `docs/contracts/data-schema.md` and `docs/contracts/api-contracts.md`.
2. ❌ **Invent custom error JSON responses:** Never return `{"error": "something failed"}`. Always use the standardized RFC 7807 `ProblemDetail` model with registered codes from `docs/contracts/error-format.md`.
3. ❌ **Violate modular monolith encapsulation:** Never inject another module's repository directly. Use public service interfaces or Spring Application Events.
4. ❌ **Use sequential IDs:** Never introduce `BIGSERIAL`, `AUTO_INCREMENT`, or sequential numeric identifiers for domain entities. Use `UUID`.
5. ❌ **Trust client clock or client timers:** Never let the client dictate deadlines. All match timers (`ready_deadline_at`, `ban_deadline_at`) are computed and validated on the backend in UTC `Instant`.
6. ❌ **Write JUnit assertions or field-by-field asserts in Java tests:** Always use AssertJ recursive comparison against fixtures in `TestResources`.
7. ❌ **Place phase comments in tests:** Never write `// Given`, `// When`, `// Then`, `// Setup`, `// Assertions`. Separate phases with a single blank line.
8. ❌ **Introduce Redis or complex external brokers in MVP:** Stick to PostgreSQL 16 persistence and Spring STOMP in-memory WebSocket broker as decided in ADR-0004 and ADR-0005.
9. ❌ **Place business logic in UI:** Frontend is an interactive presentation layer; all tournament progression, roster locks, and tie-breaking must execute in Spring Boot transactions.
10. ❌ **Silently change contracts:** If an implementation requires a contract change, update `docs/contracts/` first with clear rationale before modifying code.

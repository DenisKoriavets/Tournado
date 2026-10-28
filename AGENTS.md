# Tournado — Universal AI Agents Guide (AGENTS.md)

> **Repository:** `DenisKoriavets/Tournado`  
> **Course:** Generative AI in Software Development — Practical 4  
> **Single Source of Truth:** `docs/contracts/` (`data-schema.md`, `api-contracts.md`, `error-format.md`), `docs/adr/`, `docs/analysis/requirements-digest.md`, `docs/requirements.md`

This document defines universal rules, architectural invariants, and testing standards for all AI coding agents working in this repository.

---

## 1. System Architecture & Tech Stack

* **Monorepo Structure (ADR-0001):**
  * `core/`: Java 21 + Spring Boot 3.3+ modular monolith (Maven).
  * `frontend/`: React 19 + TypeScript + Vite + Tailwind CSS.
  * `ai-service/`: Python 3.12 + FastAPI (screenshot OCR & dispute arbitration).
  * `docs/`: Technical contracts, ADRs, requirements digest, user stories.
  * `prompts/`: Coursework engineering prompts (4.1..4.4).
* **Database & State (ADR-0004, ADR-0005):**
  * PostgreSQL 16+ as Single Source of Truth; Flyway migrations (`core/src/main/resources/db/migration/`).
  * Server-side timers in PostgreSQL (`TIMESTAMPTZ` in UTC); no Redis in MVP.
* **Orchestration (ADR-0009):**
  * Local run: `docker compose up --build` (starts db, mail, core, frontend, ai-service).

---

## 2. Essential Commands

| Scope | Action | Command |
| :--- | :--- | :--- |
| **Full Stack** | Start all services | `docker compose up --build` |
| **Full Stack** | Stop all services | `docker compose down -v` |
| **Backend Core** | Run all tests | `mvn clean test` |
| **Backend Core** | Run single test | `mvn test -Dtest=TestClassName#methodName` |
| **Backend Core** | Start app locally | `mvn spring-boot:run` |
| **Frontend** | Start dev server | `npm run dev` |
| **Frontend** | Typecheck & Build | `npx tsc --noEmit && npm run build` |
| **AI Service** | Run tests | `pytest` |
| **AI Service** | Start service | `uvicorn app.main:app --reload --port 8000` |

---

## 3. Strict Architectural & Coding Invariants

1. **Contracts are Authoritative:** Never create database columns, DTO fields, or endpoints without checking `docs/contracts/`.
2. **Identifiers:** Use `UUID` (`id UUID PRIMARY KEY DEFAULT uuid_generate_v4()`) for all public and domain entities. Never use numeric auto-incrementing IDs.
3. **Timestamps & Deadlines:** Strictly `TIMESTAMPTZ` in UTC (`java.time.Instant`). Match deadlines (`ready_deadline_at`, `ban_deadline_at`) are calculated and verified on the server. Never trust client clocks.
4. **Optimistic Locking:** Entities `tournaments`, `matches`, `teams` must have `@Version private Long version;` (mapped to `version BIGINT NOT NULL DEFAULT 0`).
5. **Backend Modularity (ADR-0003):** Package-by-feature in `core/` (`identity`, `teams`, `tournaments`, `bracket`, `matches`, `integrations`, `disputes`, `common`). Modules communicate **ONLY** via public service interfaces or Spring Application Events, never via direct repository injection.
6. **Error Format (RFC 7807):** All API errors must return standardized `ProblemDetail` JSON with registered error codes from `docs/contracts/error-format.md`.
7. **Realtime WebSocket (ADR-0005):** STOMP over SockJS at `/ws`. Topics `/topic/...`, actions `/app/...`.
8. **Roster Snapshots:** Freeze player identifiers in `registration_roster_snapshots` on Roster Lock (US-14). Never query mutable user profile data during active tournament rounds.

---

## 4. Mandatory Java / Spring Testing Standards

All tests in `core/src/test/` must strictly comply with:

* **Naming:** `givenX_y_shouldZ` where `y` is the method/action under test. Never use `when` in `y`.
* **Assertions:** Exclusively AssertJ (`assertThat(...)`). JUnit assertions (`assertEquals`, `assertTrue`) are forbidden.
* **Full Object Comparison:** Compare whole DTOs/models against fixtures in `TestResources`:
  ```java
  assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
  ```
  *(Never assert objects field-by-field; use `.ignoringFields(...)` only for random UUIDs or clock instants).*
* **Visual Formatting:** Separate logical test phases with a **single blank line**. Do NOT write phase comments (`// Given`, `// When`, `// Then`, `// Setup`, `// Assertions`).
* **Static Star Imports:**
  ```java
  import static org.assertj.core.api.Assertions.*;
  import static org.mockito.Mockito.*;
  import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
  import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
  ```
* **No Redundant Verify:** Do not call `verify(...)` on interactions merely stubbed with `when(...).thenReturn(...)`.

---

## 5. Strict Engineering Prohibitions for AI

1. ❌ DO NOT invent database columns, tables, or DTO properties without checking `docs/contracts/data-schema.md` and `docs/contracts/api-contracts.md`.
2. ❌ DO NOT return custom error JSON payloads like `{"error": "msg"}`. Always use RFC 7807 `ProblemDetail` from `docs/contracts/error-format.md`.
3. ❌ DO NOT inject another module's repository directly. Use public service interfaces or Spring events.
4. ❌ DO NOT use sequential or auto-incrementing integer IDs. Always use `UUID`.
5. ❌ DO NOT trust client-side timers or timestamps. Deadlines are evaluated on the backend in UTC.
6. ❌ DO NOT write JUnit assertions or field-by-field assertions in Java tests. Use AssertJ recursive comparison.
7. ❌ DO NOT put phase comment headers in test methods.
8. ❌ DO NOT add Redis, Kafka, or heavy message brokers for MVP. Rely on PostgreSQL 16 and Spring STOMP.
9. ❌ DO NOT place business logic or state transitions in the frontend. Core backend is the single source of truth.
10. ❌ DO NOT silently change contracts. Update `docs/contracts/` first if requirements evolve.

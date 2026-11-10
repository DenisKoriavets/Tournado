# PR #5 AI Code Review & Security Audit Report

> **Target Branch:** `5_vertical_slice_tournaments`  
> **Review Scope:** Vertical Slice 1 — Tournament Creation & Retrieval (`US-07`..`US-11`)  
> **Review Date:** 2026-11-10  
> **Reviewers:** Senior AI Code Reviewer & Lead Security Auditor  
> **Traceability:** ADR-0001, ADR-0003, ADR-0004, `docs/contracts/` (`api-contracts.md`, `data-schema.md`, `error-format.md`), `CLAUDE.md`

---

## 1. Executive Summary & Verdict

### Final Verdict: **APPROVED WITH COMMENTS (READY TO SHIP)**

The implementation for **Vertical Slice 1 (Tournament Creation & Retrieval, US-07..US-11)** is exemplary. It establishes clean architectural patterns, enforces database and application-level invariants, integrates unified RFC 7807 Problem Details error handling, and delivers a polished, responsive React 19 / TypeScript UI with client-side schedule timeline validation and graceful offline fallbacks.

All automated test suites pass cleanly:
- **Backend Core:** `15 / 15` tests pass (`mvn test -DargLine="-Dnet.bytebuddy.experimental=true"`).
- **Frontend Web:** `21 / 21` tests pass (`npm test` via Vitest).
- **Frontend Production Build:** Clean compilation without TypeScript errors (`npm run build`).

A few minor spec alignment and test polish recommendations (detailed below) have been cataloged with confidence scores, none of which block merging.

---

## 2. Review Dimensions Scorecard

| Dimension | Rating | Summary |
| :--- | :---: | :--- |
| **1. Architecture & Modular Monolith** | **9.8 / 10** | Strict encapsulation in `core/.../tournaments/`. Aggregate root `TournamentEntity` uses UUID PK and `@Version Long version`. Decoupled `TournamentQueryService` interface provided for cross-module queries. |
| **2. Technical Contract Fidelity** | **9.6 / 10** | DTOs mirror `api-contracts.md`; JPA entity mappings strictly adhere to `data-schema.md`; `GlobalExceptionHandler` strictly produces RFC 7807 Problem Details; `V2__seed_system_user.sql` maintains DB referential integrity. |
| **3. Business Rules Verification** | **9.5 / 10** | Strict schedule order (`deadline < checkIn < start`) and slot limit (`8, 16, 32`) enforced at service and DDL levels; automatic discipline team sizing (5v5, 3v3, 1v1) validated. |
| **4. Testing Quality & Standards** | **9.2 / 10** | Mandatory `givenX_y_shouldZ` naming strictly followed; zero phase comments (`// Given` etc.); AssertJ assertions used throughout. Minor field-by-field assertions in service tests noted for recursive comparison cleanup. |
| **5. Frontend Implementation Quality** | **9.7 / 10** | React 19 + TypeScript + Tailwind CSS; full client-side validation mirroring backend invariants; interactive schedule indicator; public link copying with UX feedback; zero TS build errors. |
| **6. Security & Guardrails** | **9.9 / 10** | Non-sequential UUIDs (no ID enumeration); SQL injection immune (JPA / parameterized queries); RFC 7807 exception handler masks internal server details; optimistic locking prevents race conditions. |

---

## 3. Detailed Findings & Invariants Inspection

### 3.1 Architecture & Modular Monolith Encapsulation (ADR-0001, ADR-0003, CLAUDE.md)
- **Package Encapsulation:** All tournament business logic, entities, and repositories reside strictly inside `com.tournado.core.tournaments`.
- **Repository Isolation:** `TournamentRepository` is package-accessible and only injected by `TournamentService`. No other module's repositories are referenced.
- **Cross-Module Seam:** The public interface `com.tournado.core.tournaments.service.TournamentQueryService` is provided and implemented by `TournamentService`, allowing future modules (`bracket`, `matches`, `teams`) to query tournament metadata cleanly without repository leakage.
- **Aggregate Root Concurrency:** `TournamentEntity` declares `@Version @Column(name = "version", nullable = false) private Long version = 0L;` mapped to `version BIGINT NOT NULL DEFAULT 0` in DDL (ADR-0004, NFR-05).
- **UUID Primary Keys:** Both `TournamentEntity` and `TournamentMapEntity` use application/DB-generated `UUID` identifiers (`@Id private UUID id`), eliminating ID enumeration vulnerabilities (BR-15).

### 3.2 Technical Contracts & Database Integrity (`docs/contracts/`)
- **API Contracts (`docs/contracts/api-contracts.md`):**
  - `POST /api/v1/tournaments`: Accepts `CreateTournamentRequest` (`name`, `description`, `discipline`, `slotLimit`, `registrationDeadlineAt`, `checkInOpensAt`, `startTimeAt`, `cs2ServerConnect`) and returns HTTP 201 with `TournamentResponse`.
  - `GET /api/v1/tournaments/{id}`: Returns HTTP 200 with full `TournamentResponse`, or HTTP 404 with RFC 7807 `TOURNAMENT_NOT_FOUND`.
  - `GET /api/v1/tournaments`: Returns HTTP 200 with `List<TournamentSummaryResponse>`.
  - `publicSlug`: Properly populated from `entity.getId().toString()`.
- **Database Schema (`docs/contracts/data-schema.md`):**
  - Tables `tournaments` and `tournament_map_pool` match the schema exactly:
    - Primary keys: `UUID`
    - Timestamps: UTC `Instant` mapped to `TIMESTAMPTZ`
    - Constraints: `slot_limit IN (8, 16, 32)`, `discipline IN ('CS2', 'BRAWL_STARS', 'CHESS')`, `registration_deadline_at < check_in_opens_at AND check_in_opens_at < start_time_at`.
- **Foreign Key Seeding (`V2__seed_system_user.sql`):**
  - The migration script inserts a known system organizer (`00000000-0000-0000-0000-000000000001`, `system.organizer@tournado.kma.ua`) with `ON CONFLICT (id) DO NOTHING`.
  - Satisfies `users.chk_users_at_least_one_auth` and satisfies the foreign key `tournaments.created_by_user_id REFERENCES users(id) ON DELETE RESTRICT`.
- **Unified Error Handling (`docs/contracts/error-format.md`):**
  - `GlobalExceptionHandler` produces standard `application/problem+json` with:
    - `type`: `https://tournado.ua/errors/{code}`
    - `title`, `status`, `detail`, `instance`
    - `code`: machine-readable enum (`INVALID_SCHEDULE_ORDER`, `INVALID_SLOT_LIMIT`, `TOURNAMENT_NOT_FOUND`, `VALIDATION_FAILED`, `INTERNAL_SERVER_ERROR`)
    - `timestamp`: UTC ISO-8601 `Instant`
    - `invalidParams`: array of `{ name, reason }` for bean validation constraint failures.

### 3.3 Business Rules Verification (BR-xx)
- **BR-11 (Discipline & Team Size):** Team sizes are automatically bound by discipline (`CS2 -> 5`, `BRAWL_STARS -> 3`, `CHESS -> 1`) and cannot be manipulated by the organizer. Verified in domain enum and UI metadata.
- **BR-13 (Format & Slot Limit):** Format is locked to `SINGLE_ELIMINATION`. Slot limits are strictly validated against `{8, 16, 32}` via `validateSlotLimit()` and throw `InvalidSlotLimitException` (HTTP 422).
- **BR-14 (Schedule Chronology):** Schedule strictly requires `registrationDeadlineAt < checkInOpensAt < startTimeAt`. Tested and enforced via `validateScheduleOrder()` throwing `InvalidScheduleOrderException` (HTTP 422).
- **BR-15 (Unique Public Link):** Every created tournament generates an immutable public URL using its UUID slug, viewable without authentication.

### 3.4 Frontend Implementation Quality
- **Technology Stack:** Built with React 19, TypeScript (Strict Mode), Vite, and Tailwind CSS.
- **Components:**
  - `TournamentForm`: Multi-step responsive modal with discipline selection, slot chips, live schedule validation preview, recommended schedule generator ("⚡ Заповнити рекомендованим розкладом"), and inline RFC 7807 error banner.
  - `TournamentCard`: Clean dark-mode cards with discipline badge, team size chip, occupied slots progress bar, and schedule overview.
  - `TournamentList`: Discipline tabs with live counter badges, search filter by name/description, loading skeletons, and error retry state.
  - `TournamentDetailsModal`: Detailed view displaying full schedule breakdown, copyable public tournament link (US-11) with clipboard animation, official map pool badges, and CS2 server connect command.
- **Verification:** All 21 Vitest tests pass; `tsc -b` compiles without errors.

---

## 4. Confidence-Scored Findings

Following the two-axis code review methodology (Standards/Smells + Spec Faithfulness):

### 4.1 Specification Faithfulness: Chess.com Map Pool Representation
* **Observation:** In `core/.../tournaments/domain/Discipline.java` (line 8):
  ```java
  CHESS(1, List.of("Blitz 3+2", "Rapid 10+0"))
  ```
  When a Chess tournament is created, the backend saves `"Blitz 3+2"` and `"Rapid 10+0"` into `tournament_map_pool`.
  However, per `docs/requirements.md` US-08 AC4 (*"Для Chess.com пул карт не застосовується: розділ карт не відображається"*) and `docs/analysis/requirements-digest.md` BR-12 (*"Chess.com tournaments have no map pool"*), Chess tournaments should not have a map pool.
  In the frontend, `DISCIPLINE_CONFIG.CHESS.defaultMaps` is correctly set to `[]` (empty array).
* **Impact:** Low/cosmetic. During match flow, Chess skips the veto ban phase anyway (US-25 Scenario 4). However, having records in `tournament_map_pool` for Chess creates a slight data discrepancy with the spec.
* **Suggested Fix:** In `Discipline.java`, set `CHESS(1, List.of())` or have `TournamentMapPoolProvider` return an empty list for Chess.
* **Confidence Score:** **90% (Probable / Spec discrepancy)**

### 4.2 Standards & Robustness: Backend Validation of Past Timestamps (`SCHEDULE_TIME_IN_PAST`)
* **Observation:** `TournamentService.validateScheduleOrder()` checks:
  ```java
  !registrationDeadlineAt.isBefore(checkInOpensAt) || !checkInOpensAt.isBefore(startTimeAt)
  ```
  It does not check whether `registrationDeadlineAt.isAfter(Instant.now())`.
  `docs/contracts/error-format.md` explicitly defines error code `SCHEDULE_TIME_IN_PAST` (*"Specified start/check-in/deadline time must be strictly in the future"*).
* **Impact:** Client-side validation in `TournamentForm` already prevents submitting past dates for UX. However, for defense-in-depth against direct API requests, the backend service should reject past deadlines with `SCHEDULE_TIME_IN_PAST`.
* **Suggested Fix:** Add a check `if (registrationDeadlineAt.isBefore(Instant.now())) throw new DomainException(HttpStatus.UNPROCESSABLE_ENTITY, "SCHEDULE_TIME_IN_PAST", "Registration deadline must be strictly in the future.");` or use `@Future` on `CreateTournamentRequest`.
* **Confidence Score:** **85% (Probable / Defense-in-Depth)**

### 4.3 Testing Standards: AssertJ Recursive Comparison in Service Tests (CLAUDE.md Section 6 Rule 2)
* **Observation:** In `TournamentServiceTest.java`:
  ```java
  // In givenExistingTournamentId_getTournamentById_shouldReturnTournamentResponse:
  assertThat(response.id()).isEqualTo(TOURNAMENT_ID);
  assertThat(response.name()).isEqualTo("KMA CS2 Autumn Cup 2026");
  assertThat(response.discipline()).isEqualTo(Discipline.CS2);
  assertThat(response.teamSize()).isEqualTo(5);
  assertThat(response.slotLimit()).isEqualTo(16);
  ```
  CLAUDE.md Section 6 Rule 2 mandates:
  *"Strictly DO NOT assert objects or entities field-by-field... Compare against expected models/DTOs defined in TestResources: `assertThat(actual).usingRecursiveComparison().isEqualTo(expected);`"*
  Notice that `TournamentTestResources.buildExpectedTournamentResponse()` already exists and is ready for recursive comparison.
* **Impact:** Minor code polish / testing standards alignment.
* **Suggested Fix:** Replace field-by-field assertion blocks in `TournamentServiceTest` with `assertThat(actual).usingRecursiveComparison().isEqualTo(expected);`.
* **Confidence Score:** **85% (Probable / Standards compliance)**

### 4.4 Testing Standards: Redundant Mockito Verification in MockMvc Test (CLAUDE.md Section 6 Rule 5)
* **Observation:** In `TournamentControllerTest.java` (line 57):
  ```java
  verify(tournamentService).createTournament(any(CreateTournamentRequest.class));
  ```
  CLAUDE.md Section 6 Rule 5 states:
  *"No Redundant Verifications: Do NOT add `verify(...)` calls on mock interactions that were merely stubbed with `when(...).thenReturn(...)`."*
* **Suggested Fix:** Remove the redundant `verify(...)` call on the stubbed `tournamentService`.
* **Confidence Score:** **75% (Minor / Standards compliance)**

---

## 5. Security & Invariant Verification Matrix

| Invariant / Guardrail | Verification Method | Status | Notes |
| :--- | :--- | :---: | :--- |
| **Non-sequential IDs (BR-15)** | Source code inspection (`TournamentEntity`, `TournamentMapEntity`) | **VERIFIED** | UUIDs generated at persist time; prevents URL enumerations. |
| **Optimistic Locking (NFR-05)** | Entity inspect & `@Version` check | **VERIFIED** | `TournamentEntity.version` mapped to `BIGINT NOT NULL DEFAULT 0`. |
| **SQL Injection Immunization** | Hibernate / Spring Data inspection | **VERIFIED** | 100% parameterized queries via Spring Data JPA. |
| **Referential Integrity** | Flyway migration `V1` & `V2` execution | **VERIFIED** | System user seeded with UUID `00000000-0000-0000-0000-000000000001` to satisfy FK. |
| **RFC 7807 Error Obfuscation** | `GlobalExceptionHandler.handleGeneralException` | **VERIFIED** | Unknown 500 exceptions masked; no internal stack trace leakage. |
| **Slot Limit Invariant** | Unit tests & DDL constraints | **VERIFIED** | Tested with invalid slot `24`; correctly rejects with HTTP 422 `INVALID_SLOT_LIMIT`. |
| **Schedule Order Invariant** | Unit tests & DDL constraints | **VERIFIED** | Tested with reversed timestamps; correctly rejects with HTTP 422 `INVALID_SCHEDULE_ORDER`. |
| **Client-Side Validation Mirror** | React component tests (`TournamentForm.test.tsx`) | **VERIFIED** | React Hook validation mirrors backend DDL constraints. |

---

## 6. Conclusion & Recommendation

The PR for **Vertical Slice 1 (`5_vertical_slice_tournaments`)** is in extraordinary shape. The code is modular, tests are exhaustive, and the UI integration delivers immediate value.

The identified findings are non-blocking quality and spec alignment improvements. The branch is **APPROVED** to be merged into `main`. The team can either apply the small polish items in a quick follow-up commit or address them during the next vertical slice.

# Testing Strategy — Tournado

> **Status:** active (practice 6) · **Applies to:** `core/`, `frontend/`, `ai-service/`
> **Source of truth for test cases:** Gherkin scenarios in [`docs/requirements.md` §5](requirements.md#5-критерії-приймання-acceptance-criteria) (US-01..US-36) and error codes in [`docs/contracts/error-format.md`](contracts/error-format.md).
> **Prompt used to produce this document:** [`prompts/6.1_testing_strategy.md`](../prompts/6.1_testing_strategy.md)

## 1. Goal and the main rule

Tests prove that the product does what the **requirements** say, not what the code happens to do.
Every automated test starts from a Gherkin scenario or a checklist item of a user story. It never starts from reading the implementation. A test that turns green on buggy code is worse than no test.

## 2. Scope

| Scope In: automated (Bucket 1) | Scope Out: manual smoke checklist (Bucket 2) |
| :--- | :--- |
| Business rules in `core`: registration, slot limits, roster completeness and lock, invites, score entry, auto-advance, disputes | Visual layout, colours, animations, responsiveness on a phone |
| REST API contract: status codes **400/401/403/404/409/422** and the `code` field of the RFC 7807 body | Real Steam OpenID login and real Mailpit magic-link delivery |
| Persistence: Flyway migrations, DB constraints, transactions on a real PostgreSQL 16 | Live Match Room behaviour with two browsers, including timers ticking |
| Frontend behaviour: what a component renders for each state and API error, which buttons are enabled or disabled, which calls it makes | Real Groq AI responses on real screenshots (the optional `pytest -m live` is run by hand) |
| ai-service: parsing, mock-provider determinism, the `UNRECOGNIZED` fallback, endpoint validation | Load and performance; cross-browser checks |

The manual checklist lives in [`docs/testing-report.md`](testing-report.md) §2.

## 3. Test levels and tools

| Level | Where | Tools | What it is for |
| :--- | :--- | :--- | :--- |
| **API integration** (main level for stories) | `core/src/test/java/.../<module>/integration/*IntegrationTest.java` | JUnit 5, Spring Boot Test, MockMvc, AssertJ, **Testcontainers PostgreSQL 16** | A real HTTP request goes through the security filter chain, controller, service, JPA and Flyway-migrated PostgreSQL, and back as JSON. Nothing in the chain is mocked. |
| Web slice (`@WebMvcTest`) | `core/.../<module>/api/*ControllerTest.java` | MockMvc + `@MockBean` services | Request validation, error mapping, auth rules of a single controller |
| Unit | `core/.../<module>/service/*Test.java` | JUnit 5, Mockito, AssertJ | Branch-heavy logic in isolation (e.g. dispute rules, JWT, validators) |
| Component | `frontend/src/**/*.test.ts(x)` next to the file | Vitest, Testing Library, jsdom | UI states per Gherkin scenario. HTTP is replaced at the `src/api/*` boundary only. |
| Service | `ai-service/tests/test_*.py` | pytest, FastAPI `TestClient`, mock provider | Score and nickname extraction, contract of `/api/v1/ai/analyze-dispute` |

There are no browser E2E tests in the MVP. The end-to-end path is covered by the API level plus the manual smoke run. This trade-off was made deliberately to save time and tokens.

## 4. File organization and naming

- **One test class (file) per user story or per endpoint.** Never dump unrelated stories into one file. A story may share a file only with the scenarios of a story that is triggered by the **same endpoint**, for example score entry (US-27) and the auto-advance it causes (US-28). Each such scenario still gets its own test. Examples:
  - `TeamRegistrationIntegrationTest` (US-12)
  - `TeamJoinByInviteIntegrationTest` (US-05, plus US-14 sc.2/sc.3 on the same join endpoint)
  - `MatchScoreSubmissionIntegrationTest` (US-27/US-28)
  - `DisputeResolutionIntegrationTest` (US-34)
  - `TournamentMapPoolIntegrationTest` (US-08)
- **One Gherkin scenario = one test.** Java methods follow CLAUDE.md: `givenX_action_shouldZ` (never `when`), plus a traceable `@DisplayName("US-12 sc.2: ...")`. Checklist items that have no scenario are tagged `US-xx checklist: ...`.
- Java assertions use AssertJ only. Whole DTOs are compared with `usingRecursiveComparison()`, and errors are compared as `ApiError(status, code)`. Phase comments are not used.
- Frontend: `Component.test.tsx` sits next to `Component.tsx`. Test titles start with the story id: `it('US-27 sc.3: rejects a tied score', ...)`.

## 5. Test data and isolation

- **core integration:** a single `postgres:16-alpine` container starts once per test JVM (`support/IntegrationTestBase`). Flyway applies all migrations V1..V6, which also tests the migrations themselves.
  - **Before every test, all tables except `flyway_schema_history` are `TRUNCATE ... CASCADE`d.** Tests share no state, the order does not matter, and re-runs are idempotent.
  - "Given" data is inserted by `support/DbFixtures` with random UUIDs and unique names. "When" is always a real HTTP call.
  - Seed data from V2..V6 is never relied upon.
  - Users are authenticated with a real JWT signed by `JwtTokenService` (`Authorization: Bearer`).
- **core unit and web-slice tests:** in-memory fixtures from `<Module>TestResources`, no DB.
- **frontend:** no network. Each test builds its own props and mocks, and `vi.restoreAllMocks()` runs after each test.
- **ai-service:** `AI_PROVIDER=mock` and generated PNG fixtures (`tests/conftest.py`). Live Groq tests are opt-in (`-m live`).
- Rule: never mock the logic under test, and never use `sleep`. Time-dependent rules use deadlines relative to `Instant.now()` (past or future).

## 6. Quality gate (must be green before merging into `main`)

```bash
npm test                     # core (JUnit + integration) + frontend (Vitest) + ai-service (pytest), from the repo root
cd frontend && npm run lint && npx tsc --noEmit
cd core && ./mvnw checkstyle:check
```

Requirements: JDK 21+, Node 20+, Python 3.12+, and a running **Docker** engine for the core integration tests.

**AI-generated tests are accepted only after these checks:**
1. **Mutation check:** break one condition in the production code and confirm the test turns red.
2. **No self-testing mocks:** review the `git diff` to make sure no mock simply returns the value that the test then asserts.
3. **Run twice:** run the suite twice in a row and confirm nothing leaks between runs.

If a test exposes a real bug, the **code is fixed and the test is never weakened**.

## 7. Known risks to watch

| Risk | Current mitigation |
| :--- | :--- |
| Two captains take the last free slot at the same moment (count-then-insert in `RegistrationService`) | The unique `(tournament_id, team_id)` constraint protects against duplicates. A slot overflow under a true race is not covered yet; it is noted in the report as a post-MVP item. |
| Client-side timers drift from the server (Ready Check, bans) | Deadlines live in PostgreSQL (ADR-0005). The UI only displays them, and the manual checklist covers this. |
| Frontend types drift from Java DTOs | API clients are typed against `docs/contracts/api-contracts.md`, and component tests use contract-shaped fixtures. |

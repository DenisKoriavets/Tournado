---
name: generate-tests
description: Generate requirement-driven automated tests for one Tournado user story (e.g. "/generate-tests US-12"). Reads the Gherkin scenarios in docs/requirements.md and the rules in docs/testing-strategy.md, never the implementation, and writes API integration tests (core) or component tests (frontend).
argument-hint: US-xx [core|frontend]
---

# Skill: Generate Tests for a User Story

You are a Senior QA Automation Engineer on Tournado. Write automated tests for user story **$ARGUMENTS**.

## Strict rules
1. **Source of truth:**
   - Read the story in `docs/requirements.md` §5 "Критерії приймання" (find the heading `### US-xx`). Use its checklist and its Gherkin scenarios (`Дано / Коли / Тоді`).
   - Read the error codes in `docs/contracts/error-format.md` and the endpoint in `docs/contracts/api-contracts.md`.
   - Read the rules in `docs/testing-strategy.md`.
   - Use the implementation ONLY to find the endpoint path and DTO class names. NEVER derive expected behaviour from the code.
2. **One scenario = one test.** Map each Gherkin scenario to its own test.
   - Java: `givenX_action_shouldZ` + `@DisplayName("US-xx sc.N: <short scenario>")`.
   - Frontend: `it('US-xx sc.N: ...')`.
   - Checklist items without a scenario are named `US-xx checklist: ...`.
3. **Cover both paths:** the happy path AND every negative or boundary scenario (400/401/403/404/409/422, last free slot, duplicates, deadlines).
4. **Level:**
   - **core:** extend `com.tournado.core.support.IntegrationTestBase` (real HTTP via MockMvc + real PostgreSQL via Testcontainers).
     - Build the "Given" state with `DbFixtures`. The "When" step must be a real HTTP call.
     - Assert the HTTP status, the response DTO via `usingRecursiveComparison()`, errors as `new ApiError(status, "CODE")`, and the resulting DB state (row counts or columns).
   - **frontend:** Vitest + Testing Library.
     - Replace HTTP only at the `src/api/*` module boundary with `vi.spyOn`.
     - Assert what the user sees (text, enabled or disabled controls) and which API function was called with which arguments.
5. **No fakes:**
   - Do not mock the class under test.
   - Do not assert a value that a mock was told to return.
   - No `sleep()` or arbitrary timeouts.
   - No snapshot tests.
6. **Style (CLAUDE.md §6):**
   - AssertJ only, with static star imports.
   - Phases are separated by one blank line, with no `// Given`-style comments.
   - No field-by-field object assertions.
7. **File organization:** one file per story.
   - core: `core/src/test/java/com/tournado/core/<module>/integration/<Feature>IntegrationTest.java`
   - frontend: `<Component>.test.tsx` next to the component
   - Never append to another story's file.

## Execution steps
1. Read the sources listed in rule 1 and print a table: `Scenario | Test name | Expected status/code | Expected DB/UI effect`, plus the target file path. **Wait for human approval** before writing code.
2. Write the tests.
3. Run them:
   - `cd core && ./mvnw test -Dtest=<Class>`, or
   - `cd frontend && npx vitest run <file>`.
4. If a test fails because the application violates the requirement, **STOP and report the bug** (scenario, expected vs actual). Do NOT weaken the assertion and do NOT change the test to match the code.
5. **Mutation check:**
   - Temporarily break one guard condition in the production code (for example `>=` to `>`, remove a duplicate check, or 201 to 200).
   - Run the tests and confirm at least one turns red.
   - Revert with `git checkout -- <file>`.
   - Report which test caught which mutation.
6. Run the whole suite twice (`npm test` from the repo root) and report the totals.

# Testing Report — Practice 6

> **Branch:** `6_testing_and_frontend_mvp` · **Date:** 2026-11-26 · **Strategy:** [`docs/testing-strategy.md`](testing-strategy.md)
> **Prompts:** [`6.1`](../prompts/6.1_testing_strategy.md) (strategy) · [`6.2`](../prompts/6.2_test_generation.md) (generation) · [`6.3`](../prompts/6.3_qa_audit_and_debugging.md) (audit and debugging)

## 0. Summary

| Suite | Tests | Result | Command |
| :--- | :---: | :---: | :--- |
| core: unit, `@WebMvcTest`, and **API integration on PostgreSQL 16 (Testcontainers)** | 318 (44 new + 22 merged) | green | `npm test` (or `npm run test:core`) |
| frontend: Vitest + Testing Library | 114 (71 new) | green | `npm run test:frontend` |
| ai-service: pytest, mock provider | 35 | green | `npm run test:ai` |
| **Total** | **467** | **green** | **`npm test` from the repo root** |

> Initial Practice 6 work on `6_testing_and_frontend_mvp` started with 444 tests (296 core + 113 frontend + 35 ai-service). Merging `feat/bracket-engine` (check-in, bracket generation, standings, plus 22 Mockito/WebMvc tests by the Tournament Engine owner) brought core to **318**, and adding the modal-stacking regression test from browser QA brought frontend to **114** (**467 total**, all green). Our US-28 and US-34 integration tests close real matches, so they also exercise the bracket branch's `BracketMatchEventListener` together with `MatchResultService`. No double advancement was observed.

Additional gates:
- `cd frontend && npm run lint && npx tsc --noEmit && npm run build` is green. **Lint was red before this practice**; see Bug 2.
- Each suite was run at least twice in a row with no state leaking between runs. Core truncates every table before each test.

## 1. Stories and scenarios covered by automated tests

**Backend: API integration.** Each test sends a real HTTP request with a real JWT and runs through security, controller, service and JPA against a real PostgreSQL 16 container. Nothing is mocked.

| Story | Test class | Scenarios |
| :--- | :--- | :--- |
| **US-12** Team registration | `tournaments/integration/TeamRegistrationIntegrationTest` (10) | sc.1–sc.6. Checklist: missing Steam ID → 422 `MISSING_GAME_IDENTIFIER`, wrong discipline → 422, non-captain → 403, anonymous → 401. |
| **US-05** Join by invite (+ **US-14** sc.2/sc.3) | `teams/integration/TeamJoinByInviteIntegrationTest` (8) | sc.1–sc.5 (full roster, already a member, unknown/deactivated/expired token, roster locked after deadline). Roster still open before the deadline. Anonymous → 401. |
| **US-27** Score entry + **US-28** auto-advance | `matches/integration/MatchScoreSubmissionIntegrationTest` (17) | US-27: sc.1–sc.4 (invalid values `-1`, `"abc"`, `13.5`, missing; tie; chess). Checklist: second submission → 409; outsider or non-captain teammate → 403; not in progress → 409; unknown match → 404; anonymous → 401; checklist 5, the opponent's room exposes no score field. US-28: sc.1–sc.4 (winner moves to the correct slot of the next round; one report or a mismatch does not advance; final completes the tournament). |
| **US-34** Dispute resolution | `disputes/integration/DisputeResolutionIntegrationTest` (7) | sc.1 AI hint approved → winner advances; sc.2 manual 9:13 → higher score wins; sc.3 AI unrecognised → 422; sc.4 tie → 422. Checklist: missing scores → 400, captain instead of organizer → 403, second ruling → 409. |
| **US-08** Map pool | `tournaments/integration/TournamentMapPoolIntegrationTest` (2) | sc.1 CS2 gets the standard pool, stored in the DB; sc.2 Chess.com gets no pool. |

**Frontend: component tests.** HTTP is replaced only at the `src/api/*` boundary.

| Story | Test file | Scenarios |
| :--- | :--- | :--- |
| US-27 / US-28 | `ScoreEntryForm.test.tsx`, `utils/score.test.ts`, `MatchRoomModal.test.tsx` | sc.1–sc.4. Opponent's score is never shown. Mismatch → "Відкрити спір". Completed → winner. 409 handling. Non-captain sees no form. |
| US-30 | `OpenDisputeDialog.test.tsx`, `api/http.test.ts`, `MatchRoomModal.test.tsx` | sc.1–sc.4, more than 10 MB rejected, nginx 413, FormData upload, DISPUTED status in the room. |
| US-31 | `TournamentDisputesTab.test.tsx`, `TournamentDrawer.test.tsx` | sc.1–sc.3. Click opens the card. Counter visible before the tab is opened. Hidden from non-organizers. |
| US-32 / US-33 / US-34 | `DisputeCardModal.test.tsx` | US-32 sc.1–sc.3 and polling while PENDING. US-33 sc.1–sc.3. US-34 sc.1–sc.4, organizer-only, read-only after resolve. |
| US-05 | `JoinInviteModal.test.tsx`, `utils/pendingInvite.test.ts`, `App.test.tsx` | sc.1–sc.5 with Ukrainian messages per error code. The invite survives login. |
| US-11 | `utils/route.test.ts`, `App.test.tsx` | sc.2: anonymous deep link `/tournaments/{id}`. Unknown id shows "Турнір не знайдено". |
| US-08 | `TournamentDrawer.test.tsx` | Chess tournament shows no map pool. |

### "Break the code on purpose" (mutation check)

Each production change below was made temporarily, tests were run, and the file was restored.

| # | Mutation | Caught by (red) |
| :---: | :--- | :--- |
| 1 | `RegistrationService`: `activeCount >= slotLimit` → `>` | US-12 sc.2, sc.6 (expected 409, got 201) |
| 2 | `POST /register` returns 200 instead of 201 | US-12 sc.1, sc.5, sc.6 |
| 3 | `TeamInviteService`: duplicate-member guard removed | US-05 sc.3 (expected 409, got 500) |
| 4 | `MatchResultService`: winner written to the wrong bracket slot | US-28 sc.1 (`team2_id` of the final) |
| 5 | `utils/score.ts`: tie check removed | US-27 sc.3 (validator and form), US-34 sc.4 |
| 6 | Drawer shows "Спори" to any logged-in user | US-31 checklist 5 |
| 7 | "Затвердити підказку ШІ" ignores `canApproveAiSuggestion` | US-34 sc.3 / US-32 sc.2 |
| 8 | Screenshot 10 MB limit removed | US-30 checklist 4 |
| 9 | Bugs 6 and 7 temporarily reverted to the original code | US-12 discipline test (expected 400, got 422) and US-08 sc.2 (chess pool not empty) |

## 2. Manual smoke checklist (UI)

### 2.1 End-to-end API smoke on the running stack (done, 2026-11-26)

`docker compose up --build` was run with a fresh DB (`.env` copied from `.env.example`, `AI_PROVIDER=mock`).
- **Service health:** all 5 containers are up; Flyway applied V1–V6; core and ai-service health checks are `UP`.
- **Frontend through nginx:** `/` returns 200 and the deep link `/tournaments/{id}` returns 200 (SPA fallback); the nginx `/api` proxy returns 200.
- **Demo scenario:** run through nginx (`:3000`) exactly like the browser does, with the V6 accounts logging in through real magic links from Mailpit:

| Step | Result |
| :--- | :--- |
| Login `t.captain`, `ct.captain`, `dispute.organizer` via magic link | OK (`/auth/me` returns the display names) |
| Score `13.5` (Bug 1) | **400 VALIDATION_FAILED** |
| Captain 1 submits 13:9 → captain 2 submits 9:13 | `bothSubmitted=false`, then `bothSubmitted=true, scoresMatched=false` |
| Dispute with `test_screenshot_1.jpg` (multipart through nginx) | 201 `OPEN`, AI `PENDING`; a second dispute gets **409** |
| Organizer dispute list | `openDisputesCount = 1` |
| Dispute card after the async AI analysis | `SUCCESS 13:9`, confidence 0.985, all 10 nicknames found in the locked rosters; screenshot served as `image/jpeg` |
| Captain tries to resolve | **403 AUTH_FORBIDDEN** |
| Organizer approves the AI hint | 200 `RESOLVED`, match `COMPLETED`, winner placed in the final's team1 slot |
| New Chess.com tournament (Bug 7) | 201, `mapPool = []` |

This run consumed V6 semifinal 1. Reset the DB with `docker compose down -v && docker compose up` before the live demo.

### 2.2 UI checklist in the browser (verified 2026-11-27 against `docker compose up --build`)

The checklist was executed end-to-end in a live Chromium session on `http://localhost:3000` and Mailpit (`http://localhost:8025`) with a fresh DB and the V6 fixture "Dispute QA Arena — CS2".

| # | Check | Expected | Result |
| :---: | :--- | :--- | :---: |
| 1 | Open `http://localhost:3000` on desktop and at a 375 px width | Catalog renders, no horizontal scroll, header buttons reachable | ✅ *(re-verified after Bug 9 fix)* |
| 2 | Log in by email, then open the magic link from Mailpit in a new tab | The new tab is logged in; the name is shown in the header | ✅ |
| 3 | As a guest, open an invite link `/join/inv_…`, click "Увійти…", log in via Mailpit | After login the invite opens again with "Підтвердити вступ" (US-05) | ✅ *(re-verified after Bug 10 fix)* |
| 4 | Copy a tournament link in the drawer, open it in a private window | The same tournament opens for an anonymous visitor (US-11). A broken id shows "Турнір не знайдено". | ✅ |
| 5 | Open a Chess.com tournament → "Пул карт та правила" | No map pool, only the chess note (US-08) | ✅ |
| 6 | As `t.captain@test.tournado.ua`: menu "Матчі (QA)" → "Dispute QA — півфінал 1", enter `13.5` / `-1` / `5:5` | Errors under the fields, nothing sent (US-27 sc.2/sc.3) | ✅ |
| 7 | Same captain enters 13:9; `ct.captain@…` enters 9:13 in another browser | First captain sees "Очікується підтвердження суперником"; the second sees "Рахунки не збігаються" (US-27/28) | ✅ |
| 8 | Click "Відкрити спір": try a PDF, then upload `ai-service/resources/test_screenshot_1.jpg` | PDF is rejected with the formats message. JPG opens the dispute, and the room shows "Спір" (US-30). | ✅ |
| 9 | As `dispute.organizer@…`: open the tournament → "Спори" tab | Red counter 1 is visible before the tab is opened. The row is marked "Спір" (US-31). | ✅ |
| 10 | Open the dispute card | Screenshot, both captains' scores, «Підказка ШІ» with confidence %, and the nickname table are visible (US-32/33) | ✅ |
| 11 | Click "Затвердити підказку ШІ" and confirm | Success message; the row becomes "Опрацьовано" and the counter disappears (US-34, US-31 sc.2). *(Note: confirmation dialog opens below the scroll fold inside the modal.)* | ✅ |
| 12 | Press Escape and click outside dialogs; check text contrast on dark backgrounds | Dialogs close; text is readable | ✅ |

## 3. Real bugs found and fixed

**Bug 1 (backend, US-27 AC2): a fractional score was silently accepted.**
- *Found by:* the integration test `US-27 sc.2` with payload `{"team1Score": 13.5}`. It expected 400 but got **200**, and `13` was stored.
- *Cause:* Jackson's default `ACCEPT_FLOAT_AS_INT` truncates `13.5` to `Integer 13` before Bean Validation runs, so `@Min/@Max` never see the fraction.
- *Fix:* `spring.jackson.deserialization.accept-float-as-int: false` in `core/src/main/resources/application.yml`. It applies to every integer field of the API, including slot limits and dispute rulings. The test was kept unchanged.

**Bug 2 (frontend quality gate): `npm run lint` was red on `main`.**
- *Found by:* running the quality gate during the read-only frontend review. The error was `react-hooks/set-state-in-effect` in `MatchRoomModal.tsx:95`: the initial room load set state synchronously inside the effect, which can cause cascading renders.
- *Fix:* fetching (`fetchRoomBundle`) is separated from applying state, the effect sets state only after `await` and is guarded by a `cancelled` flag, and `isLoading` starts as `true`. The same pattern is used in all new effects (`App`, dispute card, dispute list).
- *Side improvement:* a failed background poll no longer replaces a loaded room with the error screen. This is covered by a test.

**Bug 3 (frontend, US-05 checklist 2): the invite was lost after login.**
- *Found by:* the AI review of the UI against the Gherkin checklist.
- *Symptom:* a guest who opened `/join/{token}` and logged in landed on `/` with no way back to the invite. Steam and magic-link logins both redirect to `/`, and the magic link opens in a new tab.
- *Fix:* the token is saved in `localStorage` with a 30-minute TTL right before the login modal opens (`utils/pendingInvite.ts`), and the invite reopens on load. Every storage access is in try/catch, so private mode degrades gracefully.

**Bug 4 (frontend, US-11): the public tournament link did not work.**
- *Symptom:* the drawer copied `/tournaments/{id}`, but the app only parsed `/join/...`, so opening a shared link showed the catalog with nothing selected.
- *Fix:* `utils/route.ts` parses the path, `App` opens the drawer on load and on `popstate` (anonymous users included), shows "Турнір не знайдено" on 404, and pushes or restores the URL when the drawer opens or closes. nginx already has the SPA fallback.

**Bug 5 (frontend, found by our own new test): the reason a file was rejected disappeared.**
- *Symptom:* in the new `OpenDisputeDialog`, choosing a PDF showed "Допустимі формати…", but pressing "Відкрити спір" replaced it with "Додайте скріншот…". This violated US-30 sc.3.
- *Found by:* `OpenDisputeDialog.test.tsx` (sc.3 and the size test were red on the first run).
- *Fix:* the rejection reason is kept in state. The component was fixed; the test was not changed.

**Bug 6 (backend, error contract): a discipline mismatch returned `422 VALIDATION_FAILED`.**
- *Found by:* the read-only QA audit (prompt 6.3).
- *Details:* `error-format.md` §3.6 defines `VALIDATION_FAILED` as **400**. Our own first US-12 test had **locked in the wrong pair**, because its expected status was taken from the implementation instead of the contract (see reflection item 8).
- *Fix:* `RegistrationDisciplineMismatchException` now returns 400. The test was rewritten from the contract and confirmed red on the old code.

**Bug 7 (backend, US-08 sc.2): Chess.com tournaments were created with a "map pool".**
- *Found by:* the QA audit.
- *Symptom:* `Discipline.CHESS` carried `Blitz 3+2 / Rapid 10+0`, which was stored as the tournament's map pool. US-08 says no map pool applies to Chess.com, and the contracts define none.
- *Why it went unnoticed:* our frontend guard (Bug 8 below) hid it from the UI.
- *Fix:* the chess pool is now empty. A new `TournamentMapPoolIntegrationTest` was confirmed red on the old code. Chess matches already skipped the ban phase, so nothing else depended on the pool.

**Bug 9 (frontend UI, checklist #1): mobile 375 px header overflow and brand overlap.**
- *Found by:* live browser inspection of `http://localhost:3000` at `375 × 812`.
- *Symptom:* on a 375 px viewport, the `"Створити турнір"` button extended to `x=326..440px` past the right edge and clipped to `"+ С"`. A tighter-padding pass also exposed `"Мої команди"` overlapping the last letter of `"TOURNADO"`.
- *Fix:* updated `frontend/src/App.tsx` header to compact mobile spacing (`px-2.5 sm:px-6 lg:px-8`), hidden `"Esports Platform"` subtitle on `<640px`, shorter `"Створити"` label on mobile, and tighter button padding. Re-verified in browser at `375 × 812`: right edge sits at `357.43px` (`<= 367.43px` container) with a `27px` gap after the brand wordmark.

**Bug 10 (frontend UI, US-05 / checklist #3): `AuthModal` opened behind `JoinInviteModal`.**
- *Found by:* live browser verification of `/join/inv_...` as an unauthenticated guest.
- *Symptom:* clicking `"Увійти для підтвердження вступу"` opened `"Вхід до Tournado"` (`AuthModal`) behind `"Запрошення до команди"` (`JoinInviteModal`), because both shared `z-50` and `AuthModal` appeared earlier in DOM order inside `App.tsx`. Pointer clicks on `"Отримати Magic Link"` failed with `ELEMENT_OCCLUDED`.
- *Fix:* raised `AuthModal` overlay stacking to `z-[70]` in `frontend/src/components/AuthModal.tsx`, moved `<AuthModal />` to the end of JSX in `frontend/src/App.tsx`, and added a stacking regression test in `frontend/src/App.test.tsx`. Re-verified in live browser with real pointer clicks and `POST /api/v1/auth/email/request-link` returning `200 OK`.

**Smaller fixes:**
- **Bug 8, US-08 UI guard:** a chess tournament displayed the backend's time-control list as a map pool. The guard is kept for legacy data created before Bug 7 was fixed.
- **Deployment:** nginx's 1 MB default body limit would have rejected dispute screenshots between 1 and 10 MB. Added `client_max_body_size 13m`, and the client maps `HTTP_413` to a clear message.
- **Race condition:** two quick clicks on tournaments could show the first tournament. Only the latest request now fills the drawer.
- **Node v25 test-runner compatibility (`frontend/src/test/setup.ts`):** running Vitest under Node v25.6.0 on Windows initially broke 7 tests because Node injected an incomplete `globalThis.localStorage` without `.clear()`. Added a `Storage.prototype` / `localStorage` polyfill in `setup.ts` (preserving `vi.spyOn` hooks).

### Known issues, not fixed in this iteration

| Finding | Why it is deferred |
| :--- | :--- |
| `GET /matches/{id}/room` has no access check: any logged-in user can read the room state. Only credentials are protected. Contract §6.1 says participants and organizer only. | Backend (Match Flow) change; reported to the owner. |
| The Match Room API does not tell a captain whether they already submitted a score (B1). After a reload the form reappears, and a second submit is handled as "already submitted". | The team chose the frontend fallback; the API change is planned later. |
| The last free slot can be over-booked by two captains at the same instant (count-then-insert in `RegistrationService`). | Needs a lock or constraint; listed in the strategy's risk table. |
| `cs2ServerConnect` is public in `GET /tournaments/{id}`. | Contract change is needed (US-26). |
| Bracket, check-in and standings UI (US-16, US-21, US-35). | The backend is on the unmerged branch `feat/bracket-engine`. |
| US-30 says "JPG and PNG", while the contracts and code also accept WEBP. | The code follows the contract; this is documentation drift. The requirement should be updated. |
| "Slots full" and "deadline passed" share `REGISTRATION_CLOSED`, so the UI cannot say "немає вільних місць" precisely. | The contract allows it; a dedicated code is a possible improvement. |
| A roster may stay locked after its tournament is completed (`TournamentRosterLockCheckerImpl` ignores the tournament status). | A question for the Core Platform owner. |
| Not covered yet: frontend test for the US-12 sc.4 disabled button, US-11 sc.1 copy-to-clipboard, and a concurrency test for the last slot. | Out of scope for this iteration. |

## 4. Engineering reflection: where the AI was wrong or tried to cut corners

1. **It invented history in the deliverables.**
   - *What happened:* when drafting the prompt files, the AI wrote "the AI first proposed H2, we rejected it" and "each `/generate-tests` run printed a table for approval". Neither happened in this session.
   - *Fix:* we caught it on review and rewrote both sections to match the transcript. *Lesson:* reflection sections written by an AI must be checked against what actually happened.
2. **It broke the project's test style rules.** The first US-12 happy-path test compared the response field by field (`extracting("teamId", ...)`), which CLAUDE.md §6 forbids. It was replaced with a recursive comparison against an expected DTO.
3. **It made a lifecycle mistake that would have caused flaky tests.**
   - *What happened:* the first `IntegrationTestBase` used `@Testcontainers` + `@Container` on a static field of an abstract class. JUnit would stop the container after the first test class, while Spring's cached context kept the old JDBC URL.
   - *Fix:* replaced with a JVM-wide singleton container.
4. **It retried instead of diagnosing.**
   - *What happened:* when Docker Desktop crashed, the agent's first move was to restart it in a wait loop.
   - *Fix:* the developer stopped it and asked for an analysis. The real causes were stale AF_UNIX socket files and a C: drive with about 3 GB free.
5. **One test assertion was weak.**
   - *What happened:* a test looked for the text `13:9` anywhere in the dispute card. It would have passed even if the AI-hint block were empty, because the captains' scores also contain `13:9`.
   - *Fix:* the assertion is now scoped with `within()` to the «Підказка ШІ» block.
6. **It changed existing tests, so we reviewed the change.**
   - *What happened:* adding the organizer check to the drawer made one extra API call, which consumed one response from the `mockResolvedValueOnce` sequences in two existing tests.
   - *Review:* only the mock setup was extended (one extra response, plus a mocked dispute list). No assertion was weakened, and we confirmed this in `git diff`.
7. **Mocking boundaries.** No backend integration test mocks anything. The frontend tests mock only the `src/api/*` functions, never the component or validator under test, and every assertion checks rendered text, the enabled state of controls, or the exact arguments sent to the API.
8. **It took the expected value from the code, which is the exact trap the theory warns about.**
   - *What happened:* for the US-12 "wrong discipline" checklist item, the AI read the expected status from the exception class (`422 VALIDATION_FAILED`) instead of `error-format.md` (400). The test passed and locked in a contract violation.
   - *Fix:* the independent read-only QA agent caught it. The code was fixed and the test re-derived from the contract.
   - *Lesson:* expected status and code values must come from `docs/contracts`, never from `grep` over the implementation.
9. **Mutation testing fooled by the build cache.**
   - *What happened:* after a mutation round, the restored source files kept their older timestamps. Maven's incremental compiler kept the mutated `.class` files, so the next run looked as if the fixes had disappeared.
   - *Fix:* a `./mvnw clean test` restored the truth.
   - *Lesson:* after mutation checks, run a clean build, or restore files with `git checkout`, which updates the timestamps.
10. **A naive "not exposed" assertion.**
    - *What happened:* the first US-27 checklist 5 test asserted that the room JSON "does not contain `13`". It failed on UUIDs and timestamps that contain those digits, and it could just as easily have passed for the wrong reason.
    - *Fix:* it now walks the JSON tree and asserts that no field name mentions a score or a submission.
11. **A self-testing frontend assertion.**
    - *What happened:* the US-34 component tests asserted the winner that the mocked API had returned.
    - *Fix:* the winner rule is now tested where it lives, in the backend integration test. The component tests claim only what they check: the request that is sent and the ruling that is displayed.


## 5. QA audit (prompt 6.3)

The read-only `qa-auditor` agent (`.claude/agents/qa-auditor.md`) audited all new tests against the Gherkin scenarios. The full table and our verdicts are in [`prompts/6.3_qa_audit_and_debugging.md`](../prompts/6.3_qa_audit_and_debugging.md) §3. In short:

- **2 real product bugs** (Bugs 6 and 7) were confirmed against the contract and requirement, fixed in code, and covered by tests that are red on the old code.
- **1 documentation drift** (WEBP), **1 by-design limitation** (`REGISTRATION_CLOSED` for full slots), and **1 open question** (roster lock after completion) are recorded above.
- **Test-quality fixes:**
  - the self-testing US-34 UI assertions were replaced by 7 backend dispute-resolution tests;
  - combined scenarios were split into separate tests;
  - the mislabelled 403 test now uses a real non-captain teammate;
  - 401 and 404 tests were added;
  - field-by-field row checks were replaced with whole-row comparisons;
  - US-05 sc.2 and the US-32 screenshot tests were added, and untagged tests were retagged;
  - strategy §4 was made consistent with its own examples.
- Confirmed by the audit: DB isolation (TRUNCATE before each test), no sleeps, no static state, and fake timers restored after use.

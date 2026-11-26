# Frontend MVP UI Plan (Practice 6): Scores, Disputes, Bracket

> **Owner:** Frontend (Andrii) · **Date:** 2026-11-26 · **Status:** v2, validated (see §6)
> **Sources of truth:** `docs/requirements.md` §5 (US-05, US-08, US-11, US-21, US-27, US-30..US-34), `docs/contracts/api-contracts.md` (§2.3, Epic 6–8), `docs/contracts/error-format.md`, `docs/testing-strategy.md`.

## 0. Goal and definition of done

The backend for score entry (US-27/28) and disputes with AI hints (US-30..34) is merged into `main`, but it has no UI. This plan adds that UI so the practice-6 demo can show new functionality in the browser:

1. A captain enters a score.
2. The two scores disagree.
3. A captain opens a dispute with a screenshot.
4. The organizer sees the counter, opens the dispute card with the AI hint and the nickname check, and resolves the dispute in one click.

**Done, per phase:**
- `npm run lint`, `npx tsc --noEmit` and `npm test` in `frontend/` are green.
- Every Gherkin scenario and checklist item listed for the phase has a component test named `US-xx sc.N: ...` or `US-xx checklist N: ...`, generated with `/generate-tests US-xx frontend`.
- At least one mutation check per phase: break a UI guard and confirm a test turns red.
- The manual smoke items for the phase are added to `docs/testing-report.md` §2. That file does not exist yet and is created in the global plan.

**Out of scope:**
- A WebSocket/STOMP client. Polling stays.
- React Query, Zod and React Hook Form. CLAUDE.md §7 names them, but the codebase does not use them, and migrating now is too risky.
- Redesign of existing screens.

**Lint rule for every new effect** (`react-hooks/set-state-in-effect`, React 19 plugin): do not call `setState` synchronously in an effect body.
- Use the pattern already proven in `TournamentDrawer.tsx:96-144`: an `async` function defined inside the effect, a `cancelled` flag, and state set only after `await`.
- Initialise loading flags to `true` instead of setting them at the start of the effect.

## 1. Current state

| Area | State |
| :--- | :--- |
| Stack | React 19 + **TypeScript (strict)** + Vite + Tailwind. Flat `api/ components/ context/ types/ utils/`. State uses `useState` with a hand-written `fetch` client (`api/http.ts`). Tests use Vitest + Testing Library (43 tests, green). |
| Lint | **Fails** at `MatchRoomModal.tsx:95` (`react-hooks/set-state-in-effect`). `loadRoom()` calls `setIsLoading(true)` synchronously (line 58). |
| Match Room | Ready, bans and credentials work. The room opens only from hard-coded QA match ids (`App.tsx:41-45`, V4 matches only). The menu is hidden below `lg`. No score form, no dispute button, no timers. |
| Disputes | No UI. `MatchRoomModal` only shows a "DISPUTED" label. |
| Bracket / check-in | No UI. The backend exists only on the unmerged branch `origin/feat/bracket-engine`. |
| Other High findings | No `/tournaments/{id}` route for the public link (US-11). The invite token is lost after login (US-05). Chess tournaments show a map pool (US-08). |

## 2. Backend touchpoints and gaps (decide before coding)

| # | Gap (verified) | Impact | Proposal |
| :--- | :--- | :--- | :--- |
| B1 | `MatchRoomResponse` has no score-submission data. Only `credentials` depends on the viewer. | After a reload the captain sees an empty form, and a second submit returns `409 SCORE_ALREADY_SUBMITTED`. The UI cannot tell whether the opponent has submitted. BR-45 lets **either** captain open a dispute once any submission exists. | **Recommended:** add a viewer-specific `scoreReport` to `MatchRoomResponse`: `{ myTeamSubmitted, myTeam1Score, myTeam2Score, opponentSubmitted, scoresMatched }`. Opponent **values** stay hidden (US-27 AC5).<br>Order of work: update `api-contracts.md` §6 first (CLAUDE.md rule 10), then the code, then an integration test.<br>Agree this with the Match Flow owner (Denys).<br>**Fallback:** keep the last `SubmitScoreResponse` in component state, treat `409 SCORE_ALREADY_SUBMITTED` as "already submitted, waiting", show "Відкрити спір" to both captains while `IN_PROGRESS`, and show `DISPUTE_NOT_ALLOWED` as "Спочатку хоча б один капітан має внести рахунок". |
| B2 | No public endpoint lists a tournament's matches on `main`. `findMatchesByTournamentId` is internal only. | There is no real way to navigate to a Match Room. | This arrives with the bracket merge (`GET /tournaments/{id}/bracket`). Until then the Match Room stays reachable through QA links; the V6 ids are added in F1. |
| B3 | `BracketMatchDto` on the bracket branch has no scores, and `MatchStatus` has no "технічна поразка" value. | US-21 AC2 (show the score) and the tech-loss status cannot be met. | Ask the Tournament Engine owner (Yevhenii) to add nullable `team1Score/team2Score` before merging. Otherwise show the winner highlight only. |
| B4 | `requestJson` always sets `Content-Type: application/json` when a body is present (`http.ts:49`). | Multipart upload would break: the boundary is missing. | F2: skip the header when `body instanceof FormData`. |
| B5 | `frontend/nginx.conf` has no `client_max_body_size`, so nginx's 1 MB default applies in Docker. | Screenshots between 1 and 10 MB get an HTML `413`, which the client turns into the code `HTTP_413`. | F2: add `client_max_body_size 13m;` to the `/api/` location, and map `HTTP_413` to the "Файл більший за 10 МБ" message. |
| B6 | Contract §6.1 says the room is readable by captains, members and the organizer, but `MatchFlowService.getRoom` has no access check. | Any logged-in user can read room state; only credentials are protected. | Not a frontend task. Record it in `docs/testing-report.md` as a finding. |

**Organizer detection:** `TournamentResponse` has **no** `createdByUserId`. The organizer id is available only as `organizerId` in `GET /tournaments/{id}/registrations` (public), and the drawer already uses it (`TournamentDrawer.tsx:301-303`). F3 loads it when the drawer opens.

## 3. Phases

### F0. Hygiene and blocking bugs

| Task | Files | Tests (Vitest) |
| :--- | :--- | :--- |
| **F0.1** Fix lint. Load the room inside the effect with a local `async` function and a `cancelled` flag. Initialise `isLoading` to `true` and drop the synchronous `setIsLoading(true)`. Keep polling in its own effect. | `components/MatchRoomModal.tsx` | Existing MatchRoomModal tests stay green; `npm run lint` passes. |
| **F0.2** US-11: handle `/tournaments/{uuid}` on load and on `popstate` by opening the drawer for that id, also for anonymous users. On `404 TOURNAMENT_NOT_FOUND` show "Турнір не знайдено". Push the URL when the drawer opens and go back to `/` when it closes. | `App.tsx`, new `utils/route.ts` | `route.test.ts`: parses `/tournaments/<uuid>` and rejects other paths. `App` tests: "US-11 sc.2: anonymous deep link opens the tournament" and "US-11 checklist: unknown id shows the not-found message". |
| **F0.3** US-05: keep the invite token across login. Before opening the auth modal from the join screen, store `{token, savedAt}` in **`localStorage`** (not `sessionStorage`, because the Mailpit magic link opens in a new tab). Wrap every read and write in try/catch, and expire the entry after 30 min. After `/me` succeeds on `/`, reopen `JoinInviteModal` and delete the key. | `components/JoinInviteModal.tsx`, `App.tsx`, new `utils/pendingInvite.ts` | `pendingInvite.test.ts` (save, read, expiry, storage throws). `JoinInviteModal.test.tsx`: complete US-05 **sc.1–sc.5** (the file has 2 tests today): sc.1 happy join; sc.2 `ROSTER_FULL` message; sc.3 `PLAYER_ALREADY_IN_TEAM`; sc.4 `INVITE_LINK_EXPIRED` invalid invite; sc.5 `ROSTER_LOCKED`; plus "US-05 checklist 2: returns to the invite after login". |
| **F0.4** US-08: do not render the map-pool section for `CHESS`, even when the backend sends a pool. | `components/TournamentDrawer.tsx` | `TournamentDrawer.test.tsx`: "US-08 checklist 4: chess tournament shows no map pool". |

### F1. US-27: score entry in the Match Room (CS2 / Brawl Stars)

- **QA access for the demo:** add the V6 matches `33333333-0005-4000-8000-000000000001/…02` ("Dispute QA Arena — CS2") to `QA_MATCH_OPTIONS` (`App.tsx:41-45`). Each rehearsal uses up a semifinal, so reset the DB with `docker compose down -v && docker compose up` between runs.
- **API** (`api/matches.ts`): `submitScore(matchId, team1Score, team2Score): Promise<SubmitScoreResult>`, which calls `POST /api/v1/matches/{id}/scores`.
  - Type `SubmitScoreResult { matchStatus, winnerTeamId: string|null, bothSubmitted, scoresMatched }`.
  - Add `ScoreReport` if B1 is approved.
- **Shared validator** (`utils/score.ts`): `validateScorePair(a: string, b: string)` returns field errors, or `null` when both values are valid. It mirrors the backend and US-27 AC2/AC3, and F4 reuses it.

  | Input | Message |
  | :--- | :--- |
  | empty | "Вкажіть рахунок" |
  | not `^\d+$` (`-1`, `abc`, `13.5`) | "Лише ціле невід'ємне число" |
  | greater than 999 | "Не більше 999" |
  | equal values | "Нічия неможлива — має бути переможець" |

- **UI:** new `components/ScoreEntryForm.tsx`, rendered in `MatchRoomModal` only when `status === 'IN_PROGRESS'`, the discipline is not `CHESS`, and the viewer is a captain. Captain detection already exists: `user.id` is compared with `captainId` from `getTeamById` (`MatchRoomModal.tsx:173-175`).
  - Two numeric text inputs labelled with team names, with errors under each field (`aria-describedby`).
  - The button is disabled while sending.
- **Result states:**

  | Response | UI |
  | :--- | :--- |
  | `bothSubmitted=false` | "Рахунок збережено. Очікується підтвердження суперником". The form is hidden. **The opponent's score is never shown.** |
  | `bothSubmitted && !scoresMatched` | "Рахунки не збігаються" plus the **"Відкрити спір"** button (F2). |
  | `matchStatus=COMPLETED` | "Матч завершено. Переможець: <team>". |

- **Error map:**

  | Code | UI |
  | :--- | :--- |
  | `SCORE_ALREADY_SUBMITTED` | Switch to the waiting state. |
  | `MATCH_NOT_IN_PROGRESS` | Reload the room. |
  | `SCORE_TIE_NOT_ALLOWED`, `VALIDATION_FAILED` | Show `detail` at the form. |
  | `ONLY_CAPTAIN_ALLOWED` | Hide the form. |
  | `SCORE_ENTRY_NOT_AVAILABLE` (chess) | Hide the form and show the chess note. |
  | `MATCH_NOT_FOUND` | Show the room error. |

- **Chess:** no form. Show the neutral note "Для шахових матчів рахунок вручну не вводиться". Do **not** promise an automatic import: there is no Chess.com sync on `main`.
- **Tests:**
  - `utils/score.test.ts`: table-driven over every AC2/AC3 case.
  - `ScoreEntryForm.test.tsx`:
    - sc.1: calls `submitScore(id, 13, 9)` and shows the waiting text.
    - sc.2: `-1`, `abc`, `13.5` make no API call and show the field error.
    - sc.3: 5:5 makes no call and shows the tie message.
    - checklist 5: after submit, no opponent score is rendered.
    - Mismatch: shows "Відкрити спір".
    - Completed: shows the winner.
    - `SCORE_ALREADY_SUBMITTED`: shows the waiting state.
  - `MatchRoomModal.test.tsx`:
    - sc.4: chess shows no form.
    - A non-captain sees no form.

### F2. US-30: opening a dispute with a screenshot (captain)

- **HTTP:** fix B4 in `api/http.ts`. **nginx:** fix B5.
- **API** (`api/disputes.ts`): `openDispute(matchId, file)` sends `FormData` with the part `screenshot` to `POST /api/v1/matches/{id}/disputes`. Type `OpenDisputeResult`.
- **UI:** `components/OpenDisputeDialog.tsx`, opened from the F1 mismatch or waiting state. Under the fallback it is available to both captains while `IN_PROGRESS`; see B1.
  - `accept="image/png,image/jpeg,image/webp"` and an image preview.
  - **Client checks:**

    | Case | Message |
    | :--- | :--- |
    | no file (sc.2) | "Додайте скріншот табло" |
    | other MIME type (sc.3) | "Допустимі формати: PNG, JPG, WEBP" |
    | larger than `10 * 1024 * 1024` bytes | "Файл більший за 10 МБ" |

  - **Server errors** (the server stays authoritative; it detects the format from the file signature):

    | Code | UI |
    | :--- | :--- |
    | `INVALID_SCREENSHOT_FORMAT` | formats message |
    | `SCREENSHOT_TOO_LARGE`, `HTTP_413` | size message |
    | `DISPUTE_NOT_ALLOWED` | "Спочатку хоча б один капітан має внести рахунок" |
    | `DISPUTE_ALREADY_OPEN` (sc.4; the server checks this before validating the file) | "Спір уже відкрито" |
    | `ONLY_CAPTAIN_ALLOWED` | close the dialog |
    | `MATCH_NOT_FOUND` | error banner |

  - **On success:** close the dialog and show the banner "Спір відкрито — організатор розгляне його". The next poll shows status `DISPUTED` ("Спір"), and the score form is hidden.
- **Tests:**
  - `OpenDisputeDialog.test.tsx`:
    - sc.1: a PNG calls `openDispute` with that `File` and shows success.
    - sc.2: no file, no call.
    - sc.3: a `.pdf` makes no call and shows the formats message.
    - sc.4: `409 DISPUTE_ALREADY_OPEN` shows its message.
    - checklist 4: an 11 MB file makes no call.
  - `MatchRoomModal.test.tsx`: "US-30 checklist 5: DISPUTED room shows «Спір» and hides the score form".
  - `http.test.ts`: a FormData body is sent without a JSON content type.

### F3. US-31: organizer dispute dashboard

- **API:** `listTournamentDisputes(tournamentId)` calls `GET /api/v1/tournaments/{id}/disputes`. Types `DisputeList` and `DisputeSummary` copy contract §8.2 exactly.
- **Organizer check:** when the drawer opens, load `getRegistrations(tournament.id)` (public) to get `organizerId`, reusing it for the participants tab. If `user?.id === organizerId`, load the dispute list **immediately** so the badge shows before the tab is clicked. Then poll every 10 s while the drawer is open. Clear the interval on unmount and refresh right after a resolve.
- **UI:** a new **"Спори"** tab in `TournamentDrawer`, rendered only for the organizer (AC5).
  - The tab label shows a red counter with `openDisputesCount` when it is above 0 (sc.1, sc.3).
  - Each row shows:
    - "Раунд N · Матч M" and the team names.
    - A status chip: `OPEN` is red "Спір"; `RESOLVED` is grey "Опрацьовано" (sc.2).
    - An AI summary. `aiConfidence` is 0..1, so display it as ×100 %. While analysis runs: "Аналіз триває…". If unrecognised: "Не розпізнано".
    - A neutral "Перевірте нікнейми" hint only when `aiAnalysisStatus === 'SUCCESS' && allRosterNicknamesFound === false`. The flag is also `false` when no nicknames were detected, and the summary cannot tell those cases apart. The precise ringer warning appears in F4.
  - Clicking a row opens F4 (AC4).
  - Empty state: "Спорів немає". `403` hides the tab; other errors show a banner.
- **Tests** (`TournamentDisputesTab.test.tsx` plus a drawer test):
  - sc.1: one OPEN dispute shows the "Спір" chip and counter 1.
  - sc.2: a RESOLVED dispute shows "Опрацьовано" and no counter.
  - sc.3: two OPEN disputes show counter 2.
  - AC4: a row click calls `onOpenDispute(disputeId)`.
  - AC5: a non-organizer does not see the tab, and `listTournamentDisputes` is never called.

### F4. US-32 / US-33 / US-34: dispute card and resolution (organizer)

- **API:**
  - `getDispute(id)` calls `GET /api/v1/disputes/{id}`.
  - `resolveDispute(id, body)` calls `POST /api/v1/disputes/{id}/resolve`.
  - Types copy contract §8.3 and §8.4.
- **UI:** `components/DisputeCardModal.tsx`.
  - **Screenshot:** `<img src={screenshotUrl}>`. The URL is relative and same-origin through the Vite/nginx `/api` proxy, so the cookie is sent. A click opens it full size.
  - **Captain reports:** both `submittedScores`, labelled with team names.
  - **«Підказка ШІ» block (US-32)**, by `aiAnalysisStatus`:

    | Status | UI |
    | :--- | :--- |
    | `PENDING` | "ШІ аналізує скріншот…". Refetch every 3 s; stop on a status change or on unmount. |
    | `SUCCESS` | "Підказка ШІ: 13:9 на користь <team> (впевненість 92%)", with confidence ×100. |
    | `UNRECOGNIZED` / `FAILED` | "Рахунок не розпізнано — введіть рахунок вручну". |

    The hint never closes the match by itself (US-32 sc.3).
  - **Nickname check (US-33):** one row per `nicknameChecks` entry with the nickname, its side team, and a status badge:
    - **"Збіг із ростером"** when `inRoster` (sc.1);
    - **"Не знайдено в ростері"** otherwise, with the row highlighted.

    A card-level warning **"Можливий сторонній гравець"** appears when any `inRoster === false` (sc.2 / AC3). Empty `detectedNicknames` shows "Нікнейми не розпізнано" and no ringer warning (sc.3).
  - **Actions (US-34)**, only while `status === 'OPEN'`:
    - **"Затвердити підказку ШІ"**: enabled only when `canApproveAiSuggestion`. Sends `{decision:'AI_SUGGESTION'}`.
    - **Manual ruling:** two inputs validated with `validateScorePair` (AC4) plus optional notes, at most 2000 characters. Sends `{decision:'MANUAL_SCORE', team1Score, team2Score, resolutionNotes}`.
    - Both actions ask for confirmation, because the decision is final (BR-50).
  - **After resolve:** show "Спір закрито, переможець: <team>", refresh the F3 list, and switch the card to read-only showing `resolution`.
  - **Error map:**

    | Code | UI |
    | :--- | :--- |
    | `DISPUTE_ALREADY_RESOLVED` | reload the card |
    | `AI_SUGGESTION_UNAVAILABLE` | disable the button and show a message |
    | `SCORE_TIE_NOT_ALLOWED`, `VALIDATION_FAILED` (scores missing for `MANUAL_SCORE`) | show the message at the form |
    | `AUTH_FORBIDDEN` | show "Лише організатор може закрити спір" and close |
    | `DISPUTE_NOT_FOUND` | show an error |

- **Tests** (`DisputeCardModal.test.tsx`). Polling tests use `vi.useFakeTimers({ shouldAdvanceTime: true })` and `await act(() => vi.advanceTimersByTimeAsync(3000))`, never real sleeps.

  | Story | Test |
  | :--- | :--- |
  | US-34 sc.1 | AI 13:9: approve calls `resolveDispute(id, {decision:'AI_SUGGESTION'})` and shows the success text. |
  | US-34 sc.2 | Manual 9:13 calls `resolveDispute` with `MANUAL_SCORE` 9/13. |
  | US-34 sc.3 / US-32 sc.2 | `UNRECOGNIZED`: approve is disabled and the manual form is available. |
  | US-34 sc.4 | 5:5 makes no call and shows the tie message. |
  | US-34 checklist 5 | `AUTH_FORBIDDEN` shows the organizer-only message. |
  | US-32 sc.1 | The «Підказка ШІ» label shows the score and 92%. |
  | US-32 sc.3 | Rendering the hint does not call `resolveDispute`. |
  | US-33 sc.1 | Shows "Збіг із ростером". |
  | US-33 sc.2 | `inRoster=false` shows "Не знайдено в ростері" and the card warning. |
  | US-33 sc.3 | No nicknames: shows "Нікнейми не розпізнано" and no warning. |
  | Polling | `PENDING` stops refetching after `SUCCESS`. |
  | Resolved | A resolved dispute is read-only, with no action buttons. |

### F5. After the bracket branch is merged (blocked by B2/B3)

| Story | UI | Tests |
| :--- | :--- | :--- |
| **US-21** public bracket (core of F5) | A "Сітка" tab in `TournamentDrawer`, visible without login.<br>Columns per round (`roundName`); each pair shows team names or "Очікується переможець", a status chip, the winner highlight, the score (if B3 is done) and "Технічний прохід" for byes.<br>The viewer's own team is highlighted (sc.1).<br>`GET /bracket` returns **200 with `rounds: []`** before generation, so show "Сітка з'явиться на початку турніру" when `rounds.length === 0` (sc.3). Horizontal scroll on mobile. | sc.1 rounds, statuses and own team; sc.2 a completed match puts the winner into the next round; sc.3 empty rounds; byes; fixtures for 8, 16 and 32 slots. |
| Stretch, outside this plan's story list: US-22 navigation, US-16 check-in, US-35 standings | Click a match to open `MatchRoomModal`, and keep the QA links only behind `import.meta.env.DEV`. Check-in button while `CHECK_IN_ACTIVE`. Top-3 block once `COMPLETED`. | Do these only if time remains. |

### F6. Optional polish
- Ready (10 min) and ban (30 s) countdowns from `readyDeadlineAt` / `banDeadlineAt`. They are display only; the server stays authoritative (ADR-0005).
- Medium findings from the review:
  - the drawer open race in `App.tsx:134-142`;
  - the polling error UX: keep the last room, and do not reopen the auth modal every tick;
  - toast timer cleanup;
  - stacked Escape handlers.

## 4. Order, estimate, demo

1. **Order:** B1 decision → F0 (0.5 d) → F1 (0.5–1 d) → F2 (0.5 d) → F3 (0.5 d) → F4 (1 d). That is **3.5–4 days**, including the B1 contract, code and test.
2. Every phase ends green on `npm run lint && npx tsc --noEmit && npm test`, and then on `npm test` at the repo root.
3. F5 starts only after `feat/bracket-engine` is merged into `main`.
4. **Demo** (V6 fixture "Dispute QA Arena — CS2", fresh DB):
   1. The two captains (`t.captain@…`, `ct.captain@…`) log in through Mailpit and open semifinal 1 through the QA menu.
   2. They enter different scores, and the room shows "Рахунки не збігаються".
   3. One captain opens a dispute with `ai-service/resources/test_screenshot_1.jpg` (162 KB).
   4. The organizer (`dispute.organizer@…`) sees counter 1 on "Спори" and opens the card with the AI hint and the nickname check.
   5. The organizer clicks "Затвердити підказку ШІ". The match becomes COMPLETED and the winner moves into the final.

## 5. Risks
- **Polling load.** The card polls every 3 s and the room every 4 s. Both stop on unmount, and the card also stops once the analysis is finished.
- **Screenshot auth.** It works only same-origin through the proxy. Never call `:8080` directly from `:3000`.
- **Time zones.** The Kyiv-pinned display stays as it is; this is recorded in the testing report.

## 6. Validation log
An independent read-only subagent checked plan v1 against the code, the contracts and the requirements (2026-11-26). Its verdict was **APPROVE WITH CHANGES**. Corrections applied in v2:
- **Blocker:** `createdByUserId` does not exist. The organizer is now detected through `registrations.organizerId`, loaded when the drawer opens.
- **Major:**
  - `localStorage` with a TTL replaces `sessionStorage`, because the magic link opens in a new tab.
  - nginx `client_max_body_size` and the `HTTP_413` mapping (B5).
  - The V6 demo matches are added to the QA menu.
  - The error maps are completed.
  - US-33 "Збіг із ростером" and the card-level ringer warning are added, and the false ringer warning in F3 is avoided.
  - The lint-safe effect pattern is applied.
- **Minor:**
  - The BR-45 fallback lets both captains open a dispute.
  - The chess note no longer promises an automatic import.
  - The US-08 test is renamed.
  - `docs/testing-report.md` is now marked as to be created.
  - Confidence is shown ×100, and the size check uses 10 MiB.
  - The fake-timer API for Vitest is fixed.
  - The empty bracket is detected by `rounds.length`.
  - US-16/22/35 are marked as stretch.
  - The room access mismatch is recorded (B6).
  - The estimate is raised.
- **Previously unmapped scenarios**, now mapped to tests: US-05 sc.1–5, US-11 sc.2, US-27 AC5, US-30 oversize and status, US-31 AC4, US-32 sc.1/sc.3, US-33 sc.1–3, US-34 AC5.

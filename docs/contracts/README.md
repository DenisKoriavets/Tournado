# Technical Contracts Index — Tournado

> **Status:** Accepted  
> **Course Milestone:** Practical 4 — Technical Design & Walking Skeleton  
> **Traceability:** Requirements (§4 User Stories `US-01…US-36`, §5 Acceptance Criteria), `docs/analysis/requirements-digest.md`, `docs/adr/`

This directory defines the formal, strict engineering contracts governing all interactions between the React frontend, the Java/Spring Boot core backend, the PostgreSQL database, and the Python AI service.

---

## 1. Contracts Structure

| Document | Purpose & Scope | Key Topics |
| :--- | :--- | :--- |
| 🗄️ **[`data-schema.md`](data-schema.md)** | **Data Storage Schema (PostgreSQL DDL)** | Full SQL DDL script, Flyway conventions, UUID PKs, foreign keys, partial indexes, snapshot rules, versioning (`@Version`). |
| 🌐 **[`api-contracts.md`](api-contracts.md)** | **Interaction Contracts (REST & WebSockets)** | RESTful endpoints for US-01–US-36, STOMP topics/application destinations, request/response models, internal AI Service contract. |
| ⚠️ **[`error-format.md`](error-format.md)** | **Unified Error Format Specification** | RFC 7807 Problem Details, HTTP status code mapping, application error codes catalog (`ROSTER_FULL`, etc.). |

The prompt used to design these contracts is saved in **[`prompts/4.3_technical_contracts.md`](../../prompts/4.3_technical_contracts.md)**.

---

## 2. Core Architectural Conventions Summary

1. **Identifiers:** All public entities use application-generated `UUID` primary keys. No sequential or guessable IDs (ADR-0004).
2. **Timestamps:** Every timestamp is stored as `TIMESTAMPTZ` in UTC (Java `Instant`). All timers and deadlines are enforced server-side (ADR-0005, NFR-07).
3. **Optimistic Locking:** Aggregate roots (`tournaments`, `matches`, `teams`) use a `version BIGINT DEFAULT 0` column to protect against concurrent modifications (ADR-0004, NFR-05).
4. **State Machine Invariants:** Match and tournament state progressions are enforced via database `CHECK` constraints and transactions (ACID).
5. **No Redis for State:** Ephemeral match room timers (`ready_deadline_at`, `ban_deadline_at`) reside directly in PostgreSQL, surviving application restarts (ADR-0005).

---

## 3. Standard Enumerations (Reference Dictionary)

| Enum Name | Allowed Values | Description / Context |
| :--- | :--- | :--- |
| `Discipline` | `CS2`, `BRAWL_STARS`, `CHESS` | Game disciplines supported by Tournado MVP. |
| `TournamentStatus` | `DRAFT`, `REGISTRATION_OPEN`, `CHECK_IN_ACTIVE`, `IN_PROGRESS`, `COMPLETED` | Complete lifecycle stages of a tournament. |
| `RegistrationStatus` | `REGISTERED`, `CHECKED_IN`, `EXCLUDED` | Entry state of a team in a tournament. |
| `BracketType` | `SINGLE_ELIMINATION` | Tournament tree generation algorithm. |
| `MatchStatus` | `PENDING`, `READY_CHECK`, `PICK_BAN`, `IN_PROGRESS`, `COMPLETED`, `DISPUTED` | State machine of an individual match. |
| `DisputeStatus` | `OPEN`, `RESOLVED` | Conflict resolution ticket state. |
| `AiAnalysisStatus` | `SUCCESS`, `UNRECOGNIZED`, `FAILED` | Outcome of multimodal scoreboard inspection. |

---

## 4. Full Traceability Matrix: User Stories (MVP) ➔ Contracts

| User Story | Name / Domain | DB Tables Affected | Primary API Endpoints / Destinations |
| :--- | :--- | :--- | :--- |
| **US-01** | External Login (Steam, Email) | `users`, `email_login_tokens` | `GET /auth/steam/*`, `POST /auth/email/*`, `GET /auth/me` |
| **US-02** | Link Game Handles in Profile | `users` | `PATCH /users/me/game-identifiers` |
| **US-03** | Create Team | `teams`, `team_members` | `POST /teams` |
| **US-04** | Generate Team Invite Link | `team_invites` | `POST /teams/{teamId}/invites` |
| **US-05** | Join Team via Link | `team_members`, `team_invites` | `POST /teams/join/{token}` |
| **US-06** | Check Team Roster Capacity | `team_members` | `GET /teams/{teamId}` |
| **US-07** | Select Discipline & Team Size | `tournaments` | `POST /tournaments` |
| **US-08** | Auto-populate Map Pool | `tournament_map_pool` | `POST /tournaments` |
| **US-09** | Bracket Format & Slots | `tournaments` | `POST /tournaments` |
| **US-10** | Set Tournament Schedule | `tournaments` | `POST /tournaments` |
| **US-11** | Public Tournament Link | `tournaments` | `GET /tournaments/{id}` |
| **US-12** | Register Team for Tournament | `registrations` | `POST /tournaments/{id}/register` |
| **US-13** | Remove Team Prior to Start | `registrations` | `DELETE /tournaments/{id}/registrations/{teamId}` |
| **US-14** | Roster Lock Snapshots | `registration_roster_snapshots` | `POST /tournaments/{id}/lock-rosters` |
| **US-15** | Auto-start Check-in Window | `tournaments` | Scheduled task / `GET /tournaments/{id}` |
| **US-16** | Confirm Check-in (Captain) | `registrations` | `POST /tournaments/{id}/check-in` |
| **US-17** | Purge Inactive Teams | `registrations` | Triggered during bracket generation |
| **US-18** | Generate Single Elim Bracket | `brackets`, `matches` | `POST /tournaments/{id}/generate-bracket` |
| **US-19** | Random Seeding | `matches` | Internal seeding in bracket generation |
| **US-20** | Assign Bye Matches | `matches` (`is_bye = true`) | Handled during bracket generation |
| **US-21** | View Public Bracket Tree | `brackets`, `matches` | `GET /tournaments/{id}/bracket`, WS `/topic/.../bracket` |
| **US-22** | Match Room Lobby View | `matches` | `GET /matches/{id}/room`, WS `/topic/.../room` |
| **US-23** | Ready Check (Captain) | `matches` (`teamX_ready`) | `POST /matches/{id}/ready`, WS `/app/matches/{id}/ready` |
| **US-24** | Auto Technical Loss on Timeout| `matches` (`winner_team_id`) | Background task comparing `ready_deadline_at` |
| **US-25** | Veto Map Bans with 30s Timer | `match_bans`, `matches` | `POST /matches/{id}/ban`, WS `/app/matches/{id}/ban` |
| **US-26** | Reveal Game Credentials | `match_credentials` | `GET /matches/{id}/credentials` |
| **US-27** | Submit Score (Captain) | `score_submissions` | `POST /matches/{id}/scores` |
| **US-28** | Auto-advance on Matched Score | `matches` (`winner_team_id`) | Evaluated inside `POST /matches/{id}/scores` |
| **US-29** | Sync Chess.com Game Result | `chess_import_logs`, `matches` | `POST /matches/{id}/chess/sync` |
| **US-30** | Open Dispute & Upload Proof | `disputes` | `POST /matches/{id}/disputes` (Multipart) |
| **US-31** | Organizer Dispute Counter | `disputes`, `matches` | `GET /tournaments/{id}/disputes`, WS `/topic/.../dashboard` |
| **US-32** | AI Score OCR from Screenshot | `ai_analysis_results` | `GET /disputes/{id}`, `POST /ai/analyze-dispute` |
| **US-33** | AI Roster Nickname Check | `ai_analysis_results` | Evaluated inside `POST /ai/analyze-dispute` |
| **US-34** | Organizer Final Ruling | `disputes`, `matches` | `POST /disputes/{id}/resolve` |
| **US-35** | Finalize Tournament & Standings| `tournament_standings` | `GET /tournaments/{id}/standings` |
| **US-36** | Export Results to CSV | `tournament_standings` | `GET /tournaments/{id}/standings/export` |

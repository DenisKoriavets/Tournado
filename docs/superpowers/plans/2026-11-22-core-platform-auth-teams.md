# Core Platform, Auth, Teams & Tournament Registrations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement end-to-end authentication (Email Magic Link via Mailpit + Steam OpenID 2.0 + HttpOnly JWT), user profiles with game identifiers, teams & invites with roster validation, and tournament team registration / moderation / roster-lock snapshots across PostgreSQL, Spring Boot 3, and React 19 UI.

**Architecture:** Clean modular monolith (`identity`, `teams`, `tournaments`, `common`) following SOLID principles, communicating strictly via narrow public service interfaces (`CurrentUserProvider`, `UserQueryService`, `TeamQueryService`, `TournamentRosterLockChecker`). All persistence runs strictly against existing Flyway `V1__initial_schema.sql` under `ddl-auto: validate`, and all errors conform to RFC 7807 `ProblemDetail`.

**Tech Stack:** Java 21, Spring Boot 3.3.5 (Web, JPA, Validation, Security, Mail), JJWT 0.12.6, PostgreSQL 16, Checkstyle 3.4.0, JUnit 5, AssertJ, Mockito, React 19, TypeScript, Tailwind CSS, Vitest, ESLint 10.

**Spec:** [`docs/superpowers/specs/2026-11-22-core-platform-auth-teams-design.md`](../specs/2026-11-22-core-platform-auth-teams-design.md)

## Global Constraints

- Identifiers: `UUID` everywhere; UTC `Instant` mapped to `TIMESTAMPTZ`; `@Version` preserved on `tournaments` and `teams`.
- Database schema: Must strictly satisfy `V1__initial_schema.sql` under `hibernate.ddl-auto=validate`.
- Error responses: Strictly RFC 7807 `ProblemDetail` (`application/problem+json`) with codes from `docs/contracts/error-format.md`.
- Java Testing Rules: `givenX_y_shouldZ` (never use `when` in action `y`), AssertJ `assertThat(...)` + `usingRecursiveComparison()` against `*TestResources`, single blank line separators, zero phase comments, static star imports, no redundant `verify(...)` on stubbed calls, no `jsonPath(...)` in MockMvc tests.
- Deterministic Guardrails after every block: `cd core && mvn clean test checkstyle:check -DargLine="-Dnet.bytebuddy.experimental=true"` (backend) and `cd frontend && npm run lint && npm test && npx tsc --noEmit && npm run build` (frontend).
- Each block produces its own clean git commit on `feat/core-platform-auth-teams`.

---

### Block 0: Architecture Specification & Implementation Plan

- [x] Design & approve specification (`docs/superpowers/specs/2026-11-22-core-platform-auth-teams-design.md`).
- [ ] Save this implementation plan and commit Block 0 on `feat/core-platform-auth-teams`.

### Block 1: Common Security Infrastructure, Shared Discipline Kernel & Auth Wiring

**Files:**
- Modify: `core/pom.xml`, `core/src/main/resources/application.yml`, `docker-compose.yml`
- Create: `core/src/main/java/com/tournado/core/common/domain/Discipline.java`
- Create: `core/src/main/java/com/tournado/core/common/security/*` (`AuthenticatedUser`, `CurrentUserProvider`, `SecurityContextCurrentUserProvider`, `JwtProperties`, `JwtTokenService`, `JwtAuthenticationFilter`, `RestAuthenticationEntryPoint`, `SecurityConfig`, `TimeConfig`)
- Modify: `core/src/main/java/com/tournado/core/tournaments/**` (update `Discipline` import cleanly, wire `CurrentUserProvider` into `TournamentController` / `TournamentService`)
- Test: `JwtTokenServiceTest`, `TournamentControllerTest` (authenticated & 401 `AUTH_UNAUTHORIZED`), `TournamentServiceTest`, `TournamentTestResources`

### Block 2: Identity Module (`US-01`, `US-02`)

**Files:**
- Create: `core/src/main/java/com/tournado/core/identity/domain/*` (`UserEntity`, `EmailLoginTokenEntity`, `GameIdentifierValidator`)
- Create: `core/src/main/java/com/tournado/core/identity/exception/*` (`SteamAuthFailedException`, `EmailMagicLinkExpiredException`, `InvalidGameIdentifierException`)
- Create: `core/src/main/java/com/tournado/core/identity/repository/*` (`UserRepository`, `EmailLoginTokenRepository`)
- Create: `core/src/main/java/com/tournado/core/identity/infra/*` (`SteamOpenIdClient`, `HttpSteamOpenIdClient`, `MagicLinkMailSender`, `SmtpMagicLinkMailSender`)
- Create: `core/src/main/java/com/tournado/core/identity/service/*` (`EmailMagicLinkService`, `SteamAuthService`, `UserProfileService`, `UserQueryService`, `UserProfileSummary`)
- Create: `core/src/main/java/com/tournado/core/identity/api/*` (`AuthController`, `UserProfileController`, DTOs)
- Test: `IdentityTestResources`, `GameIdentifierValidatorTest`, `EmailMagicLinkServiceTest`, `SteamAuthServiceTest`, `UserProfileServiceTest`, `AuthControllerTest`, `UserProfileControllerTest`

### Block 3: Teams Module (`US-03`..`US-06`)

**Files:**
- Create: `core/src/main/java/com/tournado/core/teams/domain/*` (`TeamEntity`, `TeamMemberEntity`, `TeamInviteEntity`)
- Create: `core/src/main/java/com/tournado/core/teams/exception/*` (`TeamNotFoundException`, `TeamNameAlreadyExistsException`, `RosterFullException`, `PlayerAlreadyInTeamException`, `InviteLinkExpiredException`, `OnlyCaptainAllowedException`, `RosterLockedException`)
- Create: `core/src/main/java/com/tournado/core/teams/repository/*` (`TeamRepository`, `TeamMemberRepository`, `TeamInviteRepository`)
- Create: `core/src/main/java/com/tournado/core/teams/service/*` (`TeamQueryService`, `TeamView`, `TeamMemberView`, `TournamentRosterLockChecker`, `TeamService`, `TeamInviteService`)
- Create: `core/src/main/java/com/tournado/core/teams/api/*` (`TeamController`, DTOs)
- Test: `TeamTestResources`, `TeamServiceTest`, `TeamInviteServiceTest`, `TeamControllerTest`

### Block 4: Tournament Registrations, Moderation & Roster Lock (`US-12`..`US-14`)

**Files:**
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/*` (`RegistrationEntity`, `RegistrationStatus`, `RosterSnapshotEntity`)
- Create: `core/src/main/java/com/tournado/core/tournaments/exception/*` (`AlreadyRegisteredException`, `RegistrationClosedException`, `RosterNotFullException`, `MissingGameIdentifierException`, `OnlyOrganizerAllowedException`)
- Create: `core/src/main/java/com/tournado/core/tournaments/repository/*` (`RegistrationRepository`, `RosterSnapshotRepository`)
- Create/Modify: `core/src/main/java/com/tournado/core/tournaments/service/*` (`GameIdentifierExtractor`, `RegistrationService`, `TournamentRosterLockCheckerImpl`, update `TournamentService` `registeredTeamsCount`)
- Create: `core/src/main/java/com/tournado/core/tournaments/api/*` (`TournamentRegistrationController`, DTOs)
- Test: `RegistrationServiceTest`, `TournamentRegistrationControllerTest`, update `TournamentServiceTest` & `TournamentTestResources`

### Block 5: Frontend Auth, Profile, Teams & Tournament Registration UI

**Files:**
- Create: `frontend/src/types/auth.ts`, `teams.ts`, `registrations.ts`
- Create/Modify: `frontend/src/api/http.ts`, `auth.ts`, `teams.ts`, `registrations.ts`, `tournaments.ts`
- Create: `frontend/src/context/AuthContext.tsx`
- Create: `frontend/src/components/AuthModal.tsx`, `ProfileModal.tsx`, `TeamsDrawer.tsx`, `JoinInviteModal.tsx`
- Modify: `frontend/src/components/icons/Icons.tsx`, `TournamentDrawer.tsx`, `App.tsx`
- Test: `frontend/src/components/AuthModal.test.tsx`, `ProfileModal.test.tsx`, `TeamsDrawer.test.tsx`, update `TournamentDrawer.test.tsx`

### Block 6: Final Docker End-to-End Verification & Documentation Sync

- Rebuild all Docker containers (`docker compose up --build -d`), verify Mailpit email flow, Steam redirect endpoint, profiles, teams, invites, registrations, roster lock, and PostgreSQL persistence.
- Update `README.md` with complete live demo instructions & run all tests + Checkstyle + ESLint after final edits.

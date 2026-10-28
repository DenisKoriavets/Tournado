# API & Interaction Contracts Specification — Tournado

> **Status:** Accepted  
> **Protocols:** REST (HTTP/1.1 JSON), WebSocket (STOMP over SockJS), Internal HTTP  
> **Traceability:** ADR-0002, ADR-0003, ADR-0005, ADR-0006, ADR-0007, US-01–US-36

---

## 1. REST API Overview & Conventions

* **Base URL:** `/api/v1`
* **Content Type:** `application/json` (except file upload endpoints which use `multipart/form-data`)
* **Date & Time Format:** ISO-8601 UTC strings (`YYYY-MM-DDTHH:mm:ss.sssZ`)
* **Authentication:** Session cookie `X-Auth-Token` (HttpOnly JWT) or `Authorization: Bearer <token>`
* **Error Format:** Strict adherence to [RFC 7807 Problem Details](error-format.md)

---

## 2. REST Endpoints by Epic (Covering US-01 – US-36)

### Epic 1: Authentication & Profiles (US-01, US-02)

#### 1.1 `GET /api/v1/auth/steam/login`
* **Stories:** US-01
* **Description:** Initiates Steam OpenID 2.0 redirect URL.
* **Access:** Public
* **Response `302 Found`:** Redirects user to `https://steamcommunity.com/openid/login`.

#### 1.2 `GET /api/v1/auth/steam/callback`
* **Stories:** US-01
* **Description:** Validates OpenID response from Steam. If valid, provisions or retrieves user and sets JWT cookie.
* **Access:** Public
* **Response `302 Found`:** Sets `HttpOnly` cookie and redirects to `/` or deep link.
* **Errors:** `401 Unauthorized` (`STEAM_AUTH_FAILED`).

#### 1.3 `POST /api/v1/auth/email/request-link`
* **Stories:** US-01
* **Description:** Requests a magic login link to the user's email address.
* **Access:** Public
* **Request Body:**
  ```json
  { "email": "student@ukma.edu.ua" }
  ```
* **Response `200 OK`:**
  ```json
  { "message": "If the provided email is valid, a login link has been dispatched." }
  ```

#### 1.4 `GET /api/v1/auth/email/verify`
* **Stories:** US-01
* **Description:** Validates one-time token from email link and issues session.
* **Access:** Public
* **Query Params:** `token` (string, required)
* **Response `302 Found`:** Sets session cookie and redirects.
* **Errors:** `400 Bad Request` (`EMAIL_MAGIC_LINK_EXPIRED`).

#### 1.5 `GET /api/v1/auth/me`
* **Stories:** US-01, US-02
* **Description:** Returns profile data of the currently authenticated user.
* **Access:** Authenticated
* **Response `200 OK`:**
  ```json
  {
    "id": "e4b2d184-f376-476c-9226-64d852a39a01",
    "email": "student@ukma.edu.ua",
    "steamId": "76561198012345678",
    "displayName": "CyberHawk",
    "supercellPlayerTag": "#2PP98L9",
    "chessUsername": "cyber_master",
    "avatarUrl": "https://avatars.steamstatic.com/sample.jpg"
  }
  ```

#### 1.6 `PATCH /api/v1/users/me/game-identifiers`
* **Stories:** US-02
* **Description:** Updates player's external game handles. Empty string removes identifier.
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "steamId": "76561198012345678",
    "supercellPlayerTag": "#2PP98L9",
    "chessUsername": "cyber_master"
  }
  ```
* **Response `200 OK`:** Updated user object.
* **Errors:** `422 Unprocessable Entity` (`INVALID_GAME_IDENTIFIER`).

---

### Epic 2: Teams & Rosters (US-03 – US-06)

#### 2.1 `POST /api/v1/teams`
* **Stories:** US-03
* **Description:** Creates a new team. Creator becomes captain and first member.
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "name": "Navi Junior KMA",
    "discipline": "CS2"
  }
  ```
* **Response `201 Created`:**
  ```json
  {
    "id": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "name": "Navi Junior KMA",
    "discipline": "CS2",
    "captainId": "e4b2d184-f376-476c-9226-64d852a39a01",
    "memberCount": 1,
    "maxMembers": 5,
    "isFull": false
  }
  ```
* **Errors:** `409 Conflict` (`TEAM_NAME_ALREADY_EXISTS`), `422 Unprocessable Entity`.

#### 2.2 `POST /api/v1/teams/{teamId}/invites`
* **Stories:** US-04
* **Description:** Generates a new shareable invite link for team roster.
* **Access:** Captain only
* **Response `201 Created`:**
  ```json
  {
    "token": "inv_a8d712f5b6e9",
    "inviteUrl": "https://tournado.ua/join/inv_a8d712f5b6e9",
    "expiresAt": "2026-10-31T23:59:59Z"
  }
  ```
* **Errors:** `403 Forbidden` (`ONLY_CAPTAIN_ALLOWED`), `409 Conflict` (`ROSTER_FULL`).

#### 2.3 `POST /api/v1/teams/join/{token}`
* **Stories:** US-05
* **Description:** Authenticated player joins team roster via invite token.
* **Access:** Authenticated
* **Response `200 OK`:** Joined team overview.
* **Errors:** `400 Bad Request` (`INVITE_LINK_EXPIRED`), `409 Conflict` (`ROSTER_FULL`, `PLAYER_ALREADY_IN_TEAM`).

#### 2.4 `GET /api/v1/teams/{teamId}`
* **Stories:** US-06
* **Description:** Retrieves team roster and capacity indicator.
* **Access:** Public
* **Response `200 OK`:**
  ```json
  {
    "id": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "name": "Navi Junior KMA",
    "discipline": "CS2",
    "captainId": "e4b2d184-f376-476c-9226-64d852a39a01",
    "memberCount": 5,
    "maxMembers": 5,
    "isFull": true,
    "members": [
      {
        "userId": "e4b2d184-f376-476c-9226-64d852a39a01",
        "displayName": "CyberHawk",
        "isCaptain": true,
        "gameIdentifier": "76561198012345678"
      }
    ]
  }
  ```

---

### Epic 3: Tournament Configuration (US-07 – US-11)

#### 3.1 `POST /api/v1/tournaments`
* **Stories:** US-07, US-08, US-09, US-10, US-11
* **Description:** Organiser creates tournament. Map pool is assigned automatically.
* **Access:** Authenticated
* **Request Body:**
  ```json
  {
    "name": "KMA CS2 Autumn Cup 2026",
    "description": "Official faculty tournament. Overtime rules: MR3 $10k.",
    "discipline": "CS2",
    "slotLimit": 16,
    "registrationDeadlineAt": "2026-11-01T15:00:00Z",
    "checkInOpensAt": "2026-11-01T16:30:00Z",
    "startTimeAt": "2026-11-01T17:00:00Z",
    "cs2ServerConnect": "connect 194.44.12.5:27015; password kma_cup"
  }
  ```
* **Response `201 Created`:**
  ```json
  {
    "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "publicSlug": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "name": "KMA CS2 Autumn Cup 2026",
    "discipline": "CS2",
    "teamSize": 5,
    "format": "SINGLE_ELIMINATION",
    "slotLimit": 16,
    "status": "REGISTRATION_OPEN",
    "mapPool": ["Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II", "Vertigo"],
    "registeredTeamsCount": 0
  }
  ```
* **Errors:** `422 Unprocessable Entity` (`INVALID_SLOT_LIMIT`, `INVALID_SCHEDULE_ORDER`).

#### 3.2 `GET /api/v1/tournaments/{id}`
* **Stories:** US-11
* **Description:** Public landing page view of tournament information.
* **Access:** Public
* **Response `200 OK`:** Full tournament details, schedule timestamps, occupied slot count.
* **Errors:** `404 Not Found` (`TOURNAMENT_NOT_FOUND`).

---

### Epic 4: Registrations & Moderation (US-12 – US-14)

#### 4.1 `POST /api/v1/tournaments/{tournamentId}/register`
* **Stories:** US-12
* **Description:** Team captain registers full roster for the tournament.
* **Access:** Captain only
* **Request Body:**
  ```json
  { "teamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64" }
  ```
* **Response `201 Created`:**
  ```json
  {
    "registrationId": "5c9b13fa-8de1-460d-9b55-66778899aabb",
    "tournamentId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "teamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "status": "REGISTERED",
    "registeredAt": "2026-10-28T16:00:00Z"
  }
  ```
* **Errors:** `409 Conflict` (`ALREADY_REGISTERED`, `REGISTRATION_CLOSED`), `422 Unprocessable Entity` (`ROSTER_NOT_FULL`, `MISSING_GAME_IDENTIFIER`).

#### 4.2 `DELETE /api/v1/tournaments/{tournamentId}/registrations/{teamId}`
* **Stories:** US-13
* **Description:** Organizer moderates/removes an unwanted team before tournament start.
* **Access:** Organizer only
* **Response `204 No Content`**
* **Errors:** `403 Forbidden`, `404 Not Found`.

#### 4.3 `POST /api/v1/tournaments/{tournamentId}/lock-rosters`
* **Stories:** US-14
* **Description:** Locks rosters and creates immutable snapshots for all registered players.
* **Access:** Organizer (or system triggered before bracket generation)
* **Response `200 OK`:**
  ```json
  {
    "status": "LOCKED",
    "totalSnapshottedPlayers": 80
  }
  ```

---

### Epic 5: Check-In & Bracket Generation (US-15 – US-21)

#### 5.1 `POST /api/v1/tournaments/{tournamentId}/check-in`
* **Stories:** US-16
* **Description:** Captain confirms team attendance during active check-in window.
* **Access:** Captain only
* **Request Body:**
  ```json
  { "teamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64" }
  ```
* **Response `200 OK`:**
  ```json
  {
    "status": "CHECKED_IN",
    "checkedInAt": "2026-11-01T16:35:12Z"
  }
  ```
* **Errors:** `409 Conflict` (`CHECKIN_NOT_OPEN`, `CHECKIN_EXPIRED`, `CHECKIN_ALREADY_CONFIRMED`).

#### 5.2 `POST /api/v1/tournaments/{tournamentId}/generate-bracket`
* **Stories:** US-15, US-17, US-18, US-19, US-20
* **Description:** Closes check-in, purges no-shows, seeds present teams randomly with byes, builds single-elimination bracket.
* **Access:** Organizer or scheduled timer
* **Response `201 Created`:**
  ```json
  {
    "bracketId": "f12c3456-789a-bcde-f012-3456789abcde",
    "totalRounds": 4,
    "activeTeamsCount": 12,
    "byesAssignedCount": 4,
    "firstRoundMatchesCount": 8
  }
  ```

#### 5.3 `GET /api/v1/tournaments/{tournamentId}/bracket`
* **Stories:** US-21
* **Description:** Returns the public single-elimination bracket tree with rounds and matches.
* **Access:** Public
* **Response `200 OK`:**
  ```json
  {
    "tournamentId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "status": "IN_PROGRESS",
    "rounds": [
      {
        "roundNumber": 1,
        "roundName": "Round of 16",
        "matches": [
          {
            "matchId": "c01d2345-6789-abcd-ef01-23456789abcd",
            "matchOrder": 1,
            "status": "READY_CHECK",
            "team1": { "id": "7a30368b-...", "name": "Navi Junior KMA" },
            "team2": { "id": "9c40479c-...", "name": "KPI Titans" },
            "winnerTeamId": null,
            "isBye": false
          }
        ]
      }
    ]
  }
  ```

---

### Epic 6: Match Room & Pre-Match Flow (US-22 – US-26)

#### 6.1 `GET /api/v1/matches/{matchId}/room`
* **Stories:** US-22, US-23, US-25, US-26
* **Description:** Retrieves live match room snapshot (ready status, deadlines, bans, credentials).
* **Access:** Authenticated (Captains, Team Members, Organizer)
* **Response `200 OK`:**
  ```json
  {
    "matchId": "c01d2345-6789-abcd-ef01-23456789abcd",
    "status": "PICK_BAN",
    "discipline": "CS2",
    "readyDeadlineAt": "2026-11-01T17:10:00Z",
    "banDeadlineAt": "2026-11-01T17:08:45Z",
    "team1": {
      "id": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
      "name": "Navi Junior KMA",
      "isReady": true
    },
    "team2": {
      "id": "9c40479c-59b1-4f18-bb59-8395f13d8d65",
      "name": "KPI Titans",
      "isReady": true
    },
    "currentBanTurnTeamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "bannedMaps": [
      { "mapName": "Vertigo", "bannedByTeamId": "9c40479c-...", "order": 1 }
    ],
    "remainingMaps": ["Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II"],
    "decidedMap": null,
    "credentials": null
  }
  ```

#### 6.2 `POST /api/v1/matches/{matchId}/ready`
* **Stories:** US-23
* **Description:** REST alternative for marking team ready.
* **Access:** Captain only
* **Response `200 OK`:** `{ "isReady": true }`

#### 6.3 `POST /api/v1/matches/{matchId}/ban`
* **Stories:** US-25
* **Description:** REST alternative for banning a map during team's veto turn.
* **Access:** Captain only
* **Request Body:** `{ "mapName": "Mirage" }`
* **Response `200 OK`:** Updated remaining maps and next turn.
* **Errors:** `403 Forbidden` (`NOT_YOUR_BAN_TURN`), `409 Conflict` (`MAP_ALREADY_BANNED`).

#### 6.4 `GET /api/v1/matches/{matchId}/credentials`
* **Stories:** US-26
* **Description:** Fetches game server credentials (CS2 server string, Brawl Stars room code, or Chess game link).
* **Access:** Match participants & organizer
* **Response `200 OK`:**
  ```json
  {
    "discipline": "CS2",
    "cs2ConnectString": "connect 194.44.12.5:27015; password kma_cup",
    "decidedMap": "Inferno"
  }
  ```

---

### Epic 7: Score Submissions & Results (US-27 – US-29)

#### 7.1 `POST /api/v1/matches/{matchId}/scores`
* **Stories:** US-27, US-28
* **Description:** Captain enters match score. If both match, progresses winner.
* **Access:** Captain only
* **Request Body:**
  ```json
  {
    "team1Score": 13,
    "team2Score": 9
  }
  ```
* **Response `200 OK`:**
  ```json
  {
    "matchStatus": "COMPLETED",
    "winnerTeamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "bothSubmitted": true,
    "scoresMatched": true
  }
  ```

#### 7.2 `POST /api/v1/matches/{matchId}/chess/sync`
* **Stories:** US-29
* **Description:** Manually triggers or scheduled sync with Chess.com API.
* **Access:** System / Authenticated
* **Response `200 OK`:**
  ```json
  {
    "externalGameUrl": "https://www.chess.com/game/live/12345678",
    "whiteScore": 1.0,
    "blackScore": 0.0,
    "winnerTeamId": "e4b2d184-f376-476c-9226-64d852a39a01"
  }
  ```

---

### Epic 8: Disputes & AI Arbitration (US-30 – US-34)

#### 8.1 `POST /api/v1/matches/{matchId}/disputes`
* **Stories:** US-30
* **Description:** Captain opens dispute and uploads scoreboard screenshot evidence.
* **Access:** Captain only
* **Request:** `multipart/form-data`
  * `screenshot`: binary image file (PNG/JPG, max 10MB)
* **Response `201 Created`:**
  ```json
  {
    "disputeId": "d01e2345-6789-abcd-ef01-23456789abcd",
    "matchId": "c01d2345-6789-abcd-ef01-23456789abcd",
    "status": "OPEN",
    "screenshotUrl": "/api/v1/disputes/d01e2345-.../screenshot",
    "aiAnalysisStatus": "PENDING"
  }
  ```
* **Errors:** `400 Bad Request` (`INVALID_SCREENSHOT_FORMAT`), `409 Conflict` (`DISPUTE_ALREADY_OPEN`).

#### 8.2 `GET /api/v1/tournaments/{tournamentId}/disputes`
* **Stories:** US-31
* **Description:** Organizer dashboard list of all active disputes.
* **Access:** Organizer only
* **Response `200 OK`:** List of open disputes with match info and AI summary.

#### 8.3 `GET /api/v1/disputes/{disputeId}`
* **Stories:** US-32, US-33
* **Description:** Organizer views detailed dispute card with AI Vision verdict and roster check.
* **Access:** Organizer only
* **Response `200 OK`:**
  ```json
  {
    "disputeId": "d01e2345-6789-abcd-ef01-23456789abcd",
    "matchId": "c01d2345-6789-abcd-ef01-23456789abcd",
    "status": "OPEN",
    "screenshotUrl": "/api/v1/disputes/d01e2345-.../screenshot",
    "aiAnalysis": {
      "status": "SUCCESS",
      "recognizedScoreTeam1": 13,
      "recognizedScoreTeam2": 9,
      "confidence": 0.985,
      "allRosterNicknamesFound": true,
      "detectedNicknames": ["CyberHawk", "s1mple_kma", "b1t_junior", "electro", "perfecto"]
    }
  }
  ```

#### 8.4 `POST /api/v1/disputes/{disputeId}/resolve`
* **Stories:** US-34
* **Description:** Organizer makes final ruling (approving AI or setting manual score).
* **Access:** Organizer only
* **Request Body:**
  ```json
  {
    "winnerTeamId": "7a30368b-59b1-4f18-bb59-8395f13d8d64",
    "resolutionNotes": "Confirmed via official CS2 scoreboard."
  }
  ```
* **Response `200 OK`:** Match closed, winner progressed in bracket.

---

### Epic 9: Finalization & Standings (US-35 – US-36)

#### 9.1 `GET /api/v1/tournaments/{tournamentId}/standings`
* **Stories:** US-35
* **Description:** Public standings showing 1st, 2nd, and 3rd place teams.
* **Access:** Public
* **Response `200 OK`:**
  ```json
  {
    "tournamentId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "standings": [
      { "place": 1, "teamName": "Navi Junior KMA" },
      { "place": 2, "teamName": "KPI Titans" },
      { "place": 3, "teamName": "Lviv Lions" }
    ]
  }
  ```

#### 9.2 `GET /api/v1/tournaments/{tournamentId}/standings/export`
* **Stories:** US-36
* **Description:** Exports tournament final results in CSV format.
* **Access:** Public / Organizer
* **Response `200 OK`:** `text/csv; charset=utf-8` file download.

---

## 3. Real-Time WebSocket (STOMP) Specification

As defined in **ADR-0005**, WebSockets use STOMP over SockJS with a server-side heartbeat.

* **WebSocket Handshake URL:** `ws://localhost:8080/ws` (or `http://localhost:8080/ws` via SockJS)
* **Auth:** Session cookie forwarded on HTTP handshake.

### 3.1 Topics (Server ➔ Client Subscriptions)

| Destination | Purpose | Payload Schema |
| :--- | :--- | :--- |
| `/topic/matches/{matchId}/room` | Broadcasts live pre-match events (Ready Check, Map Veto bans, Game Credentials reveal). | `MatchRoomEventDTO` |
| `/topic/tournaments/{tournamentId}/bracket` | Broadcasts bracket node updates when matches start or complete. | `BracketUpdateEventDTO` |
| `/topic/tournaments/{tournamentId}/dashboard` | Real-time organizer dashboard counter updates (disputes, active matches). | `DashboardEventDTO` |
| `/user/queue/errors` | Delivers individual client error messages (e.g. invalid ban attempt). | `ProblemDetailDTO` |

### 3.2 Application Destinations (Client ➔ Server Actions)

#### A. Mark Ready: `/app/matches/{matchId}/ready`
```json
{ "isReady": true }
```
* **Server Action:** Records ready timestamp in PostgreSQL; if both ready, transitions to `PICK_BAN` and sets `ban_deadline_at = NOW() + 30s`. Broadcasts event to room.

#### B. Ban Map: `/app/matches/{matchId}/ban`
```json
{ "mapName": "Mirage" }
```
* **Server Action:** Validates captain's turn; inserts record into `match_bans`; computes next turn or decider map. Broadcasts updated veto state.

---

## 4. Internal AI Service Contract (Core ⟷ Python FastAPI)

Defined in **ADR-0006**: Spring Boot calls the Python service synchronously via HTTP.

* **Method:** `POST`
* **URL:** `http://ai-service:8000/api/v1/ai/analyze-dispute`
* **Content-Type:** `multipart/form-data`

### 4.1 Request

| Form Field | Type | Description |
| :--- | :--- | :--- |
| `screenshot` | Binary (Image) | Uploaded screenshot file from captain. |
| `discipline` | String | `CS2` or `BRAWL_STARS`. |
| `team1_name` | String | Name of Team 1. |
| `team2_name` | String | Name of Team 2. |
| `team1_roster`| String (JSON) | Array of expected display names and game handles for Team 1. |
| `team2_roster`| String (JSON) | Array of expected display names and game handles for Team 2. |

### 4.2 Response `200 OK`

```json
{
  "status": "SUCCESS",
  "recognizedScore": {
    "team1": 13,
    "team2": 9
  },
  "confidence": 0.985,
  "nicknamesDetected": ["CyberHawk", "s1mple_kma", "b1t_junior", "electro", "perfecto"],
  "allRosterNicknamesFound": true,
  "processingTimeMs": 1420
}
```

*Note: If image is blurry or cannot be parsed, AI returns `"status": "UNRECOGNIZED"`. Core handles this gracefully without throwing an exception.*

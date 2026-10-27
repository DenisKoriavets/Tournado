# ADR-0007: Authentication - Steam OpenID 2.0 + e-mail one-time link, JWT in an HttpOnly cookie

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

Users log in through Steam or e-mail (US-01, NFR-01). A repeat Steam login must open the same profile (BR-02). Creating, registering and all captain/organizer actions require authentication; permissions are checked per resource (organizer of this tournament, captain of this team/match - NFR-02, NFR-03). The Match Room WebSocket must know who the user is. The digest proposes separate profiles per login method and e-mail login via a one-time link (ASM-01, GAP-22).

## Decision drivers

- Steam login is the natural identity for CS2 players and gives a verified SteamID64.
- No paid identity provider, no passwords to store if avoidable.
- Works on localhost without CORS problems (same origin, ADR-0002).
- Same identity for REST and WebSocket.
- Survives backend restarts during the demo.

## Considered options

**Steam login**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Steam OpenID 2.0, small custom implementation** | The only login Steam offers; returns a verified SteamID64; free, no API key needed for login | Spring Security no longer supports OpenID 2.0, so ~100 lines of custom redirect + `check_authentication` verification |
| B. Third-party identity broker (Auth0, Keycloak with Steam plugin) | Ready-made | Paid or heavy to run; another service |

**E-mail login**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. One-time magic link** | No passwords stored; link doubles as e-mail verification (US-01 Sc.2) | Needs SMTP - solved locally with Mailpit (fake inbox in Docker) |
| B. E-mail + password | Familiar | Password hashing, reset flow, verification flow - extra stories not in scope |

**Session**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Signed JWT in an HttpOnly, SameSite=Lax cookie** | Stateless - survives backend restarts; same cookie authenticates REST and the WebSocket handshake; not readable by JavaScript | No server-side revocation before expiry |
| B. Server session (Spring Session) | Easy revocation | In-memory sessions are lost on restart; JDBC sessions add tables |
| C. JWT in `localStorage` + `Authorization` header | Common in tutorials | Readable by any injected script; WebSocket handshake cannot send custom headers from browsers |

## Decision

- **Steam:** OpenID 2.0 flow implemented in the `identity` module: redirect to `steamcommunity.com/openid/login`, verify the response with `check_authentication`, extract SteamID64, find or create the profile (BR-02) and pre-fill the Steam ID field.
- **E-mail:** one-time link valid for a short time (≈ 15 min), single use. Locally e-mails go to **Mailpit**; a real SMTP server can be configured through environment variables.
- **Steam and e-mail logins create separate profiles** in the MVP; no account linking (ASM-01).
- **Session:** the core issues a signed JWT (HS256, secret from `.env`) in an `HttpOnly`, `SameSite=Lax` cookie, lifetime ≈ 7 days; logout clears the cookie. The WebSocket handshake reads the same cookie.
- **Authorization:** Spring Security for authentication; per-resource checks (organizer of tournament X, captain of team Y) in the service layer of each module, always on the server (NFR-03).

## Consequences

**Positive**
- No passwords stored; no paid provider.
- Users stay logged in across backend restarts during development and the demo.
- One identity mechanism for REST and WebSocket.

**Negative**
- Custom Steam OpenID code must be written and tested.
- Tokens cannot be revoked before expiry; acceptable for the MVP (no admin role, no account bans).
- One person with Steam and e-mail logins gets two profiles (ASM-01).

**Risks**
- Leaked JWT secret allows forging sessions. Mitigation: secret only in `.env`, different per environment.
- Open redirect / replay in the Steam callback. Mitigation: validate `openid.return_to` and always verify with Steam.

## Related

ADR-0002, ADR-0003, ADR-0009; US-01, US-02; BR-01, BR-02; NFR-01…NFR-04; GAP-22; ASM-01.

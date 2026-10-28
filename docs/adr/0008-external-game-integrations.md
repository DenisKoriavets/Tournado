# ADR-0008: External game integrations - Chess.com public API only

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The MVP supports three games, but the critical analysis in practice 2 concluded that controlling game servers or relying on closed game APIs is unrealistic for a semester. Scope keeps automatic result import only for chess (Scope 4.1.3б, US-29); CS2 and Brawl Stars results come from both captains (US-27, US-28). Players still need connection details: a CS2 connect string, a Brawl Stars friendly-room code, a Chess.com link (US-26; GAP-01…GAP-03, ASM-16…ASM-18).

## Decision drivers

- Zero budget and no API keys that require approval or fixed IPs.
- Every integration is a risk for the live demo.
- Results must not be closed by an unreliable external source.

## Considered options

| Integration | Options | Decision |
| :--- | :--- | :--- |
| **Chess.com results** | (a) Public read-only API, polling monthly archives `api.chess.com/pub/player/{username}/games/{YYYY}/{MM}`; (b) captains enter the result | **(a)** - no key, free, read-only; matches US-29 |
| **CS2 server / results** | (a) Own dedicated server + Game State Integration / plugins; (b) Steam Web API; (c) organizer enters a connect string, captains enter the score | **(c)** - (a) needs paid hosting and game-server expertise; (b) does not expose custom match results |
| **Brawl Stars** | (a) Supercell API (battle log); (b) captain enters the room code, captains enter the score | **(b)** - the Supercell API key is bound to fixed IP addresses and friendly battles are not reliably returned |
| **Steam** | Login only (ADR-0007) | No Steam Web API calls in MVP |

## Decision

- **Chess.com:** the `integrations` module polls the public API from the core with a Spring scheduled job (every ≈ 30 s per open chess match, sequential requests, a descriptive `User-Agent`). It looks for the first finished game between the two snapshotted nicknames that started after the Match Room pre-match ended (ASM-18). Win → close and advance; draw / not found / API unavailable → status per US-29 and organizer decision (ASM-19). The client never calls Chess.com directly.
- **CS2:** the organizer enters one or more server connect strings when creating the tournament (ASM-16); the Match Room shows the assigned one. No server control.
- **Brawl Stars:** after bans, one captain enters the friendly-room code (ASM-17).
- All external calls go through one adapter per provider with timeouts; failures never close a match (NFR-17).

## Consequences

**Positive**
- Only one external dependency in the whole MVP, without any key.
- The live demo cannot fail because of a game server or API key.

**Negative**
- CS2 and Brawl Stars depend on honest captain input; disputes and AI hints (ADR-0006) cover conflicts.
- Polling adds a delay of up to ~30 s before a chess result appears.

**Risks**
- Chess.com archive updates may lag or rate-limit (HTTP 429). Mitigation: sequential polling with back-off; "awaiting result" status; organizer manual decision.

**When to revisit**
- Supercell or Steam APIs only if a legal, free and stable way to fetch friendly-match results appears.

## Related

ADR-0003, ADR-0006, ADR-0007; US-26, US-27, US-29; NFR-17; GAP-01…GAP-04; ASM-16…ASM-19.

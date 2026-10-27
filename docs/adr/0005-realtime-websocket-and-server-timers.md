# ADR-0005: Real-time via Spring WebSocket (STOMP) and server-side deadlines in PostgreSQL - no Redis

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The Match Room is the main real-time feature: both captains must see the same state (NFR-15), the Ready Check has a 10-minute timer (BR-36, BR-37) and map bans have a 30-second turn timer with a random auto-ban on timeout (BR-38). Many other transitions are time-based and must happen with no user online: roster lock, check-in open/close, exclusion, bracket generation, Chess.com polling (NFR-13). A last-second action must have exactly one outcome, judged by the server clock (NFR-07). The organizer dashboard should show new disputes immediately (NFR-16); the public bracket may update on reload.

The team's initial sketch used **Redis 7** for Match Room state, pick/ban timers and a task queue (Redis Streams) for the AI worker.

## Decision drivers

- Correctness of timers under restarts and at the exact boundary (NFR-07, NFR-13).
- Minimal number of services to run, configure and debug (5 students, local run, zero budget).
- Scale: one backend instance; at most 16 concurrent matches and tens to hundreds of WebSocket clients.
- Built-in Spring support preferred over new infrastructure.

## Considered options

**Transport**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Spring WebSocket + STOMP, built-in simple broker** | Built into Spring; topics per match (`/topic/matches/{id}`); mature JS client (`@stomp/stompjs`); authentication through the same cookie | In-memory broker works for one instance only |
| B. Server-Sent Events + REST | Simpler protocol, one-way | Captain actions still need separate REST calls; two mechanisms instead of one |
| C. Short polling every 1-2 s | Trivial | Wasteful; visible lag in a 30-second ban turn |
| D. STOMP broker relay to RabbitMQ / Redis pub/sub | Works across many instances | Extra service with no benefit at one instance |

**Timers and Match Room state**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Deadlines stored in PostgreSQL + server checks** | State survives restarts; one source of truth; boundary decided by comparing `now()` with the stored deadline in a transaction | A scheduler must scan for expired deadlines (cheap at our scale) |
| B. In-memory Java timers only | Very simple | All running timers are lost on restart; matches hang forever |
| C. Redis keys with TTL + keyspace notifications (team sketch) | Fast, natural "expiry" concept | Expiry events are fire-and-forget: if the backend is restarting at that moment the event is lost and the ban turn never ends; state split between Redis and PostgreSQL; one more service |

## Decision

1. **Transport:** Spring WebSocket with STOMP and the built-in simple broker. Clients subscribe to per-match and per-tournament topics; commands from captains (ready, ban, score) go over REST, and the server broadcasts the resulting state over STOMP. REST for commands keeps one validation and error path (the unified error format).
2. **State and timers:** the Match Room state, including deadlines such as `ready_deadline_at` and `ban_turn_deadline_at`, is stored in PostgreSQL (ADR-0004).
   - On every captain action the server compares the server clock with the stored deadline inside the same transaction - this decides last-second cases (NFR-07, US-24 Sc.3).
   - A Spring `@Scheduled` job runs every second, finds expired deadlines and applies the timeout transition (technical loss, random auto-ban), then broadcasts the new state. The same mechanism drives the tournament schedule (roster lock, check-in, bracket generation).
   - Transitions are idempotent and guarded by optimistic locking, so a user action and the scheduler cannot both apply.
   - The client only displays a countdown from the server-provided deadline; it never decides.
3. **No Redis in the MVP.** Not for state, not for timers, not as a queue (the AI call is synchronous, ADR-0006).
4. Code hides the mechanism behind interfaces (`MatchEventPublisher`, `DeadlineScheduler`) so a different broker or scheduler can be plugged in later without touching business logic.

## Consequences

**Positive**
- One fewer service to run, configure and debug; three runtimes + one database in total.
- Timers survive restarts: after a restart the scheduler immediately applies everything that expired.
- One source of truth for match state; tests can control time through an injected `Clock`.

**Negative**
- The scheduler adds a small constant DB load (one indexed query per second) - negligible at our scale.
- Timeout transitions can be up to ~1 second late; acceptable for 30-second and 10-minute timers because the decision is still based on the stored deadline.
- The simple broker does not scale beyond one instance.

**When to revisit (conditions for bringing Redis or RabbitMQ back)**
- Running two or more backend instances (needs a shared broker for WebSocket events).
- Heavy background jobs that need a queue with retries (e.g. Post-MVP AI rules generation, batch screenshot checks).
- Hundreds of concurrent tournaments where caching hot pages becomes necessary.

## Related

ADR-0003, ADR-0004, ADR-0006; BR-36, BR-37, BR-38; NFR-07, NFR-13, NFR-15, NFR-16; US-22…US-25.

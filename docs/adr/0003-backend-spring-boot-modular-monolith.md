# ADR-0003: Backend - Java 21 + Spring Boot 3 modular monolith

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The core holds almost all business rules of the MVP: authentication and profiles, teams and rosters, tournament configuration and lifecycle, registration and check-in, bracket generation, Match Room flow, score confirmation, disputes and CSV export (BR-01…BR-53). Four of the five team roles write code here. The team voted for Java / Spring Boot for the core; the AI part is a separate Python service (ADR-0006).

## Decision drivers

- Team vote and existing familiarity (Java / Spring).
- Strong consistency for concurrency-sensitive rules: last slot, duplicates, simultaneous score submissions (NFR-05, NFR-06, NFR-08) - easiest inside one process and one database transaction.
- Scheduled server-side jobs (NFR-13) and WebSocket support (ADR-0005) without extra infrastructure.
- Four people working in parallel without stepping on each other.
- Simple local startup.

## Considered options

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Spring Boot modular monolith** (one deployable, packages per business module) | One process, one DB transaction for cross-module rules; built-in scheduling, WebSocket, security, validation; clear module ownership per role | Discipline needed to keep module boundaries; one module failure restarts the whole app |
| B. Spring Boot microservices (auth, tournaments, matches…) | Independent deploys and scaling | Distributed transactions for rules like "advance winner + close match"; service discovery, more containers; far beyond a semester for 4 developers |
| C. Node.js / NestJS monolith | Same language as the frontend | Against the team vote; weaker fit for the team's Java experience |
| D. Python (FastAPI / Django) for the whole backend | One language with the AI service | Lost the team vote (1 of 5); team experience is stronger in Java for the core |

## Decision

**Option A.** One Spring Boot 3 application on **Java 21 (LTS)**, built with **Maven**, organised by business module (package-by-feature):

| Module (package) | Main owner | Covers |
| :--- | :--- | :--- |
| `identity` | Core Platform & Auth | Login, profiles, game identifiers (US-01, US-02) |
| `teams` | Core Platform & Auth | Teams, invite links, rosters, roster lock (US-03…US-06, US-14) |
| `tournaments` | Tournament Engine | Configuration, schedule, registration, check-in, lifecycle, completion, CSV (US-07…US-13, US-15…US-17, US-35, US-36) |
| `bracket` | Tournament Engine | Generation, random seeding, byes, advancement (US-18…US-21, US-28) |
| `matches` | Game Integrations & Match Flow | Match Room, Ready Check, map ban, credentials, score entry, technical loss (US-22…US-27) |
| `integrations` | Game Integrations & Match Flow | Chess.com import (US-29, ADR-0008) |
| `disputes` | AI & Automation | Disputes, evidence, AI client, organizer decision (US-30…US-34) |
| `common` | shared | Error format, time source, security helpers |

Rules:
- Inside each module: `api` (REST/WebSocket controllers + DTOs), `domain` (entities, rules), `infra` (repositories, external clients).
- Modules talk to each other through public service interfaces or Spring application events, never through another module's repositories.
- All business rules and permission checks live in the core, never only in the UI (NFR-03).
- REST + JSON for the client API; exact endpoints and the unified error format are defined in the technical contracts (next step).

## Consequences

**Positive**
- Cross-module invariants (e.g. close match + advance winner + complete tournament, BR-42, BR-51) run in one transaction.
- Each role owns a package, which reduces merge conflicts.
- Spring gives scheduling, WebSocket, security and validation without new infrastructure.

**Negative**
- Spring Boot startup is slower than lighter frameworks; acceptable for local development.
- Module boundaries are a convention, not enforced by the build. (Spring Modulith could enforce them later.)

**Risks**
- A "big ball of mud" if modules access each other's tables. Mitigation: code review rule + `CLAUDE.md` rule.

**When to revisit**
- Only if a module needs independent scaling or a different release cycle - not expected within the course.

## Related

ADR-0004, ADR-0005, ADR-0006, ADR-0007, ADR-0008; NFR-03, NFR-05, NFR-06, NFR-08, NFR-13.

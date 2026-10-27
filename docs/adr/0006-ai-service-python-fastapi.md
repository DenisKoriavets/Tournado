# ADR-0006: AI module - separate Python / FastAPI service, synchronous HTTP, pluggable vision provider

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

When captains disagree on a score, a captain opens a dispute with a scoreboard screenshot (US-30). The AI must recognise the score per team with a confidence value (US-32) and compare the nicknames on the screenshot with the locked roster (US-33). The AI is advisory only: it never changes a match, bracket or team (NFR-20); if it fails, the card shows "not recognised" and the organizer decides manually (NFR-19, ASM-22). Screenshots are uploaded by an interested party, so their text is untrusted input (NFR-22). AI rules generation is Post-MVP (Scope 1.3.1).

The team voted for a Python AI module. The initial sketch connected it to the core through a Redis Streams queue.

Expected load: a few disputes per tournament, one screenshot each.

## Decision drivers

- Team vote: AI module in Python, owned by the AI & Automation role.
- Zero budget: no paid model APIs.
- A dispute card must never get stuck because of the AI (NFR-19).
- Structured, validated output (score per team, nicknames, confidence) - never free text treated as a decision.
- Fewest moving parts (ADR-0005 drops Redis).

## Considered options

**Service boundary**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Separate Python / FastAPI service** | Python ecosystem for images and model SDKs (Pillow, Pydantic, provider SDKs); clear ownership; can be developed and tested alone | One more container; network call from core |
| B. Call the vision API directly from Java | No extra service | Against the team vote; image pre-processing and prompt experiments are slower in Java |
| C. Python embedded in the core (Jython / subprocess) | Single deployable | Fragile, hard to debug |

**Communication with the core**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Synchronous HTTP (REST + JSON) with a timeout** | Simplest; errors visible immediately; no broker | The core waits up to the timeout (done off the request thread, see Decision) |
| B. Redis Streams / message queue (team sketch) | Retries, decoupling, back-pressure | Needs Redis (rejected in ADR-0005); queue semantics are overkill for a few disputes per tournament |

**Model provider (zero budget)**

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Hosted vision model with a free tier (e.g. Google Gemini API) behind an adapter** | Good OCR quality on scoreboards; no GPU needed; free within rate limits | Requires an API key per developer; free-tier rate limits; free-tier data may be used by the provider - no personal data beyond nicknames is sent |
| B. Local open-weights vision model via Ollama | Fully offline, no key | Needs a strong laptop (RAM/GPU); slower; weaker on small scoreboard text |
| C. Classic OCR (Tesseract) + parsing rules | Free, offline | Fragile across three different scoreboard layouts, phone photos, resolutions |

## Decision

- A separate **Python 3.12 + FastAPI** service in `ai-service/`.
- The core calls it **synchronously over HTTP** with a timeout (≈ 30 s). The call runs asynchronously inside the core (after the dispute is saved), so opening a dispute never waits for the AI; the dispute card updates when the analysis arrives (ADR-0005 topic). No queue, no Redis.
- The core sends: discipline, the screenshot bytes, the two locked rosters with in-game nicknames (ASM-10). The AI returns a validated JSON object: score per team (side identified by nickname match, ASM-22), recognised nicknames with match flags, confidence, and a status `recognised | not_recognised`.
- The vision provider is behind an adapter interface with three implementations selected by an environment variable:
  - `gemini` (default for demo) - hosted free tier, key in `.env`;
  - `ollama` - local model for members with strong hardware;
  - `mock` - deterministic fake answers for development, tests and the walking skeleton; needs no key.
- Safety rules: the prompt instructs the model to extract data only; output is parsed with a strict Pydantic schema; anything invalid, a timeout or a provider error becomes `not_recognised` (NFR-19, NFR-22). The AI never calls back into the core.

## Consequences

**Positive**
- The AI owner works independently with a clear HTTP contract and a mock mode.
- A failing or rate-limited model can never block a dispute; manual resolution always works.
- Switching providers is a config change.

**Negative**
- An extra container and an internal HTTP contract to maintain.
- The free tier has rate limits; acceptable for a few disputes per tournament.

**Risks**
- Recognition quality on phone photos and three different scoreboards. Mitigation: human-in-the-loop decision (US-34); test set of real screenshots per game.
- API key leakage. Mitigation: keys only in `.env` (git-ignored), never in code or logs.

**When to revisit**
- A queue with retries becomes useful only for heavy batch jobs (Post-MVP rules generation, bulk checks).

## Related

ADR-0003, ADR-0005, ADR-0009; US-30…US-34; NFR-19, NFR-20, NFR-22; GAP-07, GAP-08, GAP-33; ASM-10, ASM-22.

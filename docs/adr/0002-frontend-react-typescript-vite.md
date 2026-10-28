# ADR-0002: Frontend - React + TypeScript + Vite single-page app

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

The frontend covers the public tournament page and bracket (US-11, US-21), the organizer screens (tournament creation, team list, live dashboard, dispute card - US-07…US-13, US-31, US-34) and the real-time Match Room for captains (US-22…US-27). It is built mainly by one person (Frontend role). There is no SEO or server-rendering requirement: the public pages are shared by direct link in Discord / Telegram (US-11).

## Decision drivers

- One developer must deliver all screens within a semester.
- Real-time Match Room needs a STOMP-over-WebSocket client (ADR-0005).
- Bracket visualisation for 8 / 16 / 32 slots (NFR-29).
- Strong typing to keep the client in sync with the API contracts.
- AI assistants should generate reliable code for the chosen stack.
- Must run locally and be served from a container (ADR-0009).

## Considered options

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. React + TypeScript + Vite (SPA)** | Largest ecosystem (bracket components, `@stomp/stompjs`, form libraries); fast dev server with proxy for `/api` and `/ws`; output is static files; best AI code-generation quality | Routing, data fetching and state are separate libraries to pick |
| B. Next.js | React plus routing and SSR out of the box | Introduces a second server (Node) next to Spring; SSR not needed; two places where backend logic could drift |
| C. Vue 3 + Vite | Simple, good docs | Smaller ecosystem for brackets; team voted React |
| D. Angular | Opinionated, built-in DI and forms | Heaviest learning curve; slow for one developer in one semester |
| E. Server-rendered templates in Spring (Thymeleaf) | No separate frontend build | Poor fit for a real-time interactive Match Room; frontend owner works in Java code |

## Decision

**Option A.** A single-page application built with **React + TypeScript + Vite**.

Conventions:
- TypeScript in `strict` mode; API request/response types follow `docs/` contracts and are not invented in the client.
- Routing with React Router; server data through a thin typed API client (`fetch`), no global state library unless a real need appears.
- Real-time Match Room and organizer dashboard via `@stomp/stompjs` (ADR-0005). The public bracket refreshes on reload, which the requirements allow (NFR-16).
- The bracket is a custom component (CSS grid / SVG); a third-party bracket library may be used if it supports Byes and 8/16/32 slots.
- In development the Vite dev server proxies `/api` and `/ws` to the core service, so the browser sees one origin and the auth cookie works without CORS (ADR-0007).
- In Docker the built static files are served by a small Nginx container that also proxies `/api` and `/ws`.

## Consequences

**Positive**
- Fastest path to a working UI for one developer; plenty of examples for every screen.
- Same-origin setup removes CORS and cookie problems.

**Negative**
- The team must agree on a small set of libraries (router, forms) instead of getting them from a framework.
- No server rendering; acceptable because there is no SEO requirement.

**Risks**
- Client-side checks could be mistaken for security. Mitigation: every rule is enforced by the core (NFR-03); the UI only hides buttons.

## Related

ADR-0001, ADR-0005, ADR-0007, ADR-0009; NFR-03, NFR-16, NFR-29.

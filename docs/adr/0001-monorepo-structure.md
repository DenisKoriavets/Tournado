# ADR-0001: Monorepo with one folder per runtime

- **Status:** Accepted
- **Date:** 2026-10-27

## Context

Tournado consists of three runtimes in two languages plus a database: a React web client, a Java core service and a Python AI service (see ADR-0002, ADR-0003, ADR-0006). The course grades a single GitHub repository, and the project must start locally with 1-2 commands. Five people work in parallel; the API and data contracts (next step) must stay in sync between frontend, core and AI service.

## Decision drivers

- One repository link is submitted and reviewed.
- One-command startup (ADR-0009) needs all services side by side.
- Contracts change often during the semester; a change should be visible in one pull request.
- AI assistants in the IDE work better when docs and code are in the same tree (`CLAUDE.md` + `docs/`).
- Minimal tooling overhead.

## Considered options

| Option | Pros | Cons |
| :--- | :--- | :--- |
| **A. Monorepo, plain folders per runtime** | One link; one PR can change contract + backend + frontend; one `docker-compose.yml`; docs next to code | Mixed languages in one tree; CI must build only what changed (not needed for MVP) |
| B. Separate repositories per service | Clear ownership; independent histories | 3+ links to submit; contract drift between repos; startup needs cloning several repos |
| C. Monorepo with a build orchestrator (Nx, Turborepo, Gradle composite) | Cached builds, dependency graph | Extra tool to learn; Nx/Turborepo are JS-centric and do not help the Java/Python parts |

## Decision

**Option A.** One repository with this top-level layout:

```
/
├── frontend/          React + TypeScript + Vite (ADR-0002)
├── core/              Java 21 + Spring Boot 3, Maven (ADR-0003)
├── ai-service/        Python 3.12 + FastAPI (ADR-0006)
├── docs/              requirements, analysis, adr/, contracts
├── prompts/           prompts used in each practice
├── docker-compose.yml one-command local run (ADR-0009)
├── .env.example       all configuration keys with safe defaults
├── CLAUDE.md          rules for AI assistants
└── README.md          how to run and verify
```

Each runtime folder is self-contained: its own build file (`pom.xml`, `package.json`, `pyproject.toml`), `Dockerfile` and README section. No build orchestrator.

## Consequences

**Positive**
- One link, one clone, one `docker compose up`.
- A contract change and its implementation land in the same PR, so reviewers see the whole picture.

**Negative**
- Each member needs Java, Node and Python tooling only if they run services natively; Docker removes that need for running (ADR-0009).
- `.gitignore` must cover three ecosystems (`target/`, `node_modules/`, `.venv/`, `__pycache__/`).

**Risks**
- Merge conflicts in shared files (`docker-compose.yml`, `docs/`). Mitigation: one owner per shared file, small PRs, same branching convention as in practice 3.

## Related

ADR-0002, ADR-0003, ADR-0006, ADR-0009.

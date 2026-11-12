# Broadcast Studio UI Refactoring & AI Slop Elimination Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Overhaul the Tournado frontend into a high-performance "Broadcast Studio" esports arena interface by eliminating all AI slop, academic rubric markers (`US-*`, roadmap blocks, header debug box), hardcoded demo fallbacks, and replacing them with vector SVG iconography, a slide-over drawer, and a 2-step creation wizard.

**Architecture:** React 19 + TypeScript + Vite + Tailwind CSS. Component-driven decomposition with pure vector SVG icons, responsive slide-over drawer for tournament details, two-step creation modal wizard with client validation mirroring RFC 7807 backend invariants, and honest PostgreSQL persistence with an elegant empty state.

**Tech Stack:** React 19, TypeScript 5.6, Tailwind CSS 3.4, Vite 6, Vitest, React Testing Library, Spring Boot 3.3.5 Core, PostgreSQL 16.

**Spec:** `docs/superpowers/specs/2026-11-12-broadcast-studio-ui-refactoring-design.md`

## Global Constraints

- **Zero AI Slop & Academic Rubric Leakage:** Strictly forbidden to render `US-07..US-11`, `Vertical Slice 1`, `36 атомарних User Stories`, `Production Ready`, or backend debug ping strings (`Backend: UP | DB: UP`) in the UI.
- **Zero System Emojis:** Replace all emojis (`🌪️`, `🏆`, `🎮`, `📅`, etc.) with pure SVG vector icons.
- **Contract Fidelity:** Must strictly adhere to `docs/contracts/api-contracts.md` (Sections 3.1 & 3.2) and `docs/contracts/error-format.md`.
- **Honest Persistence:** Do not fallback to `DEMO_TOURNAMENTS` in client memory; display authentic database content or a clean Empty State with CTA.
- **Deterministic Quality:** All tests in `frontend/` must pass (`npm test`) and `npm run build` must compile with 0 TypeScript errors.

---

### Task 1: Vector Icon Library & Design Tokens

**Files:**
- Create: `frontend/src/components/icons/Icons.tsx`
- Modify: `frontend/src/types/tournament.ts`

**Interfaces:**
- Produces: Exported SVG icons (`TrophyIcon`, `GamepadIcon`, `CalendarIcon`, `UsersIcon`, `CopyIcon`, `CheckIcon`, `FilterIcon`, `CloseIcon`, `ChevronRightIcon`, `ServerIcon`, `ShieldIcon`, `SparklesIcon`, `ClockIcon`, `ArrowLeftIcon`, `PlusIcon`) and modernized `DISCIPLINE_CONFIG` / `STATUS_CONFIG` without emojis.

- [ ] **Step 1: Create `frontend/src/components/icons/Icons.tsx`** with accessible SVG icon components accepting `className` props.
- [ ] **Step 2: Update `frontend/src/types/tournament.ts`** replacing emoji properties with icon references and Broadcast Studio theme color classes.
- [ ] **Step 3: Verify TypeScript compilation** (`npx tsc --noEmit`).
- [ ] **Step 4: Commit changes** with message `feat(frontend): add vector icon set and modernize tournament design tokens`.

---

### Task 2: Refactor `TournamentCard.tsx`

**Files:**
- Modify: `frontend/src/components/TournamentCard.tsx`

**Interfaces:**
- Consumes: `Tournament` from `../types/tournament`, SVG icons from `./icons/Icons`.
- Produces: `TournamentCard: React.FC<TournamentCardProps>`.

- [ ] **Step 1: Refactor `TournamentCard.tsx`** into Broadcast Studio style:
  - Header: game-specific vector icon, short name, format pill (`5v5`), and animated pulse status badge.
  - Body: Tournament name with hover color transition, 2-line truncated description.
  - Metrics: Slots bar with exact count and percentage (`0 / 16`), start datetime with calendar icon, and registration deadline countdown.
  - Action footer: "Детальніше" with chevron right icon.
- [ ] **Step 2: Verify component renders without errors** via `npx tsc --noEmit`.
- [ ] **Step 3: Commit changes** with message `feat(frontend): refactor TournamentCard to broadcast studio design`.

---

### Task 3: Create `TournamentDrawer.tsx` (Slide-over Details Panel)

**Files:**
- Create: `frontend/src/components/TournamentDrawer.tsx`
- Delete: `frontend/src/components/TournamentDetailsModal.tsx`

**Interfaces:**
- Consumes: `Tournament | null`, `isOpen: boolean`, `onClose: () => void`.
- Produces: `TournamentDrawer: React.FC<TournamentDrawerProps>`.

- [ ] **Step 1: Implement `TournamentDrawer.tsx`**:
  - Backdrop overlay with frosted blur (`backdrop-blur-sm`).
  - Right-aligned slide-over drawer with smooth transition.
  - Header with game badge, status pill, tournament name, and close button.
  - Tab navigation: `Огляд`, `Розклад та слоти`, `Сервер та карти`.
  - Tab 1: Full description, shareable public link with one-click copy and feedback indicator, slots capacity bar.
  - Tab 2: Chronological schedule timeline (Registration Deadline -> Check-in Opens -> Start Time) with relative time hints.
  - Tab 3: For CS2, copyable `connect IP:PORT` code snippet; official map pool grid with map pills.
  - Keyboard accessibility: Escape key to close, focus trap.
- [ ] **Step 2: Remove legacy `TournamentDetailsModal.tsx`** and update all imports.
- [ ] **Step 3: Verify build** (`npx tsc --noEmit`).
- [ ] **Step 4: Commit changes** with message `feat(frontend): implement slide-over TournamentDrawer replacing modal`.

---

### Task 4: Refactor `TournamentFormModal.tsx` (Two-Step Creation Wizard)

**Files:**
- Create: `frontend/src/components/TournamentFormModal.tsx`
- Delete: `frontend/src/components/TournamentForm.tsx`
- Modify: `frontend/src/components/TournamentForm.test.tsx`

**Interfaces:**
- Consumes: `isOpen: boolean`, `onClose: () => void`, `onSuccess: (tournament: Tournament) => void`.
- Produces: `TournamentFormModal: React.FC<TournamentFormModalProps>`.

- [ ] **Step 1: Write / adapt test in `TournamentForm.test.tsx`** for multi-step wizard interaction.
- [ ] **Step 2: Implement `TournamentFormModal.tsx`**:
  - Step indicator: "1. Дисципліна та формат" -> "2. Слоти та розклад".
  - Step 1: Discipline selection cards (CS2, Brawl Stars, Chess.com), tournament title input, optional description. "Далі до розкладу" button.
  - Step 2: Slot limit selector pills (8, 16, 32), quick schedule presets button ("Заповнити рекомендований розклад"), datetime pickers with live validation (`deadline < check-in < start`), optional CS2 server connect.
  - Summary preview block before submission.
  - RFC 7807 error banner mapping backend `invalidParams` into human-friendly inline messages.
- [ ] **Step 3: Run Vitest tests** (`npm test -- TournamentForm`).
- [ ] **Step 4: Commit changes** with message `feat(frontend): implement two-step TournamentFormModal wizard`.

---

### Task 5: Refactor `App.tsx` and `TournamentList.tsx`

**Files:**
- Modify: `frontend/src/components/TournamentList.tsx`
- Modify: `frontend/src/components/TournamentList.test.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Connects: Live Spring Boot Core API (`/api/v1/tournaments`), `TournamentCard`, `TournamentDrawer`, `TournamentFormModal`.

- [ ] **Step 1: Refactor `TournamentList.tsx`**:
  - Discipline filter tabs (`Всі`, `CS2`, `Brawl Stars`, `Chess.com`) with count chips.
  - Search input with clear button.
  - Responsive grid layout (`grid-cols-1 md:grid-cols-2 lg:grid-cols-3`).
  - Empty State: When no tournaments match or exist, show a sleek empty illustration, friendly Ukrainian message, and a "Створити турнір" button.
  - Loading skeleton cards matching card dimensions.
- [ ] **Step 2: Refactor `App.tsx`**:
  - Sleek navbar: Tournado brand logo + links ("Турніри", "Матчі", "Рейтинги") + primary CTA "Створити турнір".
  - Strip out `Vertical Slice 1`, `Production Ready`, and header health check debug panel.
  - Strip out the 5-step roadmap card block.
  - Remove `DEMO_TOURNAMENTS` fallback in state — initialize with empty array, load real PostgreSQL data via `loadTournaments()`.
  - Non-intrusive offline toast if Core API fails, with a "Спробувати знову" button.
  - Integrate `TournamentDrawer` and `TournamentFormModal`.
- [ ] **Step 3: Run Vitest tests** (`npm test -- TournamentList`).
- [ ] **Step 4: Commit changes** with message `feat(frontend): refactor App and TournamentList to broadcast studio layout`.

---

### Task 6: Deterministic Frontend Test Suite & Build Verification

**Files:**
- Verify: `frontend/src/components/*.test.tsx`
- Verify: `frontend/src/utils/date.test.ts`

- [ ] **Step 1: Run all frontend tests** (`npm test`). Ensure 100% pass rate.
- [ ] **Step 2: Run production TypeScript build** (`npm run build`). Ensure 0 errors, clean output.
- [ ] **Step 3: Commit any test adjustments** with message `test(frontend): verify all test suites and production build`.

---

### Task 7: Update Course Deliverables (`prompts/`, `README.md`, PR Review)

**Files:**
- Modify: `prompts/5.1_code_generation.md`
- Modify: `prompts/5.2_fixing_and_refactoring.md`
- Modify: `prompts/5.3_ai_code_review.md`
- Modify: `README.md`

- [ ] **Step 1: Update `prompts/5.1_code_generation.md`** with the refined prompt for the professional UI and backend integration.
- [ ] **Step 2: Update `prompts/5.2_fixing_and_refactoring.md`** adding Issue 5: Elimination of AI Slop & Refactoring to Broadcast Studio UI.
- [ ] **Step 3: Update `prompts/5.3_ai_code_review.md`** adding audit criteria for professional UI quality and absence of academic leaks.
- [ ] **Step 4: Update `README.md`** with a crisp, step-by-step 3-minute Live Demo guide matching the teacher's grading rubric.
- [ ] **Step 5: Post an AI Code Review comment to GitHub PR #10** using GitHub MCP.
- [ ] **Step 6: Commit all documentation updates** with message `docs: update prompts, readme demo guide, and pr review report`.

---

### Task 8: End-to-End Live Stack Verification

- [ ] **Step 1: Rebuild and restart the frontend Docker container** (`docker compose up --build -d frontend`).
- [ ] **Step 2: Verify `http://localhost:3000`** in browser.
- [ ] **Step 3: Test tournament creation end-to-end** via the new UI, verify PostgreSQL persistence, and confirm instant appearance in the grid.

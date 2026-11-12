# Design Spec: Broadcast Studio UI Refactoring & AI Slop Elimination

**Date:** 2026-11-12  
**Topic:** Frontend UI/UX Refactoring for Tournado Vertical Slice 1 (US-07..US-11)  
**Status:** Approved by Human Partner  
**Target:** `frontend/src/` (Components, Design System, Types, Views, Tests)

---

## 1. Problem Statement & Motivation

The initial prototype implementation of Vertical Slice 1 contained excessive "AI slop" and academic homework artifacts exposed directly to end users:
1. **Academic/Rubric Junk:** Tags such as `Vertical Slice 1 • US-07..US-11`, `Production Ready`, and requirement references like `(US-07)` and `(US-11)` embedded in user-facing buttons, labels, and modal titles.
2. **Roadmap Clutter:** A 5-step roadmap card occupying 40% of the screen (`Карта вертикальних зрізів MVP (In Scope) 36 атомарних User Stories`).
3. **Debug Dashboard in Primary Nav:** A prominent backend health indicator showing raw JSON timestamps and database status next to the main CTA.
4. **Hardcoded Mock Tournaments:** The UI fell back to fake hardcoded client data (`DEMO_TOURNAMENTS`) instead of relying honestly on PostgreSQL persistence, obscuring whether new tournaments were actually being created and saved.
5. **System Emoji Soup:** Indiscriminate emojis (`🌪️`, `🏆`, `🎮`, `📅`, `🔗`, `📋`, `✓`, `✕`, `ℹ️`) instead of clean vector iconography.

This refactoring removes all AI slop, replaces the amateurish theme with a Tier-1 **"Broadcast Studio" Esports Arena** design system, and introduces a slide-over drawer and 2-step creation wizard while maintaining 100% contract compliance with `docs/contracts/` and all course rubric requirements.

---

## 2. Visual Design System ("Broadcast Studio")

### 2.1 Color Palette & Tokens
- **Background Canvas:** Deep charcoal `#0B0E14` (neutral, high-contrast, free of generic purple tint).
- **Surface Elevation 1 (Cards, Nav):** `#121620` with border `#1E2536`.
- **Surface Elevation 2 (Drawers, Modals, Inputs):** `#181F2E` with border `#263147`.
- **Primary Accent (Esports Action / CTA):** Electric Amber / Orange (`#F97316` hover `#FB923C`, shadow `rgba(249, 115, 22, 0.25)`).
- **Discipline Signatures:**
  - **Counter-Strike 2 (CS2):** Gold / Amber accent (`#F59E0B`), badge `5v5 TACTICAL`.
  - **Brawl Stars:** Electric Violet (`#A855F7`), badge `3v3 ACTION`.
  - **Chess.com:** Arctic White / Slate (`#E2E8F0`), badge `1v1 RAPID`.
- **Status Indicators (Pulse Dot + Monospace Badge):**
  - `REGISTRATION_OPEN`: Emerald `#10B981` ("РЕЄСТРАЦІЯ").
  - `CHECK_IN_ACTIVE`: Amber `#F59E0B` ("CHECK-IN").
  - `IN_PROGRESS`: Coral Red `#EF4444` ("НАЖИВО").
  - `COMPLETED`: Neutral Slate `#64748B` ("ЗАВЕРШЕНО").

### 2.2 Typography & Iconography
- Zero system emojis. All icons are pure SVG vectors (Trophy, Gamepad, Calendar, Users, Copy, Check, Filter, X, ChevronRight, Server, Shield).
- Monospace font (`font-mono`) reserved for dates, timestamps, slots (`8 / 16`), and server connect strings (`connect 194.44.12.5:27015`).
- High-contrast text scale: H1 `text-2xl/text-3xl font-extrabold tracking-tight`, Body `text-sm text-slate-300`, Meta `text-xs text-slate-400`.

---

## 3. Component Architecture & Data Flow

### 3.1 `App.tsx` (Root Layout)
- **Top Navigation Bar:**
  - Brand Mark: Tournado emblem + wordmark.
  - Sub-navigation links: "Турніри" (active), "Матчі" (disabled/coming soon badge), "Рейтинги".
  - Quick action: "Створити турнір" (Primary CTA with trophy icon).
  - Silent error banner only if Core API is unreachable (graceful retry action, no raw JSON pings).
- **Filter & Search Bar:**
  - Discipline filter tabs: `Всі`, `CS2`, `Brawl Stars`, `Chess.com` with tournament count chips.
  - Status filter pill toggle: `Всі`, `Відкрита реєстрація`.
  - Search input: live filter by tournament name or description.
- **Tournament Grid / Empty State:**
  - True persistence mode: loads directly from `GET /api/v1/tournaments`.
  - If database returns empty `[]`: renders an elegant, motivating Empty State with illustration, clear copy, and a "Створити перший турнір" button.
- **Footer:** Clean, minimalist copyright and course identifier.

### 3.2 `TournamentCard.tsx`
- Refactored to modern esports card format:
  - Top row: Discipline badge with game-specific icon + format pill (`5v5`), status pill with pulse indicator.
  - Middle: Tournament title and truncated description.
  - Bottom metrics: Slots fill progress bar with exact count (`0 / 16 команд`), start date with calendar icon, registration deadline countdown.
  - Subtle hover border glow and arrow indicator inviting the user to view details.

### 3.3 `TournamentDrawer.tsx` (Replaces `TournamentDetailsModal.tsx`)
- Slide-over panel sliding from the right edge with a frosted glass backdrop.
- Keeps background tournament list visible for orientation.
- Three content tabs:
  1. **Огляд (Overview):** Full description, organizer, shareable link with one-click copy button, slots breakdown.
  2. **Розклад (Schedule):** Timeline showing Deadline -> Check-in -> Start with relative time highlights.
  3. **Сервер та карти (Server & Maps):**
     - For CS2: dedicated `connect <ip>:<port>` box with one-click copy and warning notes.
     - Map pool: visual grid of maps for CS2 and Brawl Stars; note for Chess.com.
- Close on Escape key, backdrop click, or close button.

### 3.4 `TournamentFormModal.tsx` (Two-Step Wizard)
- Replaces the 541-line monolithic form with an intuitive 2-step wizard:
  - **Step 1: Дисципліна та Назва:**
    - Discipline selector cards (CS2, Brawl Stars, Chess.com) with automatic team size badge and description.
    - Tournament Name input with instant inline validation (min 3 chars).
    - Optional Description textarea.
  - **Step 2: Слоти, Розклад та Сервер:**
    - Slot limit selector (8, 16, 32).
    - Schedule inputs with smart quick-presets ("Завтра о 18:00", "+2 год", "+1 год") and live chronological order validator (`deadline < check-in < start`).
    - CS2 Server Connect input (shown only if CS2 discipline selected).
    - Summary preview card before final submission.
- RFC 7807 error banner displaying backend validation messages in clean Ukrainian prose without raw technical properties.

---

## 4. Testing & Verification

1. **Vitest Unit & Component Tests:**
   - Update `TournamentList.test.tsx` to verify filtering by discipline, search queries, and empty state rendering.
   - Update `TournamentForm.test.tsx` to verify step progression, schedule validation, and submission handler.
   - Update `date.test.ts` ensuring timezone-aware calculations.
2. **TypeScript & Build Guardrails:**
   - `npx tsc --noEmit && npm run build` in `frontend/` must complete with 0 errors.
3. **End-to-End Verification in Running Stack:**
   - Verify UI on `http://localhost:3000` connected to live Spring Boot `core` and PostgreSQL.
   - Create a tournament via the new wizard and verify that it persists and immediately appears in the grid.
   - Restart or inspect PostgreSQL to verify data persistence.

---

## 5. Course Deliverables Synchronization

- **`prompts/5.1_code_generation.md`:** Document the refined prompt for the professional UI and backend integration.
- **`prompts/5.2_fixing_and_refactoring.md`:** Add section documenting the elimination of AI slop, migration to Slide-over Drawer, and wizard refactoring.
- **`prompts/5.3_ai_code_review.md`:** Ensure audit criteria cover clean UX and absence of internal rubric leakage.
- **`README.md`:** Update the "Як перевірити перший робочий сценарій" section with crisp, step-by-step instructions for the 3-minute live evaluation.

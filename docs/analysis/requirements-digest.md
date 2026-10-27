# Tournado — Requirements Digest

> **Source base.** `docs/` contains a single file, `docs/requirements.md` ("Інженерна специфікація вимог — Tournado"). It holds: §1 User Journey (persona + Happy Path), §2 Story Map, §3 MVP Scope (MoSCoW, items `X.Y.Z` and participant items `У.1–У.10`), §4 User Stories `US-01…US-36`, §5 Acceptance Criteria (checklists + Gherkin scenarios). Everything below is traced to that file.
>
> **Authority order used in this digest.** Where sources disagree, §5 Acceptance Criteria > §4 User Stories > §3 Scope > §2 Story Map > §1 Journey. The Journey and Story Map describe the *full* product vision; §3 explicitly cuts it down to the MVP. Conflicts are still listed (see §2.3) because they will confuse readers.
>
> **Reference conventions.** `US-12 AC3` = third checklist item of US-12 acceptance criteria. `US-12 Sc.2` = Gherkin "Сценарій 2" of US-12 (scenario title quoted where useful). `Scope 4.1.2` = row in §3.2. `У.8` = participant item in §3.3. `Journey Step 4` = §1.2. Quotes are verbatim in the original Ukrainian.
>
> **Labels.** **[E]** explicit in the requirements · **[I]** inferred (necessary consequence of explicit text, but not stated) · **[U]** unspecified.

---

## 1. Entities and Their Lifecycles

### 1.1 Entity overview

| # | Entity | Core role | Primary stories |
|---|---|---|---|
| E1 | User / Profile | Identity, login, game identifiers | US-01, US-02 |
| E2 | Team (+ Roster membership) | Unit of registration for all disciplines | US-03, US-05, US-06, US-14 |
| E3 | Team Invite Link | Joining a roster | US-04, US-05 |
| E4 | Tournament | Configuration, schedule, overall lifecycle | US-07–US-11, US-15, US-18, US-35 |
| E5 | Registration (team-in-tournament entry) | Slot occupation, check-in, exclusion | US-12, US-13, US-16, US-17 |
| E6 | Bracket | Single Elimination structure, seeding, byes | US-18, US-19, US-20, US-21 |
| E7 | Match (incl. Bye slot) | Pairing, status, result, advancement | US-21–US-29, US-34 |
| E8 | Match Room phase state (Ready Check, Map Ban, Game Credentials) | Pre-match flow inside a Match | US-22–US-26 |
| E9 | Score Submission | Per-captain reported score | US-27, US-28 |
| E10 | Chess Result Import | External result for Chess.com matches | US-29 |
| E11 | Dispute (+ Evidence screenshot) | Conflict resolution | US-30, US-31, US-34 |
| E12 | AI Analysis (score recognition + nickname check) | Advisory input to dispute | US-32, US-33 |
| E13 | Final Standings / CSV Report | Places and export | US-35, US-36 |

There is **no explicit "Organizer" entity or role assignment** — the organizer is implied to be the user who created the tournament [I] (US-13 AC1 "організатору цього турніру", US-34 AC5, US-36 AC1). There is no platform administrator role [U].

---

### 1.2 E1 — User / Profile

**Attributes [E]:** profile name (shown in header after Steam login, US-01 AC2); Steam ID, Supercell Player Tag, Chess.com nickname — each optional (US-02 AC1); login method: Steam or e-mail (US-01 AC1).

**States**

| State | Explicit? | Notes |
|---|---|---|
| Anonymous visitor | [E] US-01 Sc.4–5 | May view tournament page (US-11 AC2) and bracket (US-21 AC1). |
| Authenticated | [E] US-01 Sc.1–2 | |
| Profile created | [E] US-01 AC4 | Created on first login; reused on repeat login. |

**Transitions**

| From → To | Initiator | Condition | Source |
|---|---|---|---|
| Visitor → Authenticated (+ profile created on first login) | User | Successful Steam or e-mail login | US-01 AC2–4, Sc.1–3 |
| Visitor → Visitor (with message) | User | Steam login cancelled | US-01 AC5, Sc.4 |
| Identifier set / changed / cleared | User | Value passes format check | US-02 AC2–4, Sc.1–3 |

**Restrictions / dependencies:** Missing identifiers do not block login but block registration for that discipline (US-02 AC5 → US-12 AC3).

**Unspecified [U]:** how e-mail login works (password vs. one-time link; US-01 Sc.2 assumes "має підтверджену електронну пошту" but no verification story exists); whether a Steam login auto-fills Steam ID; whether Steam and e-mail logins can belong to one profile; identifier formats; whether identifiers can be edited while the player is in a locked roster (see GAP-09).

---

### 1.3 E2 — Team and Roster

**Attributes [E]:** name (required, unique within discipline — US-03 AC1, AC6); discipline (required, immutable — US-03 AC5); captain (= creator — US-03 AC2); roster members; fullness indicator "N із M" with M = 5 / 3 / 1 (US-06 AC1); lock status «Склад заблоковано» (US-14 AC3).

**States**

| State | Explicit? | Source |
|---|---|---|
| Incomplete roster (N < M) | [E] | US-06 AC3, Sc.2 |
| Full roster («склад повний», N = M) | [E] | US-06 AC3, Sc.1 |
| Locked («Склад заблоковано») | [E] | US-14 AC3 |
| Unlocked again after the tournament | [U] | Nowhere specified |
| Deleted team | [I] | Only mentioned indirectly: US-05 Sc.4 "посилання вказує на видалену або неіснуючу команду"; no deletion story |

**Transitions**

| Transition | Initiator | Condition | Source |
|---|---|---|---|
| Create team | Authenticated user | Name + discipline provided | US-03 Sc.1–3 |
| Add member (N+1) | Player via invite link | Free place; not already a member; roster not locked | US-05 AC3–5, AC7 |
| Remove member (N−1) | **[U]** | US-06 AC2 mentions "після його видалення", but no story defines who removes or how | US-06 AC2 |
| Lock | System (automatic) | Registration deadline of a tournament the team is registered in | US-14 AC1, Sc.1 |
| Unlock | **[U]** | — | — |

**Restrictions:** Chess.com team = one player (US-03 AC7). Roster capacity is exactly the team size; there are **no substitutes** in MVP [I] (Story Map 2.2 mentions "запасні гравці", but US-06 caps M at team size and US-04/US-05 block joining when full).

**Unspecified [U]:** leaving a team; captain transfer; whether one player may belong to several teams of the same discipline (GAP-10); whether a lock is per tournament or global, and when it ends (GAP-11).

---

### 1.4 E3 — Team Invite Link

**Attributes [E]:** unique per team; copyable (US-04 AC2–3).

| Transition | Initiator | Condition | Source |
|---|---|---|---|
| Generate | Captain only | Roster not full | US-04 AC1, AC4, Sc.1–3 |
| Use (join) | Authenticated player | See E2 add-member conditions | US-05 |
| Invalid | — | Team deleted / non-existent | US-05 AC6, Sc.4 |

**Unspecified [U]:** expiry, revocation, whether a previously issued link remains valid once the roster becomes full (join is blocked anyway by US-05 AC5).

---

### 1.5 E4 — Tournament

**Attributes [E]:** name (US-11 AC2 — but no creation AC defines the field, GAP-21); discipline (US-07); team size (derived, read-only — US-07 AC3); map pool (derived — US-08); format = Single Elimination (US-09 AC1); slot limit ∈ {8, 16, 32} (US-09 AC2); start date-time, registration deadline, check-in opening time (US-10 AC1); public link (US-11); description field «Опис турніру» (Scope 1.3.2 rationale only); status (US-18 AC5, US-35 AC1); final places (US-35).

**States**

| State | Explicit? | Source / reasoning |
|---|---|---|
| Created / Registration open | [I] | US-12 AC1 "до дедлайну прийому заявок" |
| Registration closed + rosters locked | [I] | US-12 AC7, US-14 AC1 |
| Check-in open | [I] | US-15 AC1 |
| Check-in closed (exclusion done) | [I] | US-17 AC1 |
| «Триває» (In progress) | **[E]** | US-18 AC5 "Після побудови сітки статус турніру змінюється на «Триває»" |
| «Завершено» (Completed) | **[E]** | US-35 AC1 |
| Not started due to < 2 teams | [U] | US-17 AC5 says only "сітка не генерується" |
| Cancelled / edited / deleted | [U] | No story |

**Transitions**

| Transition | Initiator | Condition | Source |
|---|---|---|---|
| Create | Authenticated user (becomes organizer [I]) | Discipline, slot limit, valid schedule | US-07 Sc.3, US-09 Sc.2, US-10 Sc.1–4 |
| → Registration closed / rosters locked | System | Registration deadline reached | US-14 AC1 |
| → Check-in open | System | Scheduled check-in time | US-15 AC1 |
| → Check-in closed, non-checked-in teams excluded | System | End of check-in window (end time not stated, see GAP-15) | US-17 AC1 |
| → «Триває» | System | Start time reached **and** ≥ 2 checked-in teams | US-18 AC1, AC5; US-17 AC5 |
| → «Завершено» | System (no organizer action) | Final match closed; no open matches or active disputes | US-35 AC1, AC5 |

**Restrictions:** After «Завершено» match results and bracket are immutable (US-35 AC4, Sc.3). CSV export only in «Завершено» (US-36 AC1).

---

### 1.6 E5 — Registration (team-in-tournament entry)

**Attributes [E]:** team; tournament; check-in status; exclusion status.

**States**

| State | Label in requirements | Source |
|---|---|---|
| Registered | "список зареєстрованих" | US-12 AC4 |
| Checked in | «Check-in підтверджено» | US-16 AC2 |
| Excluded for no check-in | «Виключено: немає Check-in» | US-17 AC4 |
| Removed by organizer | (no label) | US-13 AC3 |
| Withdrawn by captain | [U] | No story |
| Placed in bracket / eliminated / final place | [I] | US-18 AC3, US-36 AC8 |

**Transitions**

| Transition | Initiator | Condition | Source |
|---|---|---|---|
| → Registered | Captain (or chess solo player) | Before deadline; free slot; same discipline; full roster; all IDs present; not already registered | US-12 AC1–7 |
| Registered → Removed | Organizer of this tournament | Confirmation; before bracket generation | US-13 AC1–5 |
| Registered → Checked in | Captain of the team | Window open | US-16 AC1–2 |
| Registered → Excluded | System | Window ends without check-in | US-17 AC1 |
| Checked in → in bracket | System | Start time | US-18 AC1 |

---

### 1.7 E6 — Bracket

**Attributes [E]:** rounds; pairs; byes; seeding (random).

| State | Explicit? | Source |
|---|---|---|
| Not generated ("сітка з'явиться на початку турніру") | [E] | US-21 Sc.3 |
| Generated, first-round draw fixed | [E] | US-19 AC3, Sc.2 |
| Progressing (winners fill later rounds) | [E] | US-18 AC4, US-28 AC2 |
| Frozen (tournament completed) | [E] | US-35 AC4 |

**Transition:** generation by System at start time from checked-in teams only (US-18 AC1). No organizer reorder (US-19 AC1; Scope 3.2.4 Post-MVP).

---

### 1.8 E7 — Match

**Attributes [E]:** two teams (or one team + Bye); round; status; score; winner; link to Match Room.

**Status vocabulary found in the requirements**

| Status | Source |
|---|---|
| «Очікується переможець» (opponent not yet known) | US-21 AC3 |
| очікується / триває / завершено / спір / технічна поразка | US-21 AC2 |
| Ready Check / Pick/Ban / матч триває / очікування рахунку / спір | Scope 4.1.1 / Story Map 4.1 |
| «Очікується підтвердження суперником» | US-27 AC4 |
| «Очікується результат» (chess) | US-29 AC3 |
| «Завершено» | US-28 AC1 |
| «Технічна поразка» | US-24 AC3 |
| «Спір» | US-30 AC5 |
| «Технічний прохід» (Bye) | US-20 AC3 |

**Lifecycle (composed; [E] unless marked)**

```
Waiting for opponent («Очікується переможець»)
   └─(both teams known; System)→ Ready Check [Match Room opened, 10-min timer starts]  US-22 AC1, US-24 AC1
        ├─(one team not ready in 10 min; System)→ «Технічна поразка» (closed, opponent advances)  US-24 AC2–3
        ├─(neither team ready in 10 min)→ [U]  GAP-12
        └─(both ready; Captains)→ Map Ban (CS2/BS)  US-23 AC3, US-25 AC1
                                   └─ Chess: skip ban  US-25 Sc.4
             └─(one map left; System)→ Credentials shown / game in progress [I: "after start of game" trigger undefined]  US-26, US-27 AC1
                  CS2/BS:
                  ├─(1st score submitted)→ «Очікується підтвердження суперником»  US-27 AC4
                  ├─(2nd score equal)→ «Завершено», winner advances  US-28
                  ├─(2nd score differs)→ unnamed "mismatch" state, no advancement, dispute available  US-28 AC4  [state name U]
                  └─(dispute opened by captain)→ «Спір»  US-30 AC5
                         └─(organizer decision)→ «Завершено», winner advances  US-34 AC3
                  Chess:
                  ├─(game not found)→ «Очікується результат», retry  US-29 AC3
                  ├─(win imported)→ closed, winner advances  US-29 AC2
                  ├─(draw imported)→ no advancement, organizer notified → [U] next step  US-29 AC5, GAP-04
                  └─(archive unavailable)→ not closed, organizer sees failure → [U] next step  US-29 AC4, GAP-04
Bye: «Технічний прохід» — no Match Room, team advances without play  US-20 AC3, US-22 AC5
```

**Who initiates:** System (timers, auto-close, chess import, advancement); captains (ready, bans, score, dispute); organizer (dispute closure only). **No organizer override exists in MVP** (Scope 4.3.2 Post-MVP).

---

### 1.9 E8 — Match Room phase state

| Sub-state | Attributes / rules | Source |
|---|---|---|
| Ready Check | Per-team «Готова»; 10-min visible timer; idempotent | US-23 AC1–6 |
| Map Ban | Pool from tournament; coin toss for first ban; 30 s per turn; random auto-ban on timeout; last map = played map | US-25 AC1–7 |
| Credentials | CS2 «connect IP», Brawl Stars room code, Chess.com game link; visible only to the two rosters; hidden until pre-match ends | US-26 AC1–6 |

**Unspecified [U]:** who supplies CS2 server IP (У.8 says "вказує організатор", no AC) and Brawl Stars code (У.8 says "вводить капітан", no AC), how the Chess.com link is produced (GAP-01, GAP-02, GAP-03).

---

### 1.10 E9 — Score Submission

**Attributes [E]:** two non-negative integers, one per team (US-27 AC2); submitting captain; hidden from opponent until opponent submits (US-27 AC5).

**Transitions:** submitted by each captain separately (US-27 AC4); compared by System (US-28). **Unspecified:** whether a captain can change a submission; deadline for the second submission (GAP-05, GAP-06).

---

### 1.11 E10 — Chess Result Import

**Attributes [E]:** two players' Chess.com nicknames from profiles (US-29 AC1); outcome win / draw / not found / service unavailable.

**States:** pending («Очікується результат», with retry) → imported (win → close) | draw (organizer notified) | failure (organizer sees failure). **Unspecified:** game-matching criteria, retry stop condition, organizer resolution mechanism (GAP-03, GAP-04).

---

### 1.12 E11 — Dispute (+ Evidence)

**Attributes [E]:** match; opening captain; screenshot (JPG/PNG, size-limited); AI score suggestion + confidence; nickname comparison; resolution (AI-approved or manual score).

| State | Initiator | Condition | Source |
|---|---|---|---|
| Opened (match → «Спір») | Captain of CS2/BS match | Scores mismatch **or** opponent did not confirm; screenshot attached; no other active dispute | US-30 AC1–6 |
| Notified | System | Immediately on opening; visible to organizer only | US-31 AC2, AC5 |
| AI analysed | System | After screenshot upload | US-32 AC5 |
| Resolved (match closed, notification marked processed) | Organizer of this tournament | Approve AI (only if recognised) or valid manual score | US-34 AC1–5, US-31 AC3 |

**Restrictions:** decision final; no repeat disputes (US-34 AC6). **Unspecified:** evidence from the opposing captain, who besides the organizer can see the card/screenshot (GAP-25).

---

### 1.13 E12 — AI Analysis

**Attributes [E]:** recognised score, confidence % (US-32 AC1); per-nickname match result «Не знайдено в ростері» / «Збіг із ростером» / «Нікнейми не розпізнано» (US-33 AC2–7); label «Підказка ШІ» (US-32 AC3).

**States:** recognised | «Рахунок не розпізнано» (US-32 AC4); nicknames matched | mismatch warning | not recognised (US-33). Purely advisory — never changes match/bracket state on its own (US-32 AC3, Sc.3; US-33 AC5).

**Unspecified:** which team each recognised number belongs to (GAP-07); what "nickname" the roster provides for comparison (GAP-08); behaviour when the AI service itself fails (only "not recognised" is defined).

---

### 1.14 E13 — Final Standings and CSV Report

**Attributes [E]:** 1st = final winner, 2nd = final loser (US-35 AC2); places for eliminated teams "за послідовністю вильоту" (US-36 AC8); CSV rows: team name, roster nicknames, final place, sorted ascending, UTF-8 (US-36 AC3–6).

**Unspecified:** 3rd place (contradiction C-01), tie representation for teams eliminated in the same round (GAP-17).

---

## 2. Business Rules and Invariants

### 2.1 Rules

| ID | Rule (testable) | Source | Quote |
|---|---|---|---|
| **Authentication & profile** ||||
| BR-01 | An unauthenticated visitor cannot create a tournament, create a team, or register for a tournament; the system prompts to log in instead. | US-01 AC6, Sc.5 | «Неавторизований відвідувач не може створити турнір, команду або подати заявку» |
| BR-02 | First login creates a profile; a repeat login with the same Steam account opens the existing profile and creates no duplicate. | US-01 AC4, Sc.3 | «при повторному вході відкривається той самий профіль без створення дубліката» |
| BR-03 | Each game identifier is optional; a value in an invalid format is not saved and an error is shown next to the field. | US-02 AC1, AC4, Sc.3–4 | «кожне поле можна залишити порожнім»; «система не зберігає його» |
| **Teams & rosters** ||||
| BR-04 | A team requires a name and a discipline; the creator becomes captain and first roster member. | US-03 AC1–3, Sc.1–3 | «автоматично стає її капітаном і першим учасником ростеру» |
| BR-05 | A team's discipline cannot be changed after creation. | US-03 AC5 | «Дисципліну створеної команди змінити неможливо» |
| BR-06 | Team names are unique within a discipline. | US-03 AC6 | «Кожна назва команди є унікальною в межах дисципліни» |
| BR-07 | Roster size never exceeds the discipline team size (CS2 5, Brawl Stars 3, Chess.com 1); joining a full roster is refused and no invite link is issued for a full roster. | US-04 AC4, Sc.3; US-05 AC5, Sc.2; US-06 AC1; US-03 AC7 | «вільних місць немає, і не видає нове посилання» |
| BR-08 | Only the captain can generate an invite link. | US-04 AC1, Sc.2 | «доступна лише капітану команди» |
| BR-09 | A player already in a team is not added again via its link. | US-05 AC4, Sc.3 | «повторно не додається» |
| BR-10 | An invite to a deleted/non-existent team is rejected as invalid. | US-05 AC6, Sc.4 | «повідомлення про недійсне запрошення» |
| **Tournament configuration** ||||
| BR-11 | Discipline is required; team size is set automatically (5v5 / 3v3 / 1v1) and cannot be edited by the organizer. | US-07 AC2–4, Sc.1–3 | «організатор не може змінити його вручну» |
| BR-12 | The map pool is filled automatically for the chosen discipline, saved with the tournament and used for bans; Chess.com tournaments have no map pool. | US-08 AC1–4, Sc.1–2 | «зберігається разом із турніром і використовується в Match Room» |
| BR-13 | Format is Single Elimination; slot limit must be explicitly chosen from {8, 16, 32}; no default; no arbitrary values. | US-09 AC1–3, AC6, Sc.2–3 | «доступні лише 8, 16 і 32; довільне значення ввести неможливо» |
| BR-14 | Start time, registration deadline and check-in opening are all required, all in the future, and ordered deadline < check-in opening < start. | US-10 AC1–3, Sc.2–4 | «Дедлайн прийому заявок має бути раніше за час відкриття Check-in, а Check-in - раніше за початок змагань» |
| BR-15 | Each tournament has a unique public link that never changes and opens without login, showing name, discipline, format, schedule and occupied slots; unknown link → "not found". | US-11 AC2–3, AC5, Sc.2–3 | «Посилання унікальне для кожного турніру й не змінюється після створення» |
| BR-16 | All MVP matches are Best of 1 with the standard pool; timer values are fixed (10 min ready, 30 s per ban). | Scope 1.1.3, 1.2.2 | «у MVP усі матчі Best of 1»; «фіксованими значеннями (10 хв на готовність, 30 с на бан)» |
| **Registration** ||||
| BR-17 | Registration is possible only for an authenticated captain (or chess solo player) and only before the registration deadline. | US-12 AC1, AC7, Sc.4 | «Після дедлайну прийому заявок кнопка реєстрації недоступна» |
| BR-18 | Only a team of the tournament's discipline can be registered. | US-12 AC2 | «команди інших дисциплін недоступні для вибору» |
| BR-19 | Registration requires a full roster and every roster player having the identifier for the tournament's discipline. | US-12 AC3, Sc.3; US-06 AC4, Sc.3; US-02 AC5 | «лише за повного складу … і за наявності в усіх гравців ігрового ідентифікатора» |
| BR-20 | The number of registered teams never exceeds the slot limit; a registration when all slots are taken is refused; taking the last slot succeeds. | US-09 AC5; US-12 AC6, Sc.2, Sc.6 | «Кількість зареєстрованих команд ніколи не перевищує ліміт слотів» |
| BR-21 | A team cannot be registered twice for the same tournament. | US-12 AC5, Sc.5 | «Одна команда не може бути зареєстрована на той самий турнір двічі» |
| BR-22 | Only the organizer of that tournament can remove a registered team, after confirmation and only before bracket generation; removal frees a slot and the team takes no part in check-in or bracket. | US-13 AC1–5, Sc.1–4 | «після побудови сітки (US-18) кнопка видалення недоступна» |
| **Roster lock** ||||
| BR-23 | When the registration deadline passes, rosters of all registered teams lock automatically with no organizer action; no player can be added or join; status «Склад заблоковано» is shown; membership is unchanged. Before the deadline changes are allowed. | US-14 AC1–5, Sc.1–3; US-05 AC7, Sc.5 | «Одразу після настання дедлайну … автоматично блокує зміни ростерів» |
| **Check-in** ||||
| BR-24 | The check-in window opens automatically at the scheduled time; before that the button is unavailable. | US-15 AC1, AC3, Sc.1–2 | «Участь організатора не потрібна» |
| BR-25 | Only the captain of a registered team can check in, only while the window is open; repeated presses change nothing. | US-16 AC1–6, Sc.1–4 | «Повторне натискання не створює дубльованого запису» |
| BR-26 | At the end of the window every team without «Check-in підтверджено» is excluded, labelled «Виключено: немає Check-in», and gets no bracket position; checked-in teams are unchanged. | US-17 AC1–4, Sc.1–2 | «автоматично виключає з турніру кожну команду без статусу «Check-in підтверджено»» |
| BR-27 | If fewer than two teams checked in, no bracket is generated and the organizer sees a message. | US-17 AC5, Sc.3 | «їх залишилось менше двох, сітка не генерується» |
| **Bracket** ||||
| BR-28 | At start time the system builds the bracket automatically from all and only checked-in teams; each appears exactly once; bracket size depends on checked-in count, not slot limit. | US-18 AC1, AC3; US-17 AC6; US-13 AC5 | «Кожна підтверджена команда присутня в сітці рівно один раз» |
| BR-29 | Number of rounds = log₂ of the smallest power of two ≥ team count (8→3, 16→4, 32→5). | US-18 AC2, Sc.1–2 (see C-09 on wording) | «для 8 команд - 3 раунди; для 16 - 4; для 32 - 5» |
| BR-30 | First-round pairing is random, uninfluenced by the organizer, registration order or check-in time; a pair never contains the same team twice; the draw is fixed once generated. | US-19 AC1–5, Sc.1–2 | «Результат жеребкування фіксується і не змінюється» |
| BR-31 | Byes = (next power of two) − N, only in round 1, assigned randomly, at most one per team, none when N is a power of two; a bye team advances without playing and the slot shows «Технічний прохід». | US-20 AC1–3, AC5–6, Sc.1–3 | «6 команд - 2 Bye; 11 команд - 5 Bye» |
| BR-32 | After the bracket is built, tournament status is «Триває». | US-18 AC5 | «статус турніру змінюється на «Триває»» |
| BR-33 | The bracket is viewable without login and shows team names, status and score per pair; unknown opponents shown as «Очікується переможець»; updates visible after page reload. | US-21 AC1–4, Sc.1–3 | «Сітка доступна для перегляду без авторизації» |
| **Match Room** ||||
| BR-34 | A Match Room exists only for a pair with both teams known; never for a Bye or an unknown opponent. | US-22 AC1, AC5, Sc.3 | «Match Room не створюється» |
| BR-35 | Only the two captains of the pair can act (ready, ban, score); other authenticated users are read-only; unauthenticated users are redirected to login. Both captains see identical state. | US-22 AC3–4, AC6, Sc.2 | «інші авторизовані користувачі бачать кімнату у режимі перегляду» |
| BR-36 | Ready Check: the 10-minute timer starts when the Match Room opens; the ready button works only for one's own team, only before the match ends or a technical loss is awarded; repeated presses do nothing; when both are ready the pre-match phase starts. | US-23 AC2–6, Sc.1–3; US-24 AC1 | «Відлік 10 хвилин починається з моменту відкриття Match Room» |
| BR-37 | If one team is not ready within 10 minutes it receives a technical loss, the opponent wins and advances, and the match shows «Технічна поразка». A ready press before expiry (even with 1 s left) counts. | US-24 AC2–5, Sc.1–3 | «готовність зараховується, технічна поразка не присуджується» |
| BR-38 | Map ban (CS2/BS): starts when both are ready; first ban decided by coin toss; captains alternate one ban per 30-s turn; out-of-turn bans are impossible; on timeout a random remaining map is banned and the turn passes; the last remaining map becomes the played map. Chess skips bans. | US-25 AC1–7, Sc.1–4 | «система викреслює для нього випадкову карту … і передає хід» |
| BR-39 | Game credentials appear only after the pre-match phase ends, and only to players of the two rosters. | US-26 AC1, AC5–6, Sc.4 | «Сторонні користувачі їх не бачать» |
| **Results** ||||
| BR-40 | Score entry is available only for CS2/BS, only to the two captains, after the game starts; each captain submits two non-negative integers (one per team); ties, negatives, non-integers and empty values are rejected. | US-27 AC1–3, Sc.2–4 | «Нічийний рахунок (однакові числа) відхиляється» |
| BR-41 | A captain's submitted score is not shown to the opponent until the opponent submits. | US-27 AC5 | «Рахунок першого капітана не показується другому до тієї миті, поки той не введе свій» |
| BR-42 | If both submissions are identical the match closes as «Завершено», the higher score wins, advances, and the score appears in the bracket. | US-28 AC1–3, Sc.1 | «система автоматично закриває матч зі статусом «Завершено»» |
| BR-43 | If submissions differ, or only one exists, the match stays open and nobody advances. | US-28 AC4, Sc.2–3 | «матч не закривається; … не просуває жодну команду» |
| BR-44 | Chess results are imported only from the Chess.com archive using profile nicknames; captains enter nothing. Win → close and advance; not found → «Очікується результат» with retry; archive unavailable → not closed and not auto-disputed, organizer sees failure; draw → no advancement, organizer notified. | US-29 AC1–6, Sc.1–4 | «У разі помилки імпорту немає ручного введення рахунку капітанами, лише рішення організатора» |
| **Disputes & AI** ||||
| BR-45 | A dispute can be opened only by a captain of a CS2/BS match when scores mismatch or the opponent has not confirmed; it requires a JPG/PNG screenshot within the size limit; only one active dispute per match. | US-30 AC1–4, AC6, Sc.1–4 | «без файлу спір не відкривається»; «лише один активний спір одночасно» |
| BR-46 | An open dispute sets the match to «Спір» and halts its advancement until the organizer decides. | US-30 AC5 | «просування по сітці для цього матчу зупиняється до рішення організатора» |
| BR-47 | Dispute notifications are shown only to that tournament's organizer, immediately on opening, and are marked processed when the dispute closes. | US-31 AC2–3, AC5, Sc.1–3 | «Сповіщення показується лише організатору цього турніру» |
| BR-48 | The AI score suggestion is advisory: labelled «Підказка ШІ», shown next to the original screenshot, never changes match status or bracket; if unrecognised, «Рахунок не розпізнано» without a number. | US-32 AC1–4, Sc.1–3 | «не змінює статус матчу та сітку» |
| BR-49 | The AI nickname check compares against rosters as locked at roster lock; it only warns and never removes a team or changes a result. | US-33 AC1–7, Sc.1–3 | «Результат є лише попередженням» |
| BR-50 | Only the organizer of that tournament closes a dispute, either by approving the AI suggestion (enabled only if a score was recognised) or by a manual score validated like US-27; closing sets the match «Завершено», removes «Спір», advances the winner and unblocks the next match. The decision is final. | US-34 AC1–7, Sc.1–4 | «Рішення організатора є остаточним для матчу в межах MVP» |
| **Completion & export** ||||
| BR-51 | When the final match closes (by US-28, US-29 or US-34) the tournament becomes «Завершено» automatically; 1st = final winner, 2nd = finalist; places shown publicly. | US-35 AC1–3, Sc.1 | «без додаткових дій організатора» |
| BR-52 | A tournament cannot become «Завершено» while any match is open or any dispute is active; after completion results and bracket cannot be changed. | US-35 AC4–5, Sc.2–3 | «зміна результатів матчів та складу сітки недоступна» |
| BR-53 | CSV export is available only to the organizer and only for a completed tournament; one row per team with name, roster nicknames and final place, sorted ascending, UTF-8, consistent with the page; excluded/removed teams are omitted. | US-36 AC1–8, Sc.1–4 | «Виключені на Check-in або видалені команди у звіт не потрапляють» |

### 2.2 Notes on interpretation

- **BR-40/BR-42 imply fixed score orientation.** US-28 Sc.2 treats "13:9" vs "9:13" as a mismatch, so each field must be bound to a specific team (US-27 AC2 "по одному для кожної команди"), not "my score : their score".
- **No AI auto-decision anywhere.** Every AI output requires an organizer click (BR-48–BR-50; Scope 4.2.2 "ШІ лише підказує, рішення за людиною"). The Journey's 98 % confidence example does not create an auto-approve threshold.

### 2.3 Contradictions and inconsistencies

| ID | Conflict | Sources | Effect |
|---|---|---|---|
| C-01 | **Third place.** US-35 story: "зафіксував 1-ше, 2-ге і 3-тє місця"; Journey Step 5 also mentions 3rd. US-35 AC2 and Sc.1 only define 1st and 2nd; Single Elimination has no third-place match in scope. | US-35 story vs US-35 AC2; Journey Step 5 | Ambiguous expected output on the results page and in CSV. → GAP-16 |
| C-02 | **Bye pairing rule is unsatisfiable.** US-20 AC4: "дві команди з Bye не стоять в одній парі". With 9 teams (7 byes, US-20 Sc.3) or 11 teams (5 byes, AC2), round 2 has only 8 slots, so several round-2 pairs must contain two bye teams. Only valid if the rule means "no round-1 pair is Bye vs Bye". | US-20 AC2, AC4, Sc.3 | An AC that cannot pass as written. → GAP-14 |
| C-03 | **Check-in opening time: configurable vs fixed.** US-10 makes it an organizer-entered field with only an ordering constraint; US-15 story and AC1 say 30 minutes before start ("за US-15 це за 30 хвилин"). | US-10 AC1–2 vs US-15 AC1; Scope 3.1.1 | Unclear whether the field is free or fixed. → GAP-15 |
| C-04 | **Dispute: automatic vs manual.** Journey Step 4: "Система миттєво позначає матч як «Спірний»"; Story Map 4.2 / Scope 4.2.1: notification "коли капітани ввели різні рахунки або відкрили спір". US-28 AC4 and US-30 instead require a captain to open the dispute with a screenshot. | Journey Step 4; Scope 4.2.1 vs US-28 AC4, US-30 AC1–2 | A mismatched match with no dispute opened may be invisible and stall. → GAP-05 |
| C-05 | **Completion trigger.** Journey Step 5: "Максим підтверджує завершення турніру"; US-35: automatic, "без додаткових дій організатора". | Journey Step 5 vs US-35 AC1 | US-35 governs; Journey is outdated. |
| C-06 | **Roster lock trigger.** Journey Step 2: organizer "в один клік фіксує ростери"; US-14: automatic at deadline, "без дії організатора". | Journey Step 2 vs US-14 AC1 | US-14 governs. |
| C-07 | **Series format.** Journey Step 1 / Story Map 1.1: final Best of 3. Scope 1.1.3: "у MVP усі матчі Best of 1". | Journey, Story Map vs Scope 1.1.3 | Scope governs. |
| C-08 | **Traceability.** §4.1 says У.8 was split into "**US-25** (рядок `connect IP` для сервера CS2) та **US-26**", but US-25 is the map ban (У.7) and У.8 maps only to US-26 in §4.3. Also Scope 4.1.1 (organizer live dashboard) is traced to US-22, which is a captain story; Scope 2.2.1 (organizer list of teams) is traced to US-06, which is the captain's own team indicator. | §4.1 vs §4.3 | Organizer dashboard and organizer team list have no matching AC. → GAP-20, GAP-31 |
| C-09 | **Round count wording.** US-18 AC2: "Кількість раундів дорівнює такому степеню двійки, що покриває кількість команд" — literally means rounds = 8 for 8 teams; the examples (8→3) mean the exponent. | US-18 AC2 | Wording fix only; examples govern (BR-29). |
| C-10 | **Correction of wrong results.** Scope 4.3.2: "помилковий рахунок виправляється через спір (4.2.4) до просування". But agreed scores advance immediately (US-28 AC1–2), disputes need a mismatch or missing confirmation (US-30 AC1), and dispute decisions are final (US-34 AC6). | Scope 4.3.2 vs US-28, US-30, US-34 | An agreed-but-wrong score or a wrong technical loss cannot be corrected in MVP. → GAP-19 |
| C-11 | **Chess "organizer decision" with no mechanism.** US-29 AC6: "лише рішення організатора", but US-30/US-34 (the only organizer decision path) are limited to CS2/BS. | US-29 AC4–6 vs US-30 AC1, US-34 | Chess draws/import failures stall the bracket. → GAP-04 |
| C-12 | **Waitlist / notifications / account-existence validation** appear in Journey Steps 2–3 but are Post-MVP (Scope 2.3.1, 3.1.2, 3.1.4, 2.2.2б). | Journey vs Scope | Scope governs; listed only to stop readers implementing them. |

---

## 3. Non-Functional Requirements

| ID | Area | Requirement | Label | Relevance / evidence |
|---|---|---|---|---|
| NFR-01 | Authentication | Login via Steam or e-mail; actions that create or register require authentication. | **Explicit** | US-01 AC1, AC6 |
| NFR-02 | Authorization | Role checks are per resource: organizer = organizer *of this tournament*; captain = captain *of this team / this match*. Enforced for: removing teams (US-13 AC1), check-in (US-16 AC1), invite link (US-04 AC1), Match Room actions (US-22 AC3), dispute closure (US-34 AC5), dispute notifications (US-31 AC5), CSV (US-36 AC1). | **Explicit** | Each AC above has a "не організатор / не капітан" scenario. |
| NFR-03 | Authorization | Hidden buttons are not sufficient; the same checks must hold when an action is attempted directly (e.g. stale page, crafted request). | **Implied** | AC wording uses both "недоступна" and "дія неможлива" (US-25 Sc.2); permission rules would be meaningless if bypassable. |
| NFR-04 | Public access | Tournament page and bracket are public without login; Match Room requires login (read-only for non-captains); game credentials restricted to the two rosters. | **Explicit** | US-11 AC2, US-21 AC1, US-22 AC6, US-26 AC5 |
| NFR-05 | Concurrency | Slot limit holds under simultaneous registrations (last-slot race). | **Implied** | BR-20 "ніколи не перевищує"; US-12 Sc.6 |
| NFR-06 | Concurrency | Idempotency/uniqueness under repeated or simultaneous actions: duplicate registration, duplicate check-in, duplicate join, duplicate ready, second dispute. | **Explicit** (outcome) / **Implied** (under concurrency) | US-12 AC5, US-16 AC3, US-05 AC4, US-23 AC4, US-30 AC6 |
| NFR-07 | Concurrency | Exactly one outcome per time boundary: ready pressed at expiry (US-24 Sc.3), ban at turn expiry (US-25 AC6), check-in at window end (US-16 AC5), registration at deadline (US-12 AC7). The server clock is authoritative. | **Implied** | Last-second scenario in US-24 Sc.3 is explicit; clients' clocks cannot be trusted for these rules. |
| NFR-08 | Concurrency | Both captains submitting scores at the same moment must still produce exactly one comparison and at most one advancement. | **Implied** | US-28 Sc.1 "Коли: система отримує друге підтвердження" |
| NFR-09 | Data integrity | A team advances at most once per match and appears in the bracket exactly once. | **Explicit** / **Implied** | US-18 AC3; advancement in US-24, US-28, US-29, US-34 |
| NFR-10 | Data integrity | Results immutable after completion; dispute decisions final. | **Explicit** | US-35 AC4, US-34 AC6 |
| NFR-11 | Data integrity | Locked roster snapshot is preserved for nickname checks and the CSV, independent of later profile edits. | **Implied** | US-33 AC6 "ростери, зафіксовані на момент блокування"; US-36 AC3 |
| NFR-12 | Data integrity | CSV data equals the data shown on the tournament page. | **Explicit** | US-36 AC5 |
| NFR-13 | Deadlines | Scheduled transitions run without organizer action: roster lock, check-in open/close, exclusion, bracket generation, ready-timeout, ban-timeout, chess polling. They must fire even if no user is online. | **Explicit** (automatic) / **Implied** (no user online) | US-14, US-15, US-17, US-18, US-24, US-25, US-29 |
| NFR-14 | Deadlines | Time zone handling for schedule display and validation. | **Proposed MVP Assumption** | Not mentioned; US-10 compares times "в майбутньому" — see ASM-15. |
| NFR-15 | State visibility | Match Room state is identical for both captains, and changes (ready, bans, timers) are visible to the opponent during the timed phases. | **Explicit** | US-22 AC4, US-23 AC2, US-25 AC2 |
| NFR-16 | State visibility | Bracket only needs to update on reload; dispute notification must appear on the organizer dashboard "одразу". | **Explicit** | US-21 AC4; US-31 AC2 |
| NFR-17 | External integration | Chess.com archive unavailability must not close or auto-dispute a match; organizer sees the failure; system retries when game not yet found. | **Explicit** | US-29 AC3–4, Sc.3 |
| NFR-18 | External integration | Steam login cancellation returns user unauthenticated with a message. | **Explicit** | US-01 AC5 |
| NFR-19 | External integration | AI service failure/timeout degrades to "not recognised" so manual resolution remains possible. | **Implied** | US-32 AC4, US-34 Sc.3 define manual path when recognition fails; the dispute must never be blocked by the AI. |
| NFR-20 | AI oversight | AI never changes match status, bracket, or team participation; human (organizer) decides. | **Explicit** | US-32 AC3, Sc.3; US-33 AC5; Scope 4.2.2 |
| NFR-21 | AI transparency | AI output is labelled «Підказка ШІ», shown with confidence and the original screenshot. | **Explicit** | US-32 AC1–3 |
| NFR-22 | AI robustness | Text inside a screenshot is untrusted input; AI output must be treated as data (score + nickname list), not as instructions or a decision. | **Proposed MVP Assumption** | Follows from NFR-20; screenshots are uploaded by an interested party (US-30). |
| NFR-23 | Upload safety | Only JPG/PNG within a size limit; others rejected. Limit value is not specified. | **Explicit** (rule) / **Proposed MVP Assumption** (value) | US-30 AC3–4 |
| NFR-24 | Fairness / anti-cheat | Opponent's score is hidden until own submission; draw is random and fixed; first ban by coin toss. | **Explicit** | US-27 AC5, US-19, US-25 AC3 |
| NFR-25 | Privacy | Game credentials hidden from outsiders; dispute notifications only to organizer. Visibility of e-mail, game identifiers, rosters and screenshots to the public is not specified. | **Explicit** (parts) / **Proposed MVP Assumption** (rest) | US-26 AC5, US-31 AC5; see GAP-34, ASM-24 |
| NFR-26 | Auditability | Who decided a dispute, by which path (AI-approved vs manual), when; when/why a technical loss or exclusion happened. | **Implied** | Scope 4.2.4 & Journey stress "скандали"; final decisions (US-34 AC6) without a trace cannot be justified to participants. No explicit logging requirement. |
| NFR-27 | Encoding | CSV in UTF-8 opening correctly in Excel and Google Sheets with Cyrillic. | **Explicit** | US-36 AC6, Sc.4 |
| NFR-28 | Performance | No numeric targets exist. Business context: a 16-team event in "до 5 годин" (Persona), dispute resolved "за 30 секунд" (Journey Step 4). These are outcome goals, not system SLAs. | **Explicit** (context only) | §1.1, §1.2 — do not convert into SLAs. |
| NFR-29 | Scale | Bracket renders correctly for 8, 16 and 32 slots. | **Explicit** | US-21 AC6 |

---

## 4. Gaps and Open Questions

Each gap was checked against the full text of `docs/requirements.md`. Risk = impact if left unresolved. Priority: **BI** = before implementation starts, **BF** = before the affected feature is built, **D** = deferrable.

| ID | Gap | References | Why it matters | Cheapest MVP resolution | Risk | Priority |
|---|---|---|---|---|---|---|
| GAP-01 | **Who provides the CS2 server connect string, where and when.** У.8 says "рядок підключення до сервера CS2 (вказує організатор)", but no story or AC lets the organizer enter it (per tournament? per match?). | У.8; US-26 AC2, Sc.1 | Without it no CS2 match can start; it is in the demo scenario (§3.5). | Organizer enters one or more server strings during tournament creation; each match shows the string assigned to it. → ASM-16 | High | BF |
| GAP-02 | **Who enters the Brawl Stars room code.** У.8: "вводить капітан"; no AC says which captain, when, or what happens if nobody enters it. | У.8; US-26 AC3, Sc.2 | BS matches cannot start; the ready/ban timers say nothing about this phase. | After bans, the captain who did *not* ban first enters the code; both see it. → ASM-17 | High | BF |
| GAP-03 | **Chess.com game link and game matching.** US-26 requires "посилання на партію"; the source of that link is not defined (Scope says "посилання на виклик"). US-29 matches "за нікнеймами" but not which game counts (time window, time control, multiple games, rematches), nor when polling stops. | У.8; US-26 AC4; US-29 AC1, AC3 | Wrong game imported = wrong winner with no captain input and no appeal path. | Show a link that lets the players start a game against each other; count the first finished game between the two nicknames that started after the Match Room's pre-match phase ended. Poll until organizer acts. → ASM-18 | High | BF |
| GAP-04 | **Organizer resolution for chess.** Draws, import failures and missing games all end in "лише рішення організатора" (US-29 AC6), but there is no action for it; US-30/US-34 are CS2/BS only. | US-29 AC4–6, Sc.3–4; US-30 AC1; US-34 | Chess bracket stalls; tournament can never complete (BR-52). | Allow the organizer to set the chess winner manually from the same card used in US-34 (manual result path only, no AI). Draw → organizer picks the advancing player per tournament description (e.g. replay). → ASM-19 | High | BF |
| GAP-05 | **Score mismatch handling.** Does a mismatch auto-flag «Спір» (Journey, Scope 4.2.1) or only enable the button (US-28 AC4)? Can captains correct a mistyped submission? What if neither opens a dispute? | C-04; US-27, US-28 AC4, Sc.2; US-30 AC1 | Matches stall silently; organizer is not notified (US-31 is triggered only by opening a dispute). | Mismatch shows on the organizer dashboard as a "scores do not match" attention state (not a dispute); captains may resubmit until a dispute is opened; formal dispute still requires screenshot. → ASM-20 | High | BF |
| GAP-06 | **No deadline for the second score submission or for opening a dispute.** US-28 Sc.3 keeps the match open indefinitely; US-30 allows a dispute when "суперник не підтвердив результат" but gives no waiting time. | US-28 Sc.3; US-30 AC1 | A losing captain can block the bracket by not submitting. | The other captain may open a dispute at any time after their own submission; no auto-forfeit in MVP. → ASM-21 | High | BF |
| GAP-07 | **Mapping AI-recognised score to teams.** US-32 shows "рахунок" and confidence only; the Journey shows "13:10 на користь команди Alpha". One-click approval (US-34) requires knowing which number belongs to which team. | US-32 AC1; US-34 AC1, Sc.1; Journey Step 4 | One-click approval may assign the win to the wrong team. | AI returns score per team (using nickname matching to identify sides); if side cannot be determined, treat as "not recognised" (manual only). → ASM-22 | High | BF |
| GAP-08 | **Which "nickname" the roster provides for comparison.** Profiles store Steam ID, Player Tag, Chess.com nickname (US-02). Scoreboards show in-game display names, which are neither a Steam ID nor a Player Tag. | US-02 AC1; US-33 AC1–2, AC6; US-36 AC3 | Nickname check (a Must, anti-ringer) yields false "Не знайдено в ростері" for everyone, or cannot run. | Add a "game nickname" per player at roster/registration level or use platform profile name; compare case-insensitively and show both values to the organizer. Needs product decision. → ASM-10 | High | BI |
| GAP-09 | **Editing game identifiers after registration / roster lock.** US-02 allows edits anytime; US-33 uses locked rosters; US-29 uses "збережені в профілях" nicknames. | US-02 AC3; US-14; US-29 AC1; US-33 AC6 | A player can swap identity mid-tournament, defeating anti-ringer intent and corrupting chess import. | Snapshot identifiers at roster lock and use the snapshot for the tournament. → ASM-09 | High | BI |
| GAP-10 | **One player in several teams of the same tournament.** Nothing prohibits a player joining two teams of the same discipline and both registering. | US-05; US-12 | Same player on both sides; both captains could be the same person; breaks fairness and check-in. | A player may not appear in more than one team registered for the same tournament; enforced at registration. → ASM-08 | High | BI |
| GAP-11 | **Roster lock scope, member removal and unlock.** No story for removing a player or leaving a team (US-06 AC2 implies it exists). Unknown whether lock applies per tournament or to the team globally, and whether/when it ends. Unknown whether a roster can drop below full after registration but before deadline. | US-06 AC2; US-14 AC1–5 | Team could be locked forever, or play with an incomplete roster. | Lock applies while the team has an active registration in a not-completed tournament; ends at completion/exclusion/removal. Before lock, captain can remove members; registration remains but a team not full at lock is still allowed to play (flagged to organizer). → ASM-06, ASM-07 | Medium | BI |
| GAP-12 | **Both teams miss Ready Check.** US-24 covers only "одна команда". | US-24 AC2, Sc.1 | No winner → next round blocked → tournament cannot complete. | Double technical loss is not supported; match stays visible as stalled and the organizer decides via the manual result action (ASM-19 extension). Alternative: random advance. → ASM-13 | High | BF |
| GAP-13 | **Fewer than two checked-in teams.** US-17 says no bracket and a message; tournament status afterwards is not defined, nor whether the organizer can do anything. | US-17 AC5, Sc.3 | Tournament stuck in a non-terminal state forever. | Terminal state "Скасовано (недостатньо учасників)" set by the system; no further actions. → ASM-05 | Medium | BF |
| GAP-14 | **Bye invariant unsatisfiable (C-02).** | US-20 AC4, Sc.3 | An AC will fail testing for 9, 10, 11 … teams. | Reinterpret as "no round-1 pair consists of two Byes; each Bye pairs with exactly one team". Rewrite AC. → ASM-12 | High | BI |
| GAP-15 | **Check-in window boundaries (C-03).** Is opening time free or fixed at T-30? When does the window close — at start time? | US-10 AC1; US-15 AC1; US-17 AC1; US-18 AC1 | Exclusion and bracket timing depend on it. | Window opens at the configured time (default T-30 pre-filled), closes exactly at start time; exclusion then bracket generation in that order. → ASM-11 | Medium | BI |
| GAP-16 | **Third place (C-01).** | US-35 story vs AC2 | Results page and CSV disagree with the story. | No 3rd-place match; both semifinal losers share place 3. → ASM-14 | Medium | BF |
| GAP-17 | **Places of eliminated teams.** "за послідовністю вильоту" — by round of elimination or by time? Ties format? | US-36 AC8 | CSV content and sorting undefined. | Teams eliminated in the same round share a place range (e.g. "3–4", "5–8"); sort by place then name. → ASM-14 | Medium | BF |
| GAP-18 | **Tournament editing, cancellation and pre-start statuses.** No story for editing configuration/schedule or cancelling a tournament; pre-start statuses unnamed. | US-07–US-10; US-18 AC5 | Organizer error at creation (wrong date) cannot be fixed; UI cannot show status before «Триває». | Name pre-start states and add a terminal cancelled state (ASM-05); no editing in MVP except what the team explicitly adds; organizer recreates. | Medium | BI |
| GAP-19 | **No correction path for wrongly closed matches (C-10).** Agreed-but-wrong scores, erroneous technical loss, wrong chess import. | Scope 4.3.2; US-28; US-24; US-34 AC6 | Data integrity complaints with no remedy. | Accept for MVP; document as known limitation; organizer handles off-platform before final. → ASM-23 | Medium | D |
| GAP-20 | **Organizer live dashboard has no story/AC.** Scope 4.1.1 is Must but traced to US-22 (captain Match Room); US-31 assumes "дашборд організатора" exists. | Scope 4.1.1; §4.3; US-31 | The organizer's main screen has no testable definition; mismatch/stall visibility depends on it. | Add a minimal AC: list of all matches with status from BR-vocabulary + dispute counter; organizer-only. → ASM-25 | Medium | BF |
| GAP-21 | **Tournament name and description fields** are not in any creation AC, though shown on the public page (US-11 AC2) and used for rules (Scope 1.3.2). | US-07–US-11; Scope 1.3.2 | Missing required field on the creation form. | Name required, description optional free text. → ASM-03 | Low | BF |
| GAP-22 | **E-mail login mechanics and account linking.** Password vs. magic link, e-mail verification, Steam + e-mail on one profile. | US-01 AC1, AC3, Sc.2 | Duplicate accounts per person weaken anti-ringer checks; verification flow is unscoped work. | Separate accounts per login method in MVP; e-mail login by one-time link (verification implicit). → ASM-01 | Medium | BI |
| GAP-23 | **Identifier format rules** for Steam ID, Player Tag, Chess.com nickname are not given. | US-02 AC4, Sc.3 | Untestable AC. | Define a format per identifier type and document it in the AC. → ASM-02 | Low | BF |
| GAP-24 | **Solo chess registration model.** US-12 mentions "соло-гравець", US-03 AC7 requires a one-player "team". | US-03 AC7; US-12 AC1; US-16 | UI and data model diverge if both paths are built. | Chess players register through a one-member team created by themselves (single model). → ASM-07 | Medium | BI |
| GAP-25 | **Dispute evidence and visibility.** Only the opener uploads a screenshot; the other captain cannot add evidence (US-30 Sc.4). Who can see the dispute card and screenshot is unspecified. | US-30 AC2, AC6, Sc.4; US-32 AC2 | One-sided evidence; possible leak of screenshots. | MVP: one screenshot; card visible to the organizer only; opponent sees status «Спір». → ASM-24 | Medium | D |
| GAP-26 | **"After the game starts"** — score entry trigger is undefined (no explicit "game started" event); no game-specific score plausibility. | US-27 AC1 | Captains might enter scores during bans; implausible scores accepted. | Score entry opens once credentials are shown (end of pre-match). No per-game score validation beyond US-27. → ASM-20 | Low | BF |
| GAP-27 | **Screenshot size limit value.** "встановлений ліміт" unspecified. | US-30 AC4 | Untestable AC. | Team picks one value and writes it into the AC. → ASM-24 | Low | BF |
| GAP-28 | **Organizer as participant / organizer access to Match Room.** Can the organizer register their own team and then decide its dispute? Can the organizer see credentials? | US-13, US-22 AC3, US-26 AC5, US-34 | Conflict of interest; organizer may need credentials to help. | Allowed in MVP (single-organizer events); record in decision history. Organizer has read-only Match Room view. → ASM-04 | Low | D |
| GAP-29 | **Team withdrawal by captain, captain transfer.** No story. | US-12, US-13 | Captain who cannot attend just misses check-in → excluded (already handled). | Not supported in MVP; check-in exclusion covers withdrawal. | Low | D |
| GAP-30 | **Brawl Stars "map pool"** content and source ("офіційний пул") undefined; BS maps depend on game mode. | US-08; US-25 | Ban phase undefined for BS. | Fixed curated list per discipline maintained by the team. → ASM-15 | Low | BF |
| GAP-31 | **Traceability inconsistencies (C-08)**, incl. Scope 2.2.1 organizer team list traced to US-06. | §4.1, §4.3 | Coverage reports overstate completeness. | Correct §4.1 text; add organizer team list to US-13 AC (it already shows the list). | Low | D |
| GAP-32 | **Match timing in later rounds.** Match Room opens as soon as both teams are known, starting the 10-minute ready timer (US-24 AC1); no round start times exist. | US-22 AC1; US-24 AC1 | A team finishing a long match faces an immediately running timer for the next one. | Accept: opening = both teams known; announced through the bracket (US-21). → ASM-13 | Low | D |
| GAP-33 | **Behaviour when AI service fails** (timeout/error, not "unrecognised"). | US-32 AC4–5 | Dispute card might show nothing. | Treat as "Рахунок не розпізнано" / "Нікнейми не розпізнано". → ASM-22 | Low | BF |
| GAP-34 | **Public visibility of personal data.** Rosters, game identifiers, e-mails, screenshots — public or not? | US-11 AC2; US-21; US-36 | Privacy exposure of students' accounts. | Public: team names, bracket, places; rosters show nicknames only; e-mail never shown; screenshots organizer-only. → ASM-24 | Medium | BF |

---

## 5. Proposed MVP Assumptions

> **These are proposals, not confirmed requirements.** Each needs a yes/no from the product owner before being copied into acceptance criteria.

| ID | Proposed rule / behaviour | Why a reasonable MVP default | Related gaps / sources | Risk if wrong | How to validate |
|---|---|---|---|---|---|
| ASM-01 | Steam and e-mail logins create separate profiles; no linking. E-mail login uses a one-time link, which doubles as e-mail verification. | Avoids a linking flow; satisfies US-01 Sc.2 "підтверджену електронну пошту" without a separate verification story. | GAP-22; US-01 | Medium | Ask the team whether one person using both methods is expected in the demo. |
| ASM-02 | Identifier formats: Steam ID = 17-digit SteamID64; Player Tag = `#` + allowed Supercell characters; Chess.com = username charset of Chess.com. Steam ID auto-filled after Steam login. | Makes US-02 Sc.3 testable; formats are public conventions, no API check (2.2.2б is Post-MVP). | GAP-23; US-02 AC4 | Low | Confirm formats against each game's public documentation. |
| ASM-03 | Tournament creation requires a name; «Опис турніру» is optional free text that holds rules. | Public page already shows name (US-11 AC2); Scope 1.3.2 relies on the description field. | GAP-21 | Low | Review creation form wireframe. |
| ASM-04 | The organizer is the single creator of the tournament; no co-organizers, no platform admin. The organizer may also participate; organizer has read-only Match Room view including credentials. | Matches every "організатор цього турніру" AC; simplest permission model. | GAP-28; US-13, US-31, US-34, US-36 | Low | Confirm with the persona owner (student organizer). |
| ASM-05 | Tournament statuses: «Реєстрація» → «Реєстрацію закрито» → «Check-in» → «Триває» → «Завершено»; plus terminal «Скасовано» set automatically when < 2 teams check in. No manual edit or cancel in MVP. | Names already-implied states (§1.5); gives GAP-13 a terminal state at minimal cost. | GAP-13, GAP-18; US-17 AC5, US-18 AC5, US-35 | Medium | Walk through the demo scenario (§3.5) and the "only 1 check-in" case with the team. |
| ASM-06 | Roster lock applies per team while it has a registration in a tournament that is not completed or cancelled and from which the team is not excluded/removed; the lock lifts automatically afterward. | Prevents permanently frozen teams while keeping US-14 intact. | GAP-11; US-14 | Medium | Check: team registered in two tournaments; team finishing one tournament. |
| ASM-07 | Captain may remove members before lock (needed by US-06 AC2). If a registered team's roster drops below full before lock, the registration stays but is flagged to the organizer, who may remove it (US-13). Chess players always register through a one-member team. | Avoids silent auto-deregistration; single data model for chess. | GAP-11, GAP-24; US-03 AC7, US-06 AC2, US-12 | Medium | Confirm with Core Platform owner; add AC to US-06/US-12. |
| ASM-08 | A player may belong to at most one team registered in the same tournament; registration that violates this is rejected naming the player. | Cheap check at the single registration point; protects fairness. | GAP-10; US-12 | Low | Ask whether cross-team membership is ever legitimate in student events. |
| ASM-09 | At roster lock, each roster player's identifiers for the discipline are snapshotted; nickname checks, chess import and CSV use the snapshot. | Required for US-33 AC6 to mean anything; stops mid-tournament identity swaps. | GAP-09; US-02, US-29, US-33, US-36 | Medium | Test: change Chess.com nickname after lock → import still uses old value. |
| ASM-10 | Each roster entry has an "in-game nickname" used for the AI comparison, captured at join/registration for CS2/BS; comparison is case-insensitive and shows both values. | Steam ID / Player Tag cannot be read off a scoreboard; without this US-33 cannot work. | GAP-08; US-33 | High | Upload 3 real scoreboards per game and check that the stored nicknames match what the AI reads. |
| ASM-11 | Check-in opening time is configurable (US-10) and pre-filled with start − 30 min; window closes exactly at start time; at start the system first excludes, then builds the bracket. | Reconciles US-10 and US-15 without removing a field; one closing rule. | GAP-15; C-03 | Medium | Confirm with Tournament Engine owner; update US-15 AC1. |
| ASM-12 | US-20 AC4 is read as: "every Bye is paired with exactly one real team in round 1; no team gets two Byes". Round-2 pairs may contain two teams that had Byes. | Only interpretation consistent with US-20 AC2 and Sc.3. | GAP-14; C-02 | Low | Reword AC; run 2…32 team counts through the rule. |
| ASM-13 | Match Room opens when both teams are known (per US-22/24). If neither team is ready in 10 minutes, the match is marked as stalled and the organizer chooses the advancing team (or none if the bracket allows) via the manual result action. | Avoids designing double-forfeit propagation; keeps organizer in control. | GAP-12, GAP-32; US-24 | Medium | Confirm with persona owner what student organizers do today in this case. |
| ASM-14 | No third-place match. Places: 1 = final winner, 2 = finalist, teams eliminated in the same round share a range ("3–4", "5–8", …). CSV sorted by place, then team name. | Consistent with Single Elimination and US-35 AC2; deterministic CSV. | GAP-16, GAP-17; C-01; US-36 AC4, AC8 | Medium | Confirm sponsors/university accept shared 3rd place; update US-35 story text. |
| ASM-15 | Map pools are fixed curated lists per discipline maintained with the product (no live "official" feed). Times stored in absolute time and shown in the viewer's local time zone. | No external dependency; avoids ambiguity at deadlines. | GAP-30; NFR-14; US-08 | Low | Review lists with players of each game. |
| ASM-16 | CS2: the organizer enters one or more server connect strings when creating the tournament; matches show an assigned string. | Minimal input matching У.8 "вказує організатор". | GAP-01; У.8; US-26 AC2 | High | Confirm with organizer persona how many servers a 16-team event really has. |
| ASM-17 | Brawl Stars: after bans, the captain who did not ban first enters the room code; until entered, the opponent sees "Очікується код кімнати". No timer. | Matches У.8 "вводить капітан"; deterministic choice. | GAP-02; US-26 AC3 | Medium | Play-test one BS match flow. |
| ASM-18 | Chess: Match Room shows each opponent's Chess.com profile link for challenging; the system counts the first finished game between the two snapshotted nicknames that started after the Match Room's pre-match phase ended; polling continues until the match is closed. | Chess.com public API is read-only (Scope 4.1.3б), so the platform cannot create a game; a time-window rule is the simplest matching. | GAP-03; US-26 AC4, US-29 | High | Validate against the Chess.com archive with two test accounts. |
| ASM-19 | The organizer has a manual "set result" action for chess matches (draw, import failure) and for stalled matches (ASM-13), using the same validation and finality as US-34 manual entry; no AI involved. | Gives US-29 AC6 "лише рішення організатора" a mechanism; reuses US-34 behaviour. | GAP-04, GAP-12; C-11 | Medium | Confirm with AI & Match Flow owners; add AC to US-29. |
| ASM-20 | Score entry opens when credentials are shown. On mismatch, both captains see "Рахунки не збігаються", may resubmit until a dispute is opened, and the organizer dashboard shows the match as needing attention. A formal dispute still requires a screenshot. | Resolves C-04 cheaply; no auto-dispute without evidence. | GAP-05, GAP-26; US-27, US-28, US-30 | Medium | Review with Frontend owner; update US-28 AC4. |
| ASM-21 | No auto-forfeit for a missing second score in MVP; the captain who submitted may open a dispute immediately. | US-30 already allows "суперник не підтвердив"; avoids a new timer rule. | GAP-06; US-28 Sc.3, US-30 AC1 | Medium | Observe in demo whether stalls occur; revisit Post-MVP. |
| ASM-22 | AI must return the score per team (side identified by nickname match). If side or score cannot be determined, or the AI call fails, the card shows «Рахунок не розпізнано» and only manual entry is available. | Makes one-click approval safe; reuses existing fallback. | GAP-07, GAP-33; US-32 AC4, US-34 AC2 | High | Test on scoreboards where teams are ordered differently from the bracket. |
| ASM-23 | No correction of closed matches in MVP; this is a documented known limitation. | Scope already defers override (4.3.2). | GAP-19; C-10 | Medium | Product owner accepts the limitation explicitly. |
| ASM-24 | Visibility: public = tournament info, team names, bracket, places; roster nicknames shown on team/tournament pages; e-mail never shown; game identifiers visible only to the player and the organizer; dispute card and screenshots visible only to the organizer. Upload limit is a single fixed value chosen by the team and stated in US-30. | Least exposure consistent with existing ACs. | GAP-25, GAP-27, GAP-34; NFR-25 | Medium | Confirm with team; document in US-30 and US-11. |
| ASM-25 | Organizer dashboard = organizer-only list of all matches with their status (vocabulary of §1.8), dispute counter (US-31), import failures (US-29 AC4), and mismatch/stalled indicators. | Minimum needed for US-29, US-31 and ASM-13/20 to have a place to appear. | GAP-20; Scope 4.1.1 | Low | Add AC to Scope 4.1.1 / new story; review with Frontend owner. |
| ASM-26 | Each dispute decision, technical loss, exclusion and team removal records who/what triggered it and when, visible to the organizer. | Needed to justify "final" decisions (US-34 AC6) to participants; low cost. | NFR-26 | Low | Confirm need with persona owner. |

---

## MVP Readiness Summary

**What is solid.** The happy path is well specified and testable: authentication, team creation and joining, tournament configuration, registration constraints, roster lock, check-in and exclusion, random seeding with byes, Ready Check, map ban, score agreement, AI as advisory-only, dispute closure, automatic completion and CSV export. 53 business rules (BR-01…BR-53) can be lifted directly into tests.

**Critical unresolved decisions (must be settled before or at the start of the affected feature):**
1. How players actually get into the game — CS2 connect string (GAP-01), Brawl Stars code (GAP-02), Chess.com link and game matching (GAP-03).
2. What nickname the AI compares against (GAP-08) and how AI scores map to teams (GAP-07) — the core AI value proposition depends on both.
3. Paths that currently stall the bracket forever: chess draws/import failures (GAP-04), both teams missing Ready Check (GAP-12), score mismatch with no dispute and missing second score (GAP-05, GAP-06), fewer than two check-ins (GAP-13). Because BR-52 forbids completion while any match is open, each of these blocks Step 5.
4. Identity integrity: one player in two teams (GAP-10) and identifier edits after lock (GAP-09).

**Major contradictions:** third place (C-01), unsatisfiable Bye rule (C-02), configurable vs fixed check-in time (C-03), automatic vs manual disputes (C-04), and the Scope claim that wrong scores can be fixed through disputes (C-10). Journey-level conflicts (C-05–C-07, C-12) are superseded by Scope/AC and need no decision, only cleanup.

**Assumptions needing confirmation first:** ASM-10, ASM-16, ASM-18, ASM-22 (high risk if wrong), then ASM-05, ASM-11, ASM-12, ASM-14, ASM-19, ASM-20.

**Verdict.** The requirements are **not yet sufficient** to implement the end-to-end MVP without developers making product decisions on their own. They are sufficient to start Core Platform, tournament configuration, registration, check-in and bracket generation now (after resolving GAP-10, GAP-14, GAP-15, GAP-22, GAP-24). Match Room, results, chess import and dispute work should wait until the items in points 1–3 above are decided — roughly a one-meeting decision set, since every one of them has a cheap default proposed in §5.

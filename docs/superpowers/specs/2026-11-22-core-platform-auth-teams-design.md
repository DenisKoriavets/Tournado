# Архітектурна специфікація: Core Platform, Auth, Teams & Tournament Registrations

> **Статус:** На затвердженні  
> **Автор ролі:** Гриценко Артем — Core Platform & Auth Engineer  
> **Охоплені історії:** `US-01`..`US-06` (Автентифікація, профілі, команди, ростери, інвайти) + `US-12`, `US-13`, `US-14` (Реєстрація команд на турнір, модерація та Roster Lock)  
> **Трасування:** `ADR-0003`, `ADR-0004`, `ADR-0007`, `docs/contracts/api-contracts.md`, `docs/contracts/data-schema.md`, `docs/contracts/error-format.md`, `AGENTS.md`

---

## 1. Мета та межі (Scope)

Завершити весь функціональний блок відповідальності **Core Platform & Auth Engineer** наскрізно — від PostgreSQL 16 (Flyway + Hibernate JPA) і модульного моноліту Spring Boot 3 (Java 21) до клієнтського інтерфейсу React 19 + TypeScript (Broadcast Studio UI):

1. **Епік 1 (`identity`) — `US-01`, `US-02`:**
   * Безпарольний вхід через **Email Magic Link** (відправка листів через Spring Mail у локальний Docker **Mailpit** `http://localhost:8025`) та **Steam OpenID 2.0**.
   * Stateless-сесія через підписаний **JWT (HS256)** у `HttpOnly`, `SameSite=Lax` cookie `X-Auth-Token`.
   * Профіль користувача (`GET /api/v1/auth/me`, `POST /api/v1/auth/logout`) і прив’язка/валідація ігрових ідентифікаторів (`PATCH /api/v1/users/me/game-identifiers`: `steamId`, `supercellPlayerTag`, `chessUsername`).
2. **Епік 2 (`teams`) — `US-03`..`US-06`:**
   * Створення команди (`POST /api/v1/teams`) під дисципліну (`CS2` = 5, `BRAWL_STARS` = 3, `CHESS` = 1) з автоматичним призначенням автора капітаном і першим учасником ростеру.
   * Генерація інвайт-посилань капітаном (`POST /api/v1/teams/{teamId}/invites`), прев’ю запрошення (`GET /api/v1/teams/invites/{token}`) і приєднання гравців (`POST /api/v1/teams/join/{token}`).
   * Перегляд команди з індикатором повноти складу (`GET /api/v1/teams/{teamId}`) та списку команд поточного користувача (`GET /api/v1/teams/my`).
3. **Інтеграція з турнірами (`tournaments` — `US-12`, `US-13`, `US-14`):**
   * Захист `POST /api/v1/tournaments` авторизацією та прив’язка `createdByUserId` до поточного користувача.
   * Реєстрація команди капітаном (`POST /api/v1/tournaments/{tournamentId}/register`) із перевіркою дисципліни, повноти складу, наявності ігрового ідентифікатора у кожного гравця та дедлайну/ліміту слотів.
   * Перегляд зареєстрованих команд (`GET /api/v1/tournaments/{tournamentId}/registrations`), модерація/видалення організатором (`DELETE /api/v1/tournaments/{tournamentId}/registrations/{teamId}`), блокування ростерів зі створенням незмінних зліпків у `registration_roster_snapshots` (`POST /api/v1/tournaments/{tournamentId}/lock-rosters`) та реальний підрахунок `registeredTeamsCount` замість заглушки `0`.

---

## 2. Принципи SOLID та модульна архітектура (Backend `core/`)

Згідно з **ADR-0003** та `AGENTS.md`, модулі комунікують **виключно** через публічні сервісні інтерфейси (або DTO), без прямого інжектування репозиторіїв іншого модуля:

### 2.1. Пакетна структура та межі модулів

```text
com.tournado.core
├── common
│   ├── domain
│   │   └── Discipline.java               # Спільний доменний enum (CS2=5, BRAWL_STARS=3, CHESS=1)
│   ├── security
│   │   ├── AuthenticatedUser.java        # Record (userId, displayName, email, steamId)
│   │   ├── CurrentUserProvider.java      # Інтерфейс + SecurityContext-реалізація (DIP)
│   │   ├── JwtProperties.java            # @ConfigurationProperties(prefix = "tournado.jwt")
│   │   ├── JwtTokenService.java          # Генерація/валідація HS256 JWT + управління HttpOnly cookie
│   │   ├── JwtAuthenticationFilter.java  # Читання X-Auth-Token cookie або Bearer-заголовка
│   │   ├── RestAuthenticationEntryPoint.java # RFC 7807 401 AUTH_UNAUTHORIZED / AUTH_SESSION_EXPIRED
│   │   └── SecurityConfig.java           # Stateless SecurityFilterChain (без CSRF-сесій на REST, публічні GET)
│   └── exception
│       └── GlobalExceptionHandler.java   # Існуючий RFC 7807 обробник + DomainException
│
├── identity
│   ├── api
│   │   ├── AuthController.java           # steam/login, steam/callback, email/request-link, email/verify, me, logout
│   │   ├── UserProfileController.java    # PATCH /api/v1/users/me/game-identifiers
│   │   └── dto/                          # EmailLoginRequest, UpdateGameIdentifiersRequest, UserProfileResponse
│   ├── domain
│   │   ├── UserEntity.java               # Таблиця users
│   │   ├── EmailLoginTokenEntity.java    # Таблиця email_login_tokens
│   │   └── GameIdentifierValidator.java  # Валідація форматів SteamID64, SupercellTag, ChessUsername
│   ├── exception                         # SteamAuthFailedException, EmailMagicLinkExpiredException, InvalidGameIdentifierException
│   ├── infra
│   │   ├── SteamOpenIdClient.java        # Інтерфейс (DIP) перевірки OpenID 2.0
│   │   ├── HttpSteamOpenIdClient.java    # Реальний HTTP клієнт check_authentication до steamcommunity.com
│   │   ├── MagicLinkMailSender.java      # Інтерфейс (DIP) відправки листа
│   │   └── SmtpMagicLinkMailSender.java  # Реалізація через Spring JavaMailSender -> Mailpit
│   ├── repository
│   │   ├── UserRepository.java           # package-private/module-internal
│   │   └── EmailLoginTokenRepository.java
│   └── service
│       ├── EmailMagicLinkService.java    # SHA-256 хешування токена, одноразовість, TTL 15 хв
│       ├── SteamAuthService.java         # OpenID redirect URL, callback verify, find-or-create user (BR-02)
│       ├── UserProfileService.java       # Оновлення ігрових ідентифікаторів (US-02)
│       └── UserQueryService.java         # Публічний інтерфейс для teams та tournaments
│
├── teams
│   ├── api
│   │   ├── TeamController.java           # create, my, getById, createInvite, previewInvite, joinByToken
│   │   └── dto/                          # CreateTeamRequest, TeamSummaryResponse, TeamDetailsResponse, TeamInviteResponse
│   ├── domain
│   │   ├── TeamEntity.java               # Таблиця teams (@Version Long version, UUID PK)
│   │   ├── TeamMemberEntity.java         # Таблиця team_members
│   │   └── TeamInviteEntity.java         # Таблиця team_invites
│   ├── exception                         # TeamNotFoundException, TeamNameAlreadyExistsException, RosterFullException, ...
│   ├── repository                        # TeamRepository, TeamMemberRepository, TeamInviteRepository
│   └── service
│       ├── TeamService.java              # Створення, перегляд, перевірка повноти ростеру (US-03, US-06)
│       ├── TeamInviteService.java        # Інвайти та вступ (US-04, US-05), перевірка блокування ростеру
│       └── TeamQueryService.java         # Публічний інтерфейс для tournaments (без витоку репозиторіїв)
│
└── tournaments
    ├── ... (існуючі класи турнірів)
    ├── domain
    │   ├── RegistrationEntity.java       # Таблиця registrations
    │   ├── RegistrationStatus.java       # REGISTERED, CHECKED_IN, EXCLUDED
    │   └── RosterSnapshotEntity.java     # Таблиця registration_roster_snapshots
    ├── repository
    │   ├── RegistrationRepository.java
    │   └── RosterSnapshotRepository.java
    └── service
        ├── GameIdentifierExtractor.java  # Поліморфна мапа (OCP/Strategy) для витягу ID гравця по Discipline
        ├── TournamentRosterLockChecker.java # Інтерфейс перевірки чи ростер заблокований (US-05 Sc.5 / US-14)
        └── RegistrationService.java      # US-12, US-13, US-14
```

### 2.2. Як застосовано SOLID
* **Single Responsibility (SRP):** кожен сценарій має окремий сфокусований сервіс (`EmailMagicLinkService`, `SteamAuthService`, `UserProfileService`, `TeamService`, `TeamInviteService`, `RegistrationService`) без «God Service».
* **Open/Closed (OCP):** вибір і перевірка обов’язкового ігрового ідентифікатора під дисципліну (`CS2 -> steamId`, `BRAWL_STARS -> supercellPlayerTag`, `CHESS -> chessUsername`) інкапсульовані у стратегії/мапі на базі `Discipline`, без розгалужень `if/else if`.
* **Liskov Substitution (LSP) & Interface Segregation (ISP):** вузькі інтерфейси `UserQueryService`, `TeamQueryService`, `CurrentUserProvider`, `SteamOpenIdClient`, `MagicLinkMailSender` містять тільки методи, потрібні споживачам.
* **Dependency Inversion (DIP):** бізнес-логіка залежить від абстракцій (`SteamOpenIdClient`, `MagicLinkMailSender`, `Clock`, `TeamQueryService`, `UserQueryService`), що робить юніт-тести повністю ізольованими від SMTP, Steam і системного часу.

---

## 3. Узгодженість зі схемою БД (`V1__initial_schema.sql`) та контрактами

* **Перевірка Flyway / Hibernate (`ddl-auto: validate`):**
  * `users` (без `version`), `email_login_tokens`, `teams` (**з `@Version Long version`**), `team_members`, `team_invites`, `registrations`, `registration_roster_snapshots` уже присутні у `V1__initial_schema.sql`.
  * Для ідентифікації заблокованих заявок і збереження сумісності з `V1`: у `registrations` вже є `status`, `registered_at`, `checked_in_at`, `excluded_at`, `exclusion_reason`, а блокування ростеру фіксується наявністю рядків у `registration_roster_snapshots` + перевіркою `Instant.now(clock).isAfter(tournament.registrationDeadlineAt)` (згідно з `US-05` АС7 та `US-14`!). Нові таблиці не потрібні; додавати зайві колонки поза `docs/contracts/data-schema.md` заборонено.
* **Каталог кодів помилок RFC 7807 (`docs/contracts/error-format.md`):**
  * `401`: `AUTH_UNAUTHORIZED`, `AUTH_SESSION_EXPIRED`, `STEAM_AUTH_FAILED`
  * `400`: `EMAIL_MAGIC_LINK_EXPIRED`, `INVITE_LINK_EXPIRED`
  * `403`: `AUTH_FORBIDDEN`, `ONLY_CAPTAIN_ALLOWED`
  * `404`: `TOURNAMENT_NOT_FOUND`, `TEAM_NOT_FOUND`
  * `409`: `TEAM_NAME_ALREADY_EXISTS`, `ROSTER_FULL`, `PLAYER_ALREADY_IN_TEAM`, `ROSTER_LOCKED`, `ALREADY_REGISTERED`, `REGISTRATION_CLOSED`
  * `422`: `INVALID_GAME_IDENTIFIER`, `ROSTER_NOT_FULL`, `MISSING_GAME_IDENTIFIER`, `TEAM_DISCIPLINE_IMMUTABLE`

---

## 4. REST API Контракти та Правила Доступу

| Ендпоінт | Доступ | Опис |
| :--- | :---: | :--- |
| `GET /api/v1/auth/steam/login` | Public | Редірект на Steam OpenID 2.0 |
| `GET /api/v1/auth/steam/callback` | Public | Верифікація OpenID `check_authentication`, створення/пошук юзера, cookie `X-Auth-Token`, редірект на `/` |
| `POST /api/v1/auth/email/request-link` | Public | Генерація токена, запис SHA-256 у `email_login_tokens`, лист у Mailpit |
| `GET /api/v1/auth/email/verify?token=...` | Public | Перевірка одноразового токена, встановлення cookie `X-Auth-Token`, редірект на `/` |
| `GET /api/v1/auth/me` | Auth | Профіль поточного користувача (або `401 AUTH_UNAUTHORIZED`) |
| `POST /api/v1/auth/logout` | Public | Очищення `HttpOnly` cookie `X-Auth-Token` |
| `PATCH /api/v1/users/me/game-identifiers` | Auth | Оновлення/очищення `steamId`, `supercellPlayerTag`, `chessUsername` |
| `POST /api/v1/teams` | Auth | Створення команди; творець стає капітаном і 1-м учасником |
| `GET /api/v1/teams/my` | Auth | Команди, у яких користувач є учасником або капітаном |
| `GET /api/v1/teams/{teamId}` | Public | Деталі команди, список гравців, `memberCount / maxMembers / isFull` |
| `POST /api/v1/teams/{teamId}/invites` | Captain | Генерація активного інвайт-посилання (заборонено при повному ростері) |
| `GET /api/v1/teams/invites/{token}` | Public | Інформація про команду для прев’ю інвайту перед підтвердженням (`US-05`) |
| `POST /api/v1/teams/join/{token}` | Auth | Вступ у команду за інвайтом (перевірка `ROSTER_FULL`, `PLAYER_ALREADY_IN_TEAM`, `ROSTER_LOCKED`) |
| `POST /api/v1/tournaments` | Auth | Створення турніру з `createdByUserId` поточного користувача |
| `POST /api/v1/tournaments/{id}/register` | Captain | Подача заявки повної команди з перевіркою ігрових ID усіх гравців (`US-12`) |
| `GET /api/v1/tournaments/{id}/registrations` | Public | Список зареєстрованих команд і кількість зліпків |
| `DELETE /api/v1/tournaments/{id}/registrations/{teamId}` | Organizer | Видалення команди організатором до початку ігор (`US-13`) |
| `POST /api/v1/tournaments/{id}/lock-rosters` | Organizer | Заморозка складів у `registration_roster_snapshots` (`US-14`) |

---

## 5. Архітектура фронтенду (`frontend/` — Broadcast Studio UI)

1. **`AuthContext` (`src/context/AuthContext.tsx`):**
   * Завантажує `GET /api/v1/auth/me` при старті (з підтримкою cookie `credentials: 'include'`), надає `user`, `isLoading`, `refreshUser`, `logout`, `openAuthModal`.
2. **Хедер у `App.tsx`:**
   * Для гостя — кнопка **«Увійти»**, яка відкриває стильну модалку `AuthModal.tsx` (Steam-кнопка + форма Magic Link поштою з підказкою про поштову скриньку Mailpit на `:8025` і зрозумілим повідомленням, якщо Steam-вхід скасовано).
   * Для авторизованого — чіп користувача в шапці з переходом у **«Профіль»** (`ProfileModal.tsx`) та навігацією **«Мої команди»** (`TeamsDrawer.tsx` або секція).
3. **`ProfileModal.tsx` (`US-02`):**
   * Форма прив’язки `Steam ID`, `Supercell Player Tag`, `Chess.com Username` з миттєвим відображенням помилок RFC 7807 біля полів.
4. **`TeamsDrawer.tsx` + `JoinInviteModal.tsx` (`US-03`..`US-06`):**
   * Створення команди (назва + вибір дисципліни через логотипи), індикатор заповненості `N із M` (і повідомлення про статус «Склад повний» / скільки гравців не вистачає), кнопка генерації та копіювання інвайту для капітана.
   * Автоматична обробка URL `/join/{token}`: показує назву та дисципліну команди; якщо гість не увійшов — пропонує спочатку авторизуватися, а після входу дозволяє натиснути **«Підтвердити вступ»**.
5. **Оновлення `TournamentDrawer.tsx` (`US-12`..`US-14`):**
   * Вкладка або блок **«Команди та реєстрація»**: показує перелік заявлених команд; для капітана — вибір своєї команди відповідної дисципліни та кнопку **«Зареєструвати команду»**; для організатора турніру — кнопку **«Видалити»** (з підтвердженням) і **«Зафіксувати склади»**.
   * При спробі створити турнір неавторизованим користувачем (`US-01` Сценарій 5) — показується `AuthModal`.

---

## 6. Вимоги до якості коду, тестів і лінтерів

### 6.1. Backend (`core/src/test/...`)
* Іменування тестів: виключно `givenX_y_shouldZ`, **без слова `when`** у назві дії `y`.
* Тільки AssertJ (`assertThat(...)`), порівняння цілих об’єктів/DTO проти фікстур у `*TestResources` через `assertThat(actual).usingRecursiveComparison().isEqualTo(expected)` (`.ignoringFields(...)` лише для випадкових UUID/timestamp). Жодних JUnit `assertEquals`/`assertTrue` і по-полевих перевірок.
* Статичні `*`-імпорти (`Assertions.*`, `Mockito.*`, `MockMvcRequestBuilders.*`, `MockMvcResultMatchers.*`, `*TestResources.*`).
* Один порожній рядок між фазами (без `// Given`, `// When`, `// Then`).
* Без зайвих `verify(...)` на методах, що вже замокані через `when(...).thenReturn(...)`.
* MockMvc-тести перевіряють статус/хедери без `jsonPath(...)`, десеріалізуючи `ProblemDetail` / DTO через `ObjectMapper`.
* `mvn clean test checkstyle:check -DargLine="-Dnet.bytebuddy.experimental=true"` має проходити з **0 порушень Checkstyle** та **0 попереджень `-Xlint`**.

### 6.2. Frontend (`frontend/`)
* TypeScript strict, без дублювання логіки, чисті векторні іконки без системних емодзі та `•`-маркерів.
* Покриття Vitest + Testing Library для нових компонентів і API-клієнтів.
* `npm run lint` (`eslint . --max-warnings 0`), `npm test`, `npx tsc --noEmit`, `npm run build` — **0 помилок і 0 попереджень**.

# Tournament Creation & Viewing Slice (US-07..US-11) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the first end-to-end vertical slice (Tournament Creation, Listing, and Public Details) across all layers: React 19 Frontend -> Spring Boot 3.3 Core -> PostgreSQL 16.

**Architecture:** Package-by-feature Modular Monolith in Spring Boot Core (`com.tournado.core.tournaments`), RFC 9457 ProblemDetail error handling, Spring Data JPA with Flyway migrations, and a responsive Tailwind CSS React 19 UI with deterministic TypeScript type checking.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Spring Data JPA, Flyway, PostgreSQL 16, AssertJ, Mockito, MockMvc, React 19, TypeScript, Tailwind CSS, Vite.

**Spec:** `docs/analysis/requirements-digest.md`, `docs/contracts/api-contracts.md` (Sections 3.1 & 3.2), `docs/contracts/data-schema.md`, `docs/contracts/error-format.md`.

## Global Constraints

- **Single Source of Truth:** Data structures must match `docs/contracts/data-schema.md` and `docs/contracts/api-contracts.md` exactly.
- **Error Format:** All client-facing errors must follow `docs/contracts/error-format.md` using Spring Boot `ProblemDetail` (RFC 9457).
- **Persistence:** Real persistence in PostgreSQL; data must survive container restarts.
- **Testing Standards:**
  - Method naming: `givenX_y_shouldZ` (`y` is the action, NEVER use the word `when` in `y`).
  - Assertions: AssertJ (`assertThat(...)`) only; never JUnit/Hamcrest assertions.
  - Object comparison: `assertThat(actual).usingRecursiveComparison().isEqualTo(expected)`.
  - Visual formatting: Logical test phases separated by a single blank line. No phase header comments.
  - Star imports: Static wildcard imports for test APIs (`org.assertj.core.api.Assertions.*`, `org.mockito.Mockito.*`, `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*`, `org.springframework.test.web.servlet.result.MockMvcResultMatchers.*`).
  - Fixtures: Kept in dedicated `TournamentTestResources`.
- **Git & Workflow:** Feature branch `feat/tournament-creation-slice`, PR on GitHub, AI Code Review audit trace, prompts saved in `prompts/5.1_code_generation.md`, `prompts/5.2_fixing_and_refactoring.md`, `prompts/5.3_ai_code_review.md`.

---

## File Structure Map

```
Tournado/
├── core/
│   ├── src/main/resources/db/migration/
│   │   └── V2__seed_demo_organizer.sql
│   ├── src/main/java/com/tournado/core/
│   │   ├── common/
│   │   │   ├── api/GlobalExceptionHandler.java
│   │   │   └── exception/BusinessRuleException.java
│   │   └── tournaments/
│   │       ├── api/
│   │       │   ├── TournamentController.java
│   │       │   └── dto/
│   │       │       ├── CreateTournamentRequest.java
│   │       │       ├── TournamentResponse.java
│   │       │       └── TournamentDetailResponse.java
│   │       ├── domain/
│   │       │   ├── Discipline.java
│   │       │   ├── TournamentFormat.java
│   │       │   ├── TournamentStatus.java
│   │       │   ├── Tournament.java
│   │       │   ├── TournamentMapPool.java
│   │       │   └── TournamentRepository.java
│   │       └── service/
│   │           └── TournamentService.java
│   └── src/test/java/com/tournado/core/tournaments/
│       ├── TournamentTestResources.java
│       ├── service/TournamentServiceTest.java
│       └── api/TournamentControllerTest.java
├── frontend/
│   └── src/
│       ├── types/
│       │   └── tournament.ts
│       ├── api/
│       │   └── tournamentApi.ts
│       ├── components/
│       │   ├── CreateTournamentModal.tsx
│       │   ├── TournamentCard.tsx
│       │   └── TournamentDetailModal.tsx
│       └── App.tsx
├── prompts/
│   ├── 5.1_code_generation.md
│   ├── 5.2_fixing_and_refactoring.md
│   └── 5.3_ai_code_review.md
└── README.md
```

---

## Tasks

### Task 1: Seed Demo Organizer User & Prepare Feature Branch

**Files:**
- Create: `core/src/main/resources/db/migration/V2__seed_demo_organizer.sql`

**Interfaces:**
- Consumes: PostgreSQL `users` table from `V1__initial_schema.sql`
- Produces: Persistent user `e4b2d184-f376-476c-9226-64d852a39a01` for foreign key `tournaments.created_by_user_id`

- [ ] **Step 1: Create feature branch**

```bash
git checkout -b feat/tournament-creation-slice
```

- [ ] **Step 2: Write Flyway migration `V2__seed_demo_organizer.sql`**

```sql
INSERT INTO users (id, email, steam_id, display_name, avatar_url)
VALUES ('e4b2d184-f376-476c-9226-64d852a39a01', 'organizer@tournado.local', '76561198000000001', 'Demo Organizer', 'https://avatars.steamstatic.com/default.jpg')
ON CONFLICT (id) DO NOTHING;
```

- [ ] **Step 3: Commit migration**

```bash
git add core/src/main/resources/db/migration/V2__seed_demo_organizer.sql
git commit -m "chore: add V2 flyway migration to seed demo organizer user"
```

---

### Task 2: Common RFC 9457 Error Handling

**Files:**
- Create: `core/src/main/java/com/tournado/core/common/exception/BusinessRuleException.java`
- Create: `core/src/main/java/com/tournado/core/common/api/GlobalExceptionHandler.java`

**Interfaces:**
- Consumes: Spring Boot `ProblemDetail`, `MethodArgumentNotValidException`
- Produces: Consistent JSON error responses with `code`, `timestamp`, and `invalidParams` matching `docs/contracts/error-format.md`

- [ ] **Step 1: Create `BusinessRuleException`**

```java
package com.tournado.core.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessRuleException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public BusinessRuleException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
```

- [ ] **Step 2: Create `GlobalExceptionHandler`**

```java
package com.tournado.core.common.api;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.tournado.core.common.exception.BusinessRuleException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleBusinessRuleException(BusinessRuleException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setType(URI.create("https://tournado.ua/errors/" + ex.getCode().toLowerCase()));
        problem.setTitle(ex.getCode());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", ex.getCode());
        problem.setProperty("timestamp", Instant.now().toString());
        return ResponseEntity.status(ex.getStatus()).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Помилка валідації вхідних даних");
        problem.setType(URI.create("https://tournado.ua/errors/validation_error"));
        problem.setTitle("VALIDATION_ERROR");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", "VALIDATION_ERROR");
        problem.setProperty("timestamp", Instant.now().toString());

        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("name", err.getField(), "reason", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"))
                .toList();
        problem.setProperty("invalidParams", invalidParams);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }
}
```

- [ ] **Step 3: Commit common error handling**

```bash
git add core/src/main/java/com/tournado/core/common/
git commit -m "feat: add RFC 9457 ProblemDetail exception handler and BusinessRuleException"
```

---

### Task 3: Tournaments Domain Entities & Enums

**Files:**
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/Discipline.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/TournamentFormat.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/TournamentStatus.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/Tournament.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/TournamentMapPool.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/domain/TournamentRepository.java`

**Interfaces:**
- Consumes: Tables `tournaments`, `tournament_map_pool` from `V1__initial_schema.sql`
- Produces: JPA Repository and Entities matching Flyway schema constraints

- [ ] **Step 1: Create Enums (`Discipline`, `TournamentFormat`, `TournamentStatus`)**

`Discipline.java`:
```java
package com.tournado.core.tournaments.domain;

import java.util.List;

public enum Discipline {
    CS2(5, List.of("Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II", "Vertigo")),
    BRAWL_STARS(3, List.of("Center Stage", "Hard Rock Mine", "Sneaky Fields", "Double Swoop", "Kaboom Canyon")),
    CHESS(1, List.of("Standard 10+0"));

    private final int teamSize;
    private final List<String> defaultMapPool;

    Discipline(int teamSize, List<String> defaultMapPool) {
        this.teamSize = teamSize;
        this.defaultMapPool = defaultMapPool;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public List<String> getDefaultMapPool() {
        return defaultMapPool;
    }
}
```

`TournamentFormat.java`:
```java
package com.tournado.core.tournaments.domain;

public enum TournamentFormat {
    SINGLE_ELIMINATION
}
```

`TournamentStatus.java`:
```java
package com.tournado.core.tournaments.domain;

public enum TournamentStatus {
    DRAFT,
    REGISTRATION_OPEN,
    CHECK_IN_ACTIVE,
    IN_PROGRESS,
    COMPLETED
}
```

- [ ] **Step 2: Create JPA Entities (`Tournament`, `TournamentMapPool`)**

`Tournament.java`:
```java
package com.tournado.core.tournaments.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "tournaments")
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Discipline discipline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;

    @Column(name = "slot_limit", nullable = false)
    private Integer slotLimit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TournamentStatus status = TournamentStatus.REGISTRATION_OPEN;

    @Column(name = "registration_deadline_at", nullable = false)
    private Instant registrationDeadlineAt;

    @Column(name = "check_in_opens_at", nullable = false)
    private Instant checkInOpensAt;

    @Column(name = "start_time_at", nullable = false)
    private Instant startTimeAt;

    @Column(name = "cs2_server_connect", columnDefinition = "TEXT")
    private String cs2ServerConnect;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<TournamentMapPool> mapPool = new ArrayList<>();

    public Tournament() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Discipline getDiscipline() { return discipline; }
    public void setDiscipline(Discipline discipline) { this.discipline = discipline; }

    public TournamentFormat getFormat() { return format; }
    public void setFormat(TournamentFormat format) { this.format = format; }

    public Integer getSlotLimit() { return slotLimit; }
    public void setSlotLimit(Integer slotLimit) { this.slotLimit = slotLimit; }

    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }

    public Instant getRegistrationDeadlineAt() { return registrationDeadlineAt; }
    public void setRegistrationDeadlineAt(Instant registrationDeadlineAt) { this.registrationDeadlineAt = registrationDeadlineAt; }

    public Instant getCheckInOpensAt() { return checkInOpensAt; }
    public void setCheckInOpensAt(Instant checkInOpensAt) { this.checkInOpensAt = checkInOpensAt; }

    public Instant getStartTimeAt() { return startTimeAt; }
    public void setStartTimeAt(Instant startTimeAt) { this.startTimeAt = startTimeAt; }

    public String getCs2ServerConnect() { return cs2ServerConnect; }
    public void setCs2ServerConnect(String cs2ServerConnect) { this.cs2ServerConnect = cs2ServerConnect; }

    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<TournamentMapPool> getMapPool() { return mapPool; }
    public void setMapPool(List<TournamentMapPool> mapPool) { this.mapPool = mapPool; }

    public void addMap(String mapName, int sortOrder) {
        TournamentMapPool map = new TournamentMapPool(this, mapName, sortOrder);
        mapPool.add(map);
    }
}
```

`TournamentMapPool.java`:
```java
package com.tournado.core.tournaments.domain;

import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "tournament_map_pool")
public class TournamentMapPool {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "map_name", nullable = false, length = 64)
    private String mapName;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public TournamentMapPool() {}

    public TournamentMapPool(Tournament tournament, String mapName, Integer sortOrder) {
        this.tournament = tournament;
        this.mapName = mapName;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public Tournament getTournament() { return tournament; }
    public String getMapName() { return mapName; }
    public Integer getSortOrder() { return sortOrder; }
}
```

`TournamentRepository.java`:
```java
package com.tournado.core.tournaments.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, UUID> {
    List<Tournament> findAllByOrderByCreatedAtDesc();
}
```

- [ ] **Step 3: Commit Domain Entities**

```bash
git add core/src/main/java/com/tournado/core/tournaments/domain/
git commit -m "feat: add Tournament and TournamentMapPool entities and repository"
```

---

### Task 4: DTOs & Validation Contracts

**Files:**
- Create: `core/src/main/java/com/tournado/core/tournaments/api/dto/CreateTournamentRequest.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/api/dto/TournamentResponse.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/api/dto/TournamentDetailResponse.java`

**Interfaces:**
- Consumes: JSON input from client adhering to `docs/contracts/api-contracts.md` §3.1 & §3.2
- Produces: Strictly typed records with validation annotations (`@NotBlank`, `@NotNull`, `@Min`, `@Max`)

- [ ] **Step 1: Create `CreateTournamentRequest`**

```java
package com.tournado.core.tournaments.api.dto;

import java.time.Instant;
import com.tournado.core.tournaments.domain.Discipline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTournamentRequest(
    @NotBlank(message = "Назва турніру обов'язкова")
    @Size(min = 3, max = 150, message = "Назва турніру має містити від 3 до 150 символів")
    String name,

    String description,

    @NotNull(message = "Дисципліна обов'язкова")
    Discipline discipline,

    @NotNull(message = "Ліміт слотів обов'язковий")
    Integer slotLimit,

    @NotNull(message = "Дедлайн реєстрації обов'язковий")
    Instant registrationDeadlineAt,

    @NotNull(message = "Час відкриття check-in обов'язковий")
    Instant checkInOpensAt,

    @NotNull(message = "Час початку турніру обов'язковий")
    Instant startTimeAt,

    String cs2ServerConnect
) {}
```

- [ ] **Step 2: Create `TournamentResponse` & `TournamentDetailResponse`**

`TournamentResponse.java`:
```java
package com.tournado.core.tournaments.api.dto;

import java.util.List;
import java.util.UUID;
import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;

public record TournamentResponse(
    UUID id,
    String publicSlug,
    String name,
    Discipline discipline,
    int teamSize,
    TournamentFormat format,
    int slotLimit,
    TournamentStatus status,
    List<String> mapPool,
    int registeredTeamsCount
) {}
```

`TournamentDetailResponse.java`:
```java
package com.tournado.core.tournaments.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;

public record TournamentDetailResponse(
    UUID id,
    String publicSlug,
    String name,
    String description,
    Discipline discipline,
    int teamSize,
    TournamentFormat format,
    int slotLimit,
    TournamentStatus status,
    Instant registrationDeadlineAt,
    Instant checkInOpensAt,
    Instant startTimeAt,
    String cs2ServerConnect,
    List<String> mapPool,
    int registeredTeamsCount,
    Instant createdAt
) {}
```

- [ ] **Step 3: Commit DTOs**

```bash
git add core/src/main/java/com/tournado/core/tournaments/api/dto/
git commit -m "feat: add Tournament DTOs for creation, listing, and detail views"
```

---

### Task 5: Business Logic Service & TDD

**Files:**
- Create: `core/src/test/java/com/tournado/core/tournaments/TournamentTestResources.java`
- Create: `core/src/test/java/com/tournado/core/tournaments/service/TournamentServiceTest.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/service/TournamentService.java`

**Interfaces:**
- Consumes: `TournamentRepository`, `CreateTournamentRequest`
- Produces: Validated and persisted `TournamentResponse`, `TournamentDetailResponse`

- [ ] **Step 1: Create `TournamentTestResources`**

```java
package com.tournado.core.tournaments;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentDetailResponse;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;

public final class TournamentTestResources {
    public static final UUID DEMO_ORGANIZER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");

    public static CreateTournamentRequest validCs2TournamentRequest() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return new CreateTournamentRequest(
            "KMA CS2 Autumn Cup 2026",
            "Official faculty tournament. Overtime rules: MR3 $10k.",
            Discipline.CS2,
            16,
            now.plus(1, ChronoUnit.DAYS),
            now.plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS),
            now.plus(1, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS),
            "connect 194.44.12.5:27015; password kma_cup"
        );
    }

    public static CreateTournamentRequest invalidScheduleTournamentRequest() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return new CreateTournamentRequest(
            "Invalid Schedule Cup",
            "Bad dates",
            Discipline.CS2,
            16,
            now.plus(2, ChronoUnit.DAYS),
            now.plus(1, ChronoUnit.DAYS),
            now.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS),
            null
        );
    }

    public static TournamentResponse expectedCs2TournamentResponse(UUID id) {
        return new TournamentResponse(
            id,
            id.toString(),
            "KMA CS2 Autumn Cup 2026",
            Discipline.CS2,
            5,
            TournamentFormat.SINGLE_ELIMINATION,
            16,
            TournamentStatus.REGISTRATION_OPEN,
            List.of("Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II", "Vertigo"),
            0
        );
    }
}
```

- [ ] **Step 2: Write failing unit test `TournamentServiceTest`**

```java
package com.tournado.core.tournaments.service;

import java.util.Optional;
import java.util.UUID;
import com.tournado.core.common.exception.BusinessRuleException;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.Tournament;
import com.tournado.core.tournaments.domain.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void givenValidRequest_createTournament_shouldPersistAndReturnResponse() {
        CreateTournamentRequest request = validCs2TournamentRequest();
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament t = invocation.getArgument(0);
            t.setId(TOURNAMENT_ID);
            return t;
        });

        TournamentResponse actual = tournamentService.createTournament(request, DEMO_ORGANIZER_ID);

        TournamentResponse expected = expectedCs2TournamentResponse(TOURNAMENT_ID);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidScheduleOrder_createTournament_shouldThrowBusinessRuleException() {
        CreateTournamentRequest request = invalidScheduleTournamentRequest();

        assertThatThrownBy(() -> tournamentService.createTournament(request, DEMO_ORGANIZER_ID))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Дедлайн реєстрації має бути раніше за відкриття check-in");
    }

    @Test
    void givenInvalidSlotLimit_createTournament_shouldThrowBusinessRuleException() {
        CreateTournamentRequest request = new CreateTournamentRequest(
            "Invalid Slots", null, com.tournado.core.tournaments.domain.Discipline.CHESS,
            12,
            java.time.Instant.now().plus(1, java.time.temporal.ChronoUnit.HOURS),
            java.time.Instant.now().plus(2, java.time.temporal.ChronoUnit.HOURS),
            java.time.Instant.now().plus(3, java.time.temporal.ChronoUnit.HOURS),
            null
        );

        assertThatThrownBy(() -> tournamentService.createTournament(request, DEMO_ORGANIZER_ID))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Ліміт слотів для Single Elimination має бути 8, 16 або 32");
    }
}
```

- [ ] **Step 3: Run test to confirm it fails**

```bash
cd core; ./mvnw test -Dtest=TournamentServiceTest
```
Expected: Compilation failure / Class `TournamentService` not found.

- [ ] **Step 4: Implement `TournamentService`**

```java
package com.tournado.core.tournaments.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.tournado.core.common.exception.BusinessRuleException;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentDetailResponse;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.Tournament;
import com.tournado.core.tournaments.domain.TournamentMapPool;
import com.tournado.core.tournaments.domain.TournamentRepository;
import com.tournado.core.tournaments.domain.TournamentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TournamentService {

    private static final Set<Integer> ALLOWED_SLOTS = Set.of(8, 16, 32);

    private final TournamentRepository tournamentRepository;

    public TournamentService(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    public TournamentResponse createTournament(CreateTournamentRequest req, UUID organizerUserId) {
        validateBusinessRules(req);

        Tournament tournament = new Tournament();
        tournament.setName(req.name());
        tournament.setDescription(req.description());
        tournament.setDiscipline(req.discipline());
        tournament.setSlotLimit(req.slotLimit());
        tournament.setStatus(TournamentStatus.REGISTRATION_OPEN);
        tournament.setRegistrationDeadlineAt(req.registrationDeadlineAt());
        tournament.setCheckInOpensAt(req.checkInOpensAt());
        tournament.setStartTimeAt(req.startTimeAt());
        tournament.setCs2ServerConnect(req.cs2ServerConnect());
        tournament.setCreatedByUserId(organizerUserId);

        List<String> defaultPool = req.discipline().getDefaultMapPool();
        for (int i = 0; i < defaultPool.size(); i++) {
            tournament.addMap(defaultPool.get(i), i + 1);
        }

        Tournament saved = tournamentRepository.save(tournament);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TournamentResponse> getAllTournaments() {
        return tournamentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TournamentDetailResponse getTournamentById(UUID id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("TOURNAMENT_NOT_FOUND", "Турнір з ідентифікатором " + id + " не знайдено", HttpStatus.NOT_FOUND));

        return toDetailResponse(tournament);
    }

    private void validateBusinessRules(CreateTournamentRequest req) {
        if (!ALLOWED_SLOTS.contains(req.slotLimit())) {
            throw new BusinessRuleException("INVALID_SLOT_LIMIT", "Ліміт слотів для Single Elimination має бути 8, 16 або 32", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!req.registrationDeadlineAt().isBefore(req.checkInOpensAt()) || !req.checkInOpensAt().isBefore(req.startTimeAt())) {
            throw new BusinessRuleException("INVALID_SCHEDULE_ORDER", "Дедлайн реєстрації має бути раніше за відкриття check-in, а check-in раніше за початок", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private TournamentResponse toResponse(Tournament t) {
        List<String> maps = t.getMapPool().stream()
                .map(TournamentMapPool::getMapName)
                .toList();

        return new TournamentResponse(
                t.getId(),
                t.getId().toString(),
                t.getName(),
                t.getDiscipline(),
                t.getDiscipline().getTeamSize(),
                t.getFormat(),
                t.getSlotLimit(),
                t.getStatus(),
                maps,
                0
        );
    }

    private TournamentDetailResponse toDetailResponse(Tournament t) {
        List<String> maps = t.getMapPool().stream()
                .map(TournamentMapPool::getMapName)
                .toList();

        return new TournamentDetailResponse(
                t.getId(),
                t.getId().toString(),
                t.getName(),
                t.getDescription(),
                t.getDiscipline(),
                t.getDiscipline().getTeamSize(),
                t.getFormat(),
                t.getSlotLimit(),
                t.getStatus(),
                t.getRegistrationDeadlineAt(),
                t.getCheckInOpensAt(),
                t.getStartTimeAt(),
                t.getCs2ServerConnect(),
                maps,
                0,
                t.getCreatedAt()
        );
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd core; ./mvnw test -Dtest=TournamentServiceTest
```
Expected: Tests pass (3 passed, 0 failures).

- [ ] **Step 6: Commit TournamentService**

```bash
git add core/src/main/java/com/tournado/core/tournaments/service/TournamentService.java core/src/test/java/com/tournado/core/tournaments/
git commit -m "feat: implement TournamentService with business rule validation and map pool assignment"
```

---

### Task 6: REST Controller & MockMvc Integration Tests

**Files:**
- Create: `core/src/test/java/com/tournado/core/tournaments/api/TournamentControllerTest.java`
- Create: `core/src/main/java/com/tournado/core/tournaments/api/TournamentController.java`

**Interfaces:**
- Consumes: HTTP requests on `/api/v1/tournaments`
- Produces: `201 Created` for `POST`, `200 OK` for `GET`, `422 Unprocessable Entity` on validation error, `404 Not Found` when tournament does not exist

- [ ] **Step 1: Write failing controller test `TournamentControllerTest`**

```java
package com.tournado.core.tournaments.api;

import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tournado.core.common.api.GlobalExceptionHandler;
import com.tournado.core.common.exception.BusinessRuleException;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.service.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;

@ExtendWith(MockitoExtension.class)
class TournamentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private TournamentController tournamentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tournamentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void givenValidPayload_createTournament_shouldReturn201AndTournamentResponse() throws Exception {
        CreateTournamentRequest request = validCs2TournamentRequest();
        TournamentResponse expected = expectedCs2TournamentResponse(TOURNAMENT_ID);
        when(tournamentService.createTournament(any(CreateTournamentRequest.class), any(UUID.class)))
                .thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TournamentResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), TournamentResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidSchedule_createTournament_shouldReturn422WithProblemDetail() throws Exception {
        CreateTournamentRequest request = invalidScheduleTournamentRequest();
        when(tournamentService.createTournament(any(CreateTournamentRequest.class), any(UUID.class)))
                .thenThrow(new BusinessRuleException("INVALID_SCHEDULE_ORDER", "Дедлайн реєстрації має бути раніше", HttpStatus.UNPROCESSABLE_ENTITY));

        mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_ORDER"));
    }

    @Test
    void givenExistingTournaments_getAllTournaments_shouldReturn200AndList() throws Exception {
        TournamentResponse item = expectedCs2TournamentResponse(TOURNAMENT_ID);
        when(tournamentService.getAllTournaments()).thenReturn(List.of(item));

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments"))
                .andExpect(status().isOk())
                .andReturn();

        List<TournamentResponse> actualList = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, TournamentResponse.class)
        );
        assertThat(actualList).usingRecursiveComparison().isEqualTo(List.of(item));
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
cd core; ./mvnw test -Dtest=TournamentControllerTest
```
Expected: Fail (`TournamentController` not found).

- [ ] **Step 3: Implement `TournamentController`**

```java
package com.tournado.core.tournaments.api;

import java.util.List;
import java.util.UUID;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentDetailResponse;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.service.TournamentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private static final UUID DEFAULT_DEMO_ORGANIZER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(@Valid @RequestBody CreateTournamentRequest request) {
        TournamentResponse response = tournamentService.createTournament(request, DEFAULT_DEMO_ORGANIZER_ID);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TournamentResponse>> getAllTournaments() {
        return ResponseEntity.ok(tournamentService.getAllTournaments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentDetailResponse> getTournamentById(@PathVariable UUID id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }
}
```

- [ ] **Step 4: Run controller test to verify it passes**

```bash
cd core; ./mvnw test -Dtest=TournamentControllerTest
```
Expected: Passes.

- [ ] **Step 5: Commit Controller and Tests**

```bash
git add core/src/main/java/com/tournado/core/tournaments/api/TournamentController.java core/src/test/java/com/tournado/core/tournaments/api/TournamentControllerTest.java
git commit -m "feat: implement TournamentController with POST and GET endpoints and tests"
```

---

### Task 7: React Frontend - Types, API Client & Components

**Files:**
- Create: `frontend/src/types/tournament.ts`
- Create: `frontend/src/api/tournamentApi.ts`
- Create: `frontend/src/components/CreateTournamentModal.tsx`
- Create: `frontend/src/components/TournamentCard.tsx`
- Create: `frontend/src/components/TournamentDetailModal.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: REST API `/api/v1/tournaments`
- Produces: Reactive interactive UI for creating tournaments, browsing the tournament grid, and viewing full tournament details.

- [ ] **Step 1: Create `frontend/src/types/tournament.ts`**

```typescript
export type Discipline = 'CS2' | 'BRAWL_STARS' | 'CHESS'
export type TournamentFormat = 'SINGLE_ELIMINATION'
export type TournamentStatus = 'DRAFT' | 'REGISTRATION_OPEN' | 'CHECK_IN_ACTIVE' | 'IN_PROGRESS' | 'COMPLETED'

export interface TournamentSummary {
  id: string
  publicSlug: string
  name: string
  discipline: Discipline
  teamSize: number
  format: TournamentFormat
  slotLimit: number
  status: TournamentStatus
  mapPool: string[]
  registeredTeamsCount: number
}

export interface TournamentDetail extends TournamentSummary {
  description?: string
  registrationDeadlineAt: string
  checkInOpensAt: string
  startTimeAt: string
  cs2ServerConnect?: string
  createdAt: string
}

export interface CreateTournamentPayload {
  name: string
  description?: string
  discipline: Discipline
  slotLimit: number
  registrationDeadlineAt: string
  checkInOpensAt: string
  startTimeAt: string
  cs2ServerConnect?: string
}

export interface ProblemDetail {
  type?: string
  title: string
  status: number
  detail: string
  instance?: string
  code?: string
  timestamp?: string
  invalidParams?: Array<{ name: string; reason: string }>
}
```

- [ ] **Step 2: Create `frontend/src/api/tournamentApi.ts`**

```typescript
import { CreateTournamentPayload, TournamentDetail, TournamentSummary, ProblemDetail } from '../types/tournament'

export async function fetchTournaments(): Promise<TournamentSummary[]> {
  const res = await fetch('/api/v1/tournaments')
  if (!res.ok) {
    throw new Error('Не вдалося завантажити турніри')
  }
  return res.json()
}

export async function fetchTournamentById(id: string): Promise<TournamentDetail> {
  const res = await fetch(`/api/v1/tournaments/${id}`)
  if (!res.ok) {
    throw new Error('Турнір не знайдено')
  }
  return res.json()
}

export async function createTournament(payload: CreateTournamentPayload): Promise<TournamentSummary> {
  const res = await fetch('/api/v1/tournaments', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  })

  if (!res.ok) {
    const errorData: ProblemDetail = await res.json().catch(() => ({
      title: 'Помилка',
      status: res.status,
      detail: 'Невідома помилка сервера'
    }))
    throw errorData
  }

  return res.json()
}
```

- [ ] **Step 3: Create UI Components (`TournamentCard`, `CreateTournamentModal`, `TournamentDetailModal`)**

`TournamentCard.tsx`:
```tsx
import { TournamentSummary } from '../types/tournament'

interface Props {
  tournament: TournamentSummary
  onSelect: (id: string) => void
}

const DISCIPLINE_LABELS: Record<string, { label: string; badgeClass: string; icon: string }> = {
  CS2: { label: 'Counter-Strike 2', badgeClass: 'bg-amber-950/60 text-amber-300 border-amber-800', icon: '🔫' },
  BRAWL_STARS: { label: 'Brawl Stars', badgeClass: 'bg-purple-950/60 text-purple-300 border-purple-800', icon: '⭐' },
  CHESS: { label: 'Chess.com', badgeClass: 'bg-emerald-950/60 text-emerald-300 border-emerald-800', icon: '♟️' }
}

export function TournamentCard({ tournament, onSelect }: Props) {
  const disc = DISCIPLINE_LABELS[tournament.discipline] || { label: tournament.discipline, badgeClass: 'bg-slate-800 text-slate-300', icon: '🎮' }

  return (
    <div
      onClick={() => onSelect(tournament.id)}
      className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-indigo-600/60 hover:shadow-lg hover:shadow-indigo-950/40 cursor-pointer transition flex flex-col justify-between"
    >
      <div>
        <div className="flex items-center justify-between gap-2 mb-3">
          <span className={`text-xs font-semibold px-2.5 py-1 rounded-full border ${disc.badgeClass} flex items-center gap-1.5`}>
            <span>{disc.icon}</span>
            <span>{disc.label}</span>
          </span>
          <span className="text-xs font-mono text-emerald-400 bg-emerald-950/40 px-2 py-0.5 rounded border border-emerald-800/60">
            {tournament.status}
          </span>
        </div>

        <h3 className="text-lg font-bold text-white mb-2 line-clamp-1">{tournament.name}</h3>

        <div className="space-y-1.5 text-xs text-slate-400">
          <div className="flex justify-between">
            <span>Формат:</span>
            <span className="text-slate-200 font-medium">Single Elimination</span>
          </div>
          <div className="flex justify-between">
            <span>Слоти:</span>
            <span className="text-slate-200 font-medium">{tournament.registeredTeamsCount} / {tournament.slotLimit} команд</span>
          </div>
          <div className="flex justify-between">
            <span>Склад команди:</span>
            <span className="text-slate-200 font-medium">{tournament.teamSize} гравців</span>
          </div>
        </div>
      </div>

      <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
        <span className="text-xs text-indigo-400 font-medium">Пул карт: {tournament.mapPool.length}</span>
        <span className="text-xs text-slate-400 hover:text-white transition">Детальніше &rarr;</span>
      </div>
    </div>
  )
}
```

`CreateTournamentModal.tsx`:
```tsx
import { useState } from 'react'
import { Discipline, CreateTournamentPayload, ProblemDetail } from '../types/tournament'
import { createTournament } from '../api/tournamentApi'

interface Props {
  isOpen: boolean
  onClose: () => void
  onCreated: () => void
}

export function CreateTournamentModal({ isOpen, onClose, onCreated }: Props) {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [discipline, setDiscipline] = useState<Discipline>('CS2')
  const [slotLimit, setSlotLimit] = useState<number>(16)
  const [regDeadline, setRegDeadline] = useState('')
  const [checkIn, setCheckIn] = useState('')
  const [startTime, setStartTime] = useState('')
  const [serverConnect, setServerConnect] = useState('')

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<ProblemDetail | null>(null)

  if (!isOpen) return null

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      const payload: CreateTournamentPayload = {
        name,
        description: description || undefined,
        discipline,
        slotLimit: Number(slotLimit),
        registrationDeadlineAt: new Date(regDeadline).toISOString(),
        checkInOpensAt: new Date(checkIn).toISOString(),
        startTimeAt: new Date(startTime).toISOString(),
        cs2ServerConnect: discipline === 'CS2' && serverConnect ? serverConnect : undefined
      }

      await createTournament(payload)
      onCreated()
      onClose()
    } catch (err) {
      setError(err as ProblemDetail)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-xl w-full p-6 text-slate-100 shadow-2xl my-8">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <h2 className="text-xl font-bold text-white">🏆 Створити новий турнір</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-2xl leading-none">&times;</button>
        </div>

        {error && (
          <div className="mt-4 p-3 bg-red-950/60 border border-red-800 rounded-xl text-xs text-red-200">
            <div className="font-bold">{error.title || 'Помилка валідації'}</div>
            <div>{error.detail}</div>
            {error.invalidParams && (
              <ul className="list-disc pl-4 mt-1 space-y-0.5">
                {error.invalidParams.map((p, i) => (
                  <li key={i}><span className="font-mono text-red-300">{p.name}</span>: {p.reason}</li>
                ))}
              </ul>
            )}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-5 space-y-4 text-sm">
          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Назва турніру *</label>
            <input
              type="text"
              required
              placeholder="наприклад: KMA CS2 Autumn Cup 2026"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Дисципліна *</label>
              <select
                value={discipline}
                onChange={(e) => setDiscipline(e.target.value as Discipline)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
              >
                <option value="CS2">Counter-Strike 2 (5v5)</option>
                <option value="BRAWL_STARS">Brawl Stars (3v3)</option>
                <option value="CHESS">Chess.com (1v1)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Кількість слотів *</label>
              <select
                value={slotLimit}
                onChange={(e) => setSlotLimit(Number(e.target.value))}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
              >
                <option value={8}>8 команд</option>
                <option value={16}>16 команд</option>
                <option value={32}>32 команди</option>
              </select>
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Опис турніру</label>
            <textarea
              rows={2}
              placeholder="Правила, регламент овертаймів, призовий фонд..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div className="space-y-3 pt-2 border-t border-slate-800/80">
            <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400">Розклад турніру (хронологічний порядок)</span>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] text-slate-400 mb-1">1. Дедлайн реєстрації *</label>
                <input
                  type="datetime-local"
                  required
                  value={regDeadline}
                  onChange={(e) => setRegDeadline(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div>
                <label className="block text-[11px] text-slate-400 mb-1">2. Відкриття Check-in *</label>
                <input
                  type="datetime-local"
                  required
                  value={checkIn}
                  onChange={(e) => setCheckIn(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>
              <div>
                <label className="block text-[11px] text-slate-400 mb-1">3. Початок турніру *</label>
                <input
                  type="datetime-local"
                  required
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2 py-1.5 text-xs text-white focus:outline-none focus:border-indigo-500"
                />
              </div>
            </div>
          </div>

          {discipline === 'CS2' && (
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Дані сервера CS2 (опціонально)</label>
              <input
                type="text"
                placeholder="connect 194.44.12.5:27015; password kma_cup"
                value={serverConnect}
                onChange={(e) => setServerConnect(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-white font-mono text-xs focus:outline-none focus:border-indigo-500"
              />
            </div>
          )}

          <div className="pt-4 flex items-center justify-end gap-3 border-t border-slate-800">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-lg text-slate-400 hover:text-white transition"
            >
              Скасувати
            </button>
            <button
              type="submit"
              disabled={loading}
              className="bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-5 py-2 rounded-lg shadow-lg shadow-indigo-600/30 transition disabled:opacity-50"
            >
              {loading ? 'Створення...' : 'Створити турнір'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
```

`TournamentDetailModal.tsx`:
```tsx
import { useEffect, useState } from 'react'
import { TournamentDetail } from '../types/tournament'
import { fetchTournamentById } from '../api/tournamentApi'

interface Props {
  tournamentId: string | null
  onClose: () => void
}

export function TournamentDetailModal({ tournamentId, onClose }: Props) {
  const [tournament, setTournament] = useState<TournamentDetail | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!tournamentId) return
    setLoading(true)
    fetchTournamentById(tournamentId)
      .then(setTournament)
      .catch(() => setTournament(null))
      .finally(() => setLoading(false))
  }, [tournamentId])

  if (!tournamentId) return null

  return (
    <div className="fixed inset-0 z-50 bg-black/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-2xl w-full p-6 text-slate-100 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div>
            <span className="text-xs font-semibold text-indigo-400 uppercase tracking-wider">Деталі турніру (US-11)</span>
            <h2 className="text-2xl font-bold text-white mt-1">{tournament?.name || 'Завантаження...'}</h2>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-2xl leading-none">&times;</button>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-400">Завантаження деталей турніру...</div>
        ) : tournament ? (
          <div className="mt-5 space-y-5 text-sm">
            {tournament.description && (
              <p className="text-slate-300 bg-slate-950/60 p-3 rounded-xl border border-slate-800/80">
                {tournament.description}
              </p>
            )}

            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
                <div className="text-xs text-slate-500">Дисципліна</div>
                <div className="font-bold text-white mt-0.5">{tournament.discipline}</div>
              </div>
              <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
                <div className="text-xs text-slate-500">Слоти</div>
                <div className="font-bold text-white mt-0.5">{tournament.registeredTeamsCount} / {tournament.slotLimit}</div>
              </div>
              <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
                <div className="text-xs text-slate-500">Формат</div>
                <div className="font-bold text-white mt-0.5">Single Elim.</div>
              </div>
              <div className="bg-slate-950 p-3 rounded-xl border border-slate-800">
                <div className="text-xs text-slate-500">Статус</div>
                <div className="font-bold text-emerald-400 mt-0.5">{tournament.status}</div>
              </div>
            </div>

            <div>
              <h4 className="text-xs font-semibold uppercase text-slate-400 mb-2">Автоматично призначений пул карт</h4>
              <div className="flex flex-wrap gap-2">
                {tournament.mapPool.map((map) => (
                  <span key={map} className="px-3 py-1 bg-slate-800 border border-slate-700 rounded-lg text-xs font-medium text-slate-200">
                    🗺️ {map}
                  </span>
                ))}
              </div>
            </div>

            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2 text-xs">
              <div className="font-semibold text-slate-300">Розклад:</div>
              <div className="flex justify-between text-slate-400">
                <span>Дедлайн прийому заявок:</span>
                <span className="font-mono text-slate-200">{new Date(tournament.registrationDeadlineAt).toLocaleString()}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Вікно Check-in:</span>
                <span className="font-mono text-slate-200">{new Date(tournament.checkInOpensAt).toLocaleString()}</span>
              </div>
              <div className="flex justify-between text-slate-400">
                <span>Старт першого раунду:</span>
                <span className="font-mono text-slate-200">{new Date(tournament.startTimeAt).toLocaleString()}</span>
              </div>
            </div>

            {tournament.cs2ServerConnect && (
              <div>
                <div className="text-xs text-slate-400 mb-1">Підключення до CS2 сервера:</div>
                <code className="block bg-slate-950 border border-slate-800 p-2.5 rounded-lg text-xs text-indigo-300 font-mono select-all">
                  {tournament.cs2ServerConnect}
                </code>
              </div>
            )}
          </div>
        ) : null}
      </div>
    </div>
  )
}
```

- [ ] **Step 4: Update `frontend/src/App.tsx`**

Integrate state for tournaments list, `CreateTournamentModal`, and `TournamentDetailModal`. Replace the static mockup with live data from `/api/v1/tournaments`.

- [ ] **Step 5: Type-check and lint frontend**

```bash
cd frontend; npx tsc --noEmit
```
Expected: 0 errors.

- [ ] **Step 6: Commit Frontend Changes**

```bash
git add frontend/
git commit -m "feat: implement tournament creation modal, card list, and detail modal in React"
```

---

### Task 8: End-to-End Verification & Persistence Test

- [ ] **Step 1: Build Core package**

```bash
cd core; ./mvnw clean package -DskipTests=false
```
Expected: BUILD SUCCESS.

- [ ] **Step 2: Start Docker Compose environment**

```bash
docker compose up -d --build
```
Verify all 5 containers are UP and healthy (`docker compose ps`).

- [ ] **Step 3: Execute end-to-end API test and verify DB persistence**

1. Send `POST /api/v1/tournaments` via curl or frontend.
2. Confirm `201 Created` with tournament ID and map pool.
3. Check PostgreSQL:
   ```bash
   docker exec -i tournado-db psql -U tournado_user -d tournado -c "SELECT id, name, discipline, slot_limit, status FROM tournaments;"
   ```
4. Restart docker containers:
   ```bash
   docker compose restart tournado-core tournado-db
   ```
5. Check `GET /api/v1/tournaments` to verify the tournament persisted across restarts!

---

### Task 9: Prompts Documentation & README Update

**Files:**
- Create: `prompts/5.1_code_generation.md`
- Create: `prompts/5.2_fixing_and_refactoring.md`
- Create: `prompts/5.3_ai_code_review.md`
- Modify: `README.md`

- [ ] **Step 1: Write `prompts/5.1_code_generation.md`**
  Detail the engineering prompts used for generating JPA entities, service validation, REST controllers, DTOs, and React components.

- [ ] **Step 2: Write `prompts/5.2_fixing_and_refactoring.md`**
  Detail the prompts used for strict TypeScript type checking, Flyway migration synchronization, and AssertJ test refactoring.

- [ ] **Step 3: Write `prompts/5.3_ai_code_review.md`**
  Detail the AI Code Review system instructions and audit checklist used to review the pull request before merging.

- [ ] **Step 4: Update `README.md`**
  Add section: *"Як перевірити перший робочий сценарій"* with step-by-step instructions (UI flow + DB check command).

- [ ] **Step 5: Commit documentation**

```bash
git add prompts/ README.md
git commit -m "docs: add Practical 5 prompt logs (5.1-5.3) and testing walkthrough in README"
```

---

### Task 10: Pull Request, AI Code Review & Merge

- [ ] **Step 1: Push branch to GitHub**

```bash
git push -u origin feat/tournament-creation-slice
```

- [ ] **Step 2: Create Pull Request on GitHub**
  Title: `feat: implement tournament creation and public view end-to-end slice (US-07..US-11)`

- [ ] **Step 3: Run AI Code Review on PR**
  Execute automated code review on GitHub PR inspecting architecture, contracts, security, and tests. Post review comments / approval.

- [ ] **Step 4: Merge PR into `main`**

```bash
git checkout main
git pull origin main
```
Confirm `main` has the clean merged commit.

package com.tournado.core.tournaments;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class TournamentTestResources {

    private TournamentTestResources() {
    }

    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID ORGANIZER_USER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final AuthenticatedUser ORGANIZER_AUTH_USER = new AuthenticatedUser(
            ORGANIZER_USER_ID,
            "CyberHawk",
            "student@ukma.edu.ua",
            "76561198012345678"
    );
    public static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    public static final Instant REGISTRATION_DEADLINE = NOW.plus(2, ChronoUnit.HOURS);
    public static final Instant CHECK_IN_OPENS = NOW.plus(3, ChronoUnit.HOURS);
    public static final Instant START_TIME = NOW.plus(4, ChronoUnit.HOURS);

    public static CreateTournamentRequest buildValidCreateTournamentRequest() {
        return new CreateTournamentRequest(
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                16,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                "connect 194.44.12.5:27015; password kma_cup"
        );
    }

    public static CreateTournamentRequest buildBrawlStarsTournamentRequest() {
        return new CreateTournamentRequest(
                "Brawl Stars Faculty Clash",
                "3v3 Gem Grab and Brawl Ball",
                Discipline.BRAWL_STARS,
                8,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );
    }

    public static CreateTournamentRequest buildChessTournamentRequest() {
        return new CreateTournamentRequest(
                "KMA Chess Championship",
                "1v1 blitz and rapid",
                Discipline.CHESS,
                32,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );
    }

    public static CreateTournamentRequest buildInvalidScheduleTournamentRequest() {
        return new CreateTournamentRequest(
                "KMA CS2 Autumn Cup 2026",
                "Invalid schedule order.",
                Discipline.CS2,
                16,
                START_TIME,
                CHECK_IN_OPENS,
                REGISTRATION_DEADLINE,
                null
        );
    }

    public static CreateTournamentRequest buildInvalidSlotLimitTournamentRequest() {
        return new CreateTournamentRequest(
                "Invalid Slots Cup",
                "Testing invalid slot count",
                Discipline.CS2,
                24,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );
    }

    public static TournamentEntity buildTournamentEntity(UUID id, Discipline discipline, int slotLimit) {
        TournamentEntity entity = new TournamentEntity();
        entity.setId(id);
        entity.setName("KMA CS2 Autumn Cup 2026");
        entity.setDescription("Official faculty tournament. Overtime rules: MR3 $10k.");
        entity.setDiscipline(discipline);
        entity.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        entity.setSlotLimit(slotLimit);
        entity.setStatus(TournamentStatus.REGISTRATION_OPEN);
        entity.setRegistrationDeadlineAt(REGISTRATION_DEADLINE);
        entity.setCheckInOpensAt(CHECK_IN_OPENS);
        entity.setStartTimeAt(START_TIME);
        entity.setCs2ServerConnect("connect 194.44.12.5:27015; password kma_cup");
        entity.setCreatedByUserId(ORGANIZER_USER_ID);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        entity.setVersion(0L);

        List<String> mapNames = discipline.getDefaultMapPool();
        for (int i = 0; i < mapNames.size(); i++) {
            entity.addMap(mapNames.get(i), i);
        }

        return entity;
    }

    public static TournamentResponse buildExpectedTournamentResponse() {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                "connect 194.44.12.5:27015; password kma_cup",
                List.of("de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo"),
                0,
                NOW
        );
    }

    public static TournamentResponse buildExpectedBrawlStarsResponse() {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "Brawl Stars Faculty Clash",
                "3v3 Gem Grab and Brawl Ball",
                Discipline.BRAWL_STARS,
                3,
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null,
                List.of("Shooting Star", "Backyard Bowl", "Hard Rock Mine", "Dueling Beetles", "Ring of Fire"),
                0,
                NOW
        );
    }

    public static TournamentResponse buildExpectedChessResponse() {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA Chess Championship",
                "1v1 blitz and rapid",
                Discipline.CHESS,
                1,
                TournamentFormat.SINGLE_ELIMINATION,
                32,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null,
                List.of("Blitz 3+2", "Rapid 10+0"),
                0,
                NOW
        );
    }

    public static TournamentSummaryResponse buildExpectedTournamentSummaryResponse() {
        return new TournamentSummaryResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                0,
                NOW
        );
    }

    public static ProblemDetail buildInvalidScheduleProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Schedule order violated: must be registrationDeadline < checkInOpen < startTime."
        );
        problem.setType(URI.create("https://tournado.ua/errors/INVALID_SCHEDULE_ORDER"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create("/api/v1/tournaments"));
        problem.setProperty("code", "INVALID_SCHEDULE_ORDER");
        return problem;
    }

    public static ProblemDetail buildTournamentNotFoundProblemDetail(UUID tournamentId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Tournament with id '" + tournamentId + "' was not found."
        );
        problem.setType(URI.create("https://tournado.ua/errors/TOURNAMENT_NOT_FOUND"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create("/api/v1/tournaments/" + tournamentId));
        problem.setProperty("code", "TOURNAMENT_NOT_FOUND");
        return problem;
    }
}

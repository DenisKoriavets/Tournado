package com.tournado.core.tournaments;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.LockRostersResponse;
import com.tournado.core.tournaments.api.dto.RegisterTeamRequest;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.RegisteredTeamItem;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.RosterSnapshotEntity;
import com.tournado.core.tournaments.service.RosterEntryView;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class TournamentTestResources {

    private TournamentTestResources() {
    }

    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID ORGANIZER_USER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID CAPTAIN_USER_ID = UUID.fromString("a1b2c3d4-e5f6-4789-9012-112233445566");
    public static final UUID OTHER_USER_ID = UUID.fromString("f1e2d3c4-b5a6-4789-8012-998877665544");
    public static final UUID TEAM_ID = UUID.fromString("7a30368b-59b1-4f18-bb59-8395f13d8d64");
    public static final UUID SECOND_TEAM_ID = UUID.fromString("8b40479c-6ac2-4029-cc6a-94a6f24e9e75");
    public static final UUID REGISTRATION_ID = UUID.fromString("5c9b13fa-8de1-460d-9b55-66778899aabb");
    public static final String TEAM_NAME = "Navi Junior KMA";
    public static final AuthenticatedUser ORGANIZER_AUTH_USER = new AuthenticatedUser(
            ORGANIZER_USER_ID,
            "CyberHawk",
            "student@ukma.edu.ua",
            "76561198012345678"
    );
    public static final AuthenticatedUser CAPTAIN_AUTH_USER = new AuthenticatedUser(
            CAPTAIN_USER_ID,
            "CaptainKMA",
            "captain@ukma.edu.ua",
            "76561198011111111"
    );
    public static final AuthenticatedUser OTHER_AUTH_USER = new AuthenticatedUser(
            OTHER_USER_ID,
            "RandomViewer",
            "viewer@ukma.edu.ua",
            null
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
                List.of(),
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

    public static TournamentResponse buildExpectedTournamentResponseWithCount(int registeredTeamsCount) {
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
                registeredTeamsCount,
                NOW
        );
    }

    public static TournamentSummaryResponse buildExpectedTournamentSummaryResponseWithCount(int registeredTeamsCount) {
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
                registeredTeamsCount,
                NOW
        );
    }

    public static TeamView buildFullCs2TeamView() {
        return new TeamView(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_USER_ID,
                5,
                5,
                true,
                List.of(
                        new TeamMemberView(CAPTAIN_USER_ID, "CaptainKMA", true, "76561198011111111", null, null),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111112"),
                                "s1mple_kma",
                                false,
                                "76561198011111112",
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111113"),
                                "b1t_junior",
                                false,
                                "76561198011111113",
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111114"),
                                "electro",
                                false,
                                "76561198011111114",
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111115"),
                                "perfecto",
                                false,
                                "76561198011111115",
                                null,
                                null
                        )
                )
        );
    }

    public static TeamView buildIncompleteCs2TeamView() {
        return new TeamView(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_USER_ID,
                4,
                5,
                false,
                buildFullCs2TeamView().members().subList(0, 4)
        );
    }

    public static TeamView buildFullCs2TeamViewMissingSteamId() {
        return new TeamView(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_USER_ID,
                5,
                5,
                true,
                List.of(
                        new TeamMemberView(CAPTAIN_USER_ID, "CaptainKMA", true, "76561198011111111", null, null),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111112"),
                                "s1mple_kma",
                                false,
                                null,
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111113"),
                                "b1t_junior",
                                false,
                                "76561198011111113",
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111114"),
                                "electro",
                                false,
                                "   ",
                                null,
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111115"),
                                "perfecto",
                                false,
                                "76561198011111115",
                                null,
                                null
                        )
                )
        );
    }

    public static TeamView buildFullChessTeamView() {
        return new TeamView(
                SECOND_TEAM_ID,
                "Grandmasters KMA",
                Discipline.CHESS,
                CAPTAIN_USER_ID,
                1,
                1,
                true,
                List.of(new TeamMemberView(CAPTAIN_USER_ID, "CaptainKMA", true, null, null, "cyber_master"))
        );
    }

    public static TeamView buildFullBrawlStarsTeamView() {
        return new TeamView(
                SECOND_TEAM_ID,
                "Brawl Stars KMA",
                Discipline.BRAWL_STARS,
                CAPTAIN_USER_ID,
                3,
                3,
                true,
                List.of(
                        new TeamMemberView(CAPTAIN_USER_ID, "CaptainKMA", true, null, "#2PP98L9", null),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111112"),
                                "brawler2",
                                false,
                                null,
                                "#8Q9R0P1",
                                null
                        ),
                        new TeamMemberView(
                                UUID.fromString("11111111-1111-1111-1111-111111111113"),
                                "brawler3",
                                false,
                                null,
                                "#9ABCD12",
                                null
                        )
                )
        );
    }

    public static RegistrationEntity buildRegistrationEntity(RegistrationStatus status) {
        RegistrationEntity entity = new RegistrationEntity();
        entity.setId(REGISTRATION_ID);
        entity.setTournamentId(TOURNAMENT_ID);
        entity.setTeamId(TEAM_ID);
        entity.setStatus(status);
        entity.setRegisteredAt(NOW);
        if (status == RegistrationStatus.EXCLUDED) {
            entity.setExcludedAt(NOW);
            entity.setExclusionReason("Removed by tournament organizer");
        }
        return entity;
    }

    public static List<RosterSnapshotEntity> buildExpectedCs2Snapshots() {
        return buildFullCs2TeamView().members().stream()
                .map(member -> {
                    RosterSnapshotEntity entity = new RosterSnapshotEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setRegistrationId(REGISTRATION_ID);
                    entity.setUserId(member.userId());
                    entity.setDisplayName(member.displayName());
                    entity.setGameIdentifier(member.steamId());
                    entity.setCaptain(member.isCaptain());
                    entity.setSnapshottedAt(NOW);
                    return entity;
                })
                .toList();
    }

    public static RegisterTeamRequest buildRegisterTeamRequest() {
        return new RegisterTeamRequest(TEAM_ID);
    }

    public static RegisterTeamResponse buildExpectedRegisterTeamResponse() {
        return new RegisterTeamResponse(
                REGISTRATION_ID,
                TOURNAMENT_ID,
                TEAM_ID,
                RegistrationStatus.REGISTERED,
                NOW
        );
    }

    public static RegisterTeamResponse buildExpectedRegisterSecondTeamResponse() {
        return new RegisterTeamResponse(
                REGISTRATION_ID,
                TOURNAMENT_ID,
                SECOND_TEAM_ID,
                RegistrationStatus.REGISTERED,
                NOW
        );
    }

    public static List<com.tournado.core.common.exception.InvalidParam> buildExpectedMissingSteamIdParams() {
        return List.of(
                new com.tournado.core.common.exception.InvalidParam(
                        "steamId",
                        "Player 's1mple_kma' is missing required SteamID64."
                ),
                new com.tournado.core.common.exception.InvalidParam(
                        "steamId",
                        "Player 'electro' is missing required SteamID64."
                )
        );
    }

    public static TournamentRegistrationsResponse buildExpectedTournamentRegistrationsResponse(boolean locked) {
        return new TournamentRegistrationsResponse(
                TOURNAMENT_ID,
                ORGANIZER_USER_ID,
                16,
                1,
                locked,
                List.of(new RegisteredTeamItem(
                        REGISTRATION_ID,
                        TEAM_ID,
                        TEAM_NAME,
                        Discipline.CS2,
                        RegistrationStatus.REGISTERED,
                        5,
                        NOW,
                        locked
                ))
        );
    }

    public static LockRostersResponse buildExpectedLockRostersResponse(int totalSnapshottedPlayers) {
        return new LockRostersResponse("LOCKED", totalSnapshottedPlayers);
    }

    public static ProblemDetail buildMissingGameIdentifierProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "All roster members must have a valid SteamID64 to register."
        );
        problem.setType(URI.create("https://tournado.ua/errors/MISSING_GAME_IDENTIFIER"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/register"));
        problem.setProperty("code", "MISSING_GAME_IDENTIFIER");
        problem.setProperty("invalidParams", List.of(
                Map.of("name", "steamId", "reason", "Player 's1mple_kma' is missing required SteamID64."),
                Map.of("name", "steamId", "reason", "Player 'electro' is missing required SteamID64.")
        ));
        return problem;
    }

    public static ProblemDetail buildRosterNotFullProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Team cannot register: roster has 4 players, but CS2 requires exactly 5 players."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ROSTER_NOT_FULL"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/register"));
        problem.setProperty("code", "ROSTER_NOT_FULL");
        return problem;
    }

    public static ProblemDetail buildAlreadyRegisteredProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Team '" + TEAM_ID + "' is already registered for tournament '" + TOURNAMENT_ID + "'."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ALREADY_REGISTERED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/register"));
        problem.setProperty("code", "ALREADY_REGISTERED");
        return problem;
    }

    public static ProblemDetail buildOnlyOrganizerForbiddenProblemDetail(String instanceUri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Only the tournament organizer can perform this action."
        );
        problem.setType(URI.create("https://tournado.ua/errors/AUTH_FORBIDDEN"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(instanceUri));
        problem.setProperty("code", "AUTH_FORBIDDEN");
        return problem;
    }

    public static List<RosterEntryView> buildExpectedLockedRosterViews() {
        return List.of(
                new RosterEntryView(CAPTAIN_USER_ID, "CaptainKMA", "76561198011111111", true),
                new RosterEntryView(UUID.fromString("11111111-1111-1111-1111-111111111113"), "b1t_junior", "76561198011111113", false),
                new RosterEntryView(UUID.fromString("11111111-1111-1111-1111-111111111114"), "electro", "76561198011111114", false),
                new RosterEntryView(UUID.fromString("11111111-1111-1111-1111-111111111115"), "perfecto", "76561198011111115", false),
                new RosterEntryView(UUID.fromString("11111111-1111-1111-1111-111111111112"), "s1mple_kma", "76561198011111112", false)
        );
    }
}

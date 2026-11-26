package com.tournado.core.bracket;

import com.tournado.core.bracket.api.dto.BracketMatchDto;
import com.tournado.core.bracket.api.dto.BracketResponse;
import com.tournado.core.bracket.api.dto.BracketRoundDto;
import com.tournado.core.bracket.api.dto.BracketTeamDto;
import com.tournado.core.bracket.api.dto.GenerateBracketResponse;
import com.tournado.core.bracket.api.dto.StandingItemDto;
import com.tournado.core.bracket.api.dto.StandingsResponse;
import com.tournado.core.bracket.domain.BracketEntity;
import com.tournado.core.bracket.domain.TournamentStandingEntity;
import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.service.BracketMatchView;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
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

public final class BracketTestResources {

    private BracketTestResources() {
    }

    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID BRACKET_ID = UUID.fromString("f12c3456-789a-bcde-f012-3456789abcde");
    public static final UUID ORGANIZER_USER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID OTHER_USER_ID = UUID.fromString("f1e2d3c4-b5a6-4789-8012-998877665544");

    public static final UUID TEAM_1_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    public static final UUID TEAM_2_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    public static final UUID TEAM_3_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    public static final UUID TEAM_4_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    public static final UUID TEAM_5_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    public static final UUID TEAM_6_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");

    public static final UUID MATCH_R1_1_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    public static final UUID MATCH_R1_2_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    public static final UUID MATCH_R2_1_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    public static final AuthenticatedUser ORGANIZER_AUTH_USER = new AuthenticatedUser(
            ORGANIZER_USER_ID,
            "CyberHawk",
            "student@ukma.edu.ua",
            "76561198012345678"
    );

    public static final AuthenticatedUser OTHER_AUTH_USER = new AuthenticatedUser(
            OTHER_USER_ID,
            "RandomViewer",
            "viewer@ukma.edu.ua",
            null
    );

    public static final Instant NOW = Instant.parse("2026-11-01T17:00:00Z");
    public static final Instant READY_DEADLINE = NOW.plus(10, ChronoUnit.MINUTES);

    public static List<UUID> buildSixCheckedInTeams() {
        return List.of(TEAM_1_ID, TEAM_2_ID, TEAM_3_ID, TEAM_4_ID, TEAM_5_ID, TEAM_6_ID);
    }

    public static List<UUID> buildFourCheckedInTeams() {
        return List.of(TEAM_1_ID, TEAM_2_ID, TEAM_3_ID, TEAM_4_ID);
    }

    public static TournamentResponse buildInProgressTournamentResponse() {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.IN_PROGRESS,
                NOW.minus(2, ChronoUnit.HOURS),
                NOW.minus(30, ChronoUnit.MINUTES),
                NOW,
                "connect 194.44.12.5:27015; password kma_cup",
                List.of("de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo"),
                4,
                NOW.minus(1, ChronoUnit.DAYS)
        );
    }

    public static BracketEntity buildBracketEntity(int totalRounds) {
        BracketEntity entity = new BracketEntity();
        entity.setId(BRACKET_ID);
        entity.setTournamentId(TOURNAMENT_ID);
        entity.setType(TournamentFormat.SINGLE_ELIMINATION);
        entity.setTotalRounds(totalRounds);
        entity.setGeneratedAt(NOW);
        return entity;
    }

    public static GenerateBracketResponse buildExpectedSixTeamsGenerateResponse() {
        return new GenerateBracketResponse(
                BRACKET_ID,
                3,
                6,
                2,
                4
        );
    }

    public static Map<UUID, TeamView> buildFourTeamsMap() {
        return Map.of(
                TEAM_1_ID, buildTeamView(TEAM_1_ID, "Navi Junior KMA"),
                TEAM_2_ID, buildTeamView(TEAM_2_ID, "KPI Titans"),
                TEAM_3_ID, buildTeamView(TEAM_3_ID, "Lviv Lions"),
                TEAM_4_ID, buildTeamView(TEAM_4_ID, "Mohyla Wolves")
        );
    }

    public static TeamView buildTeamView(UUID teamId, String name) {
        return new TeamView(
                teamId,
                name,
                Discipline.CS2,
                ORGANIZER_USER_ID,
                5,
                5,
                true,
                List.of(new TeamMemberView(ORGANIZER_USER_ID, "Captain", true, "76561198011111111", null, null))
        );
    }

    public static List<BracketMatchView> buildFourTeamBracketMatches() {
        return List.of(
                new BracketMatchView(
                        MATCH_R1_1_ID,
                        1,
                        1,
                        TEAM_1_ID,
                        TEAM_4_ID,
                        TEAM_1_ID,
                        MatchStatus.COMPLETED,
                        false
                ),
                new BracketMatchView(
                        MATCH_R1_2_ID,
                        1,
                        2,
                        TEAM_2_ID,
                        TEAM_3_ID,
                        TEAM_2_ID,
                        MatchStatus.COMPLETED,
                        false
                ),
                new BracketMatchView(
                        MATCH_R2_1_ID,
                        2,
                        1,
                        TEAM_1_ID,
                        TEAM_2_ID,
                        null,
                        MatchStatus.READY_CHECK,
                        false
                )
        );
    }

    public static BracketResponse buildExpectedFourTeamBracketResponse() {
        return new BracketResponse(
                TOURNAMENT_ID,
                TournamentStatus.IN_PROGRESS,
                List.of(
                        new BracketRoundDto(
                                1,
                                "Semifinals",
                                List.of(
                                        new BracketMatchDto(
                                                MATCH_R1_1_ID,
                                                1,
                                                MatchStatus.COMPLETED,
                                                new BracketTeamDto(TEAM_1_ID, "Navi Junior KMA"),
                                                new BracketTeamDto(TEAM_4_ID, "Mohyla Wolves"),
                                                TEAM_1_ID,
                                                false
                                        ),
                                        new BracketMatchDto(
                                                MATCH_R1_2_ID,
                                                2,
                                                MatchStatus.COMPLETED,
                                                new BracketTeamDto(TEAM_2_ID, "KPI Titans"),
                                                new BracketTeamDto(TEAM_3_ID, "Lviv Lions"),
                                                TEAM_2_ID,
                                                false
                                        )
                                )
                        ),
                        new BracketRoundDto(
                                2,
                                "Final",
                                List.of(
                                        new BracketMatchDto(
                                                MATCH_R2_1_ID,
                                                1,
                                                MatchStatus.READY_CHECK,
                                                new BracketTeamDto(TEAM_1_ID, "Navi Junior KMA"),
                                                new BracketTeamDto(TEAM_2_ID, "KPI Titans"),
                                                null,
                                                false
                                        )
                                )
                        )
                )
        );
    }

    public static List<TournamentStandingEntity> buildThreeStandingEntities() {
        return List.of(
                buildStandingEntity(TEAM_1_ID, 1),
                buildStandingEntity(TEAM_2_ID, 2),
                buildStandingEntity(TEAM_4_ID, 3)
        );
    }

    public static TournamentStandingEntity buildStandingEntity(UUID teamId, int rankPlace) {
        TournamentStandingEntity entity = new TournamentStandingEntity();
        entity.setId(UUID.randomUUID());
        entity.setTournamentId(TOURNAMENT_ID);
        entity.setTeamId(teamId);
        entity.setRankPlace(rankPlace);
        entity.setCreatedAt(NOW);
        return entity;
    }

    public static StandingsResponse buildExpectedStandingsResponse() {
        return new StandingsResponse(
                TOURNAMENT_ID,
                List.of(
                        new StandingItemDto(1, "Navi Junior KMA"),
                        new StandingItemDto(2, "KPI Titans"),
                        new StandingItemDto(3, "Mohyla Wolves")
                )
        );
    }

    public static ProblemDetail buildInsufficientTeamsProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "At least 4 checked-in teams are required to generate a bracket, but only 3 checked in."
        );
        problem.setType(URI.create("https://tournado.ua/errors/INSUFFICIENT_CHECKED_IN_TEAMS"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/generate-bracket"));
        problem.setProperty("code", "INSUFFICIENT_CHECKED_IN_TEAMS");
        return problem;
    }

    public static ProblemDetail buildBracketAlreadyGeneratedProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Bracket has already been generated for tournament '" + TOURNAMENT_ID + "'."
        );
        problem.setType(URI.create("https://tournado.ua/errors/BRACKET_ALREADY_GENERATED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/generate-bracket"));
        problem.setProperty("code", "BRACKET_ALREADY_GENERATED");
        return problem;
    }
}
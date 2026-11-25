package com.tournado.core.matches;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.matches.domain.MatchBanEntity;
import com.tournado.core.matches.domain.MatchCredentialsEntity;
import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.domain.ScoreSubmissionEntity;
import com.tournado.core.matches.service.MatchView;
import com.tournado.core.matches.service.ScoreSubmissionView;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class MatchTestResources {

    private MatchTestResources() {
    }

    public static final UUID MATCH_ID = UUID.fromString("c01d2345-6789-abcd-ef01-23456789abcd");
    public static final UUID CS2_TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID CHESS_TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6e");
    public static final UUID BRACKET_ID = UUID.fromString("f12c3456-789a-bcde-f012-3456789abcde");
    public static final UUID TEAM1_ID = UUID.fromString("7a30368b-59b1-4f18-bb59-8395f13d8d64");
    public static final UUID TEAM2_ID = UUID.fromString("9c40479c-59b1-4f18-bb59-8395f13d8d65");
    public static final UUID CAPTAIN1_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID CAPTAIN2_ID = UUID.fromString("a1b2c3d4-e5f6-4789-9012-112233445566");
    public static final UUID OUTSIDER_USER_ID = UUID.fromString("00000000-1111-4222-8333-444455556666");
    public static final String TEAM1_NAME = "Navi Junior KMA";
    public static final String TEAM2_NAME = "KPI Titans";
    public static final Instant NOW = Instant.parse("2026-11-22T12:00:00Z");
    public static final String CS2_SERVER_CONNECT = "connect 194.44.12.5:27015; password kma_cup";
    public static final List<String> CS2_MAP_POOL = List.of(
            "Vertigo", "Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II"
    );

    public static AuthenticatedUser buildCaptain1AuthUser() {
        return new AuthenticatedUser(CAPTAIN1_ID, "CyberHawk", "captain1@ukma.edu.ua", null);
    }

    public static AuthenticatedUser buildCaptain2AuthUser() {
        return new AuthenticatedUser(CAPTAIN2_ID, "TitanCaptain", "captain2@ukma.edu.ua", null);
    }

    public static MatchEntity buildPendingMatchEntity() {
        MatchEntity match = new MatchEntity();
        match.setId(MATCH_ID);
        match.setTournamentId(CS2_TOURNAMENT_ID);
        match.setBracketId(BRACKET_ID);
        match.setRoundNumber(1);
        match.setMatchOrder(1);
        match.setTeam1Id(TEAM1_ID);
        match.setTeam2Id(TEAM2_ID);
        match.setStatus(MatchStatus.PENDING);
        match.setBye(false);
        match.setCreatedAt(NOW);
        match.setUpdatedAt(NOW);
        match.setVersion(0L);
        return match;
    }

    public static MatchEntity buildReadyCheckMatchEntityWithTeam1Ready() {
        MatchEntity match = buildPendingMatchEntity();
        match.setStatus(MatchStatus.READY_CHECK);
        match.setReadyDeadlineAt(NOW.plusSeconds(600L));
        match.setTeam1Ready(true);
        return match;
    }

    public static MatchEntity buildPickBanMatchEntity(UUID currentBanTurnTeamId) {
        MatchEntity match = buildPendingMatchEntity();
        match.setStatus(MatchStatus.PICK_BAN);
        match.setTeam1Ready(true);
        match.setTeam2Ready(true);
        match.setCurrentBanTurnTeamId(currentBanTurnTeamId);
        match.setBanDeadlineAt(NOW.plusSeconds(30L));
        return match;
    }

    public static MatchEntity buildInProgressCs2MatchEntity() {
        MatchEntity match = buildPendingMatchEntity();
        match.setStatus(MatchStatus.IN_PROGRESS);
        match.setTeam1Ready(true);
        match.setTeam2Ready(true);
        match.setDecidedMap("Dust II");
        return match;
    }

    public static MatchBanEntity buildMatchBanEntity(UUID teamId, String mapName, int banOrder) {
        MatchBanEntity ban = new MatchBanEntity();
        ban.setId(UUID.randomUUID());
        ban.setMatchId(MATCH_ID);
        ban.setTeamId(teamId);
        ban.setMapName(mapName);
        ban.setBanOrder(banOrder);
        ban.setBannedAt(NOW);
        return ban;
    }

    public static MatchCredentialsEntity buildCs2CredentialsEntity() {
        MatchCredentialsEntity credentials = new MatchCredentialsEntity();
        credentials.setId(UUID.randomUUID());
        credentials.setMatchId(MATCH_ID);
        credentials.setCs2ConnectString(CS2_SERVER_CONNECT);
        credentials.setCreatedAt(NOW);
        return credentials;
    }

    public static TournamentResponse buildCs2TournamentResponse() {
        return new TournamentResponse(
                CS2_TOURNAMENT_ID,
                CS2_TOURNAMENT_ID.toString(),
                "KMA CS2 Winter Major 2026",
                "Студентський турнір з Counter-Strike 2.",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.IN_PROGRESS,
                NOW.minusSeconds(7200L),
                NOW.minusSeconds(3600L),
                NOW.minusSeconds(1800L),
                CS2_SERVER_CONNECT,
                CS2_MAP_POOL,
                2,
                NOW.minusSeconds(86400L)
        );
    }

    public static TournamentResponse buildChessTournamentResponse() {
        return new TournamentResponse(
                CHESS_TOURNAMENT_ID,
                CHESS_TOURNAMENT_ID.toString(),
                "Kyiv-Mohyla Rapid Open 10+0",
                "Турнір з класичних шахів на Chess.com.",
                Discipline.CHESS,
                1,
                TournamentFormat.SINGLE_ELIMINATION,
                32,
                TournamentStatus.IN_PROGRESS,
                NOW.minusSeconds(7200L),
                NOW.minusSeconds(3600L),
                NOW.minusSeconds(1800L),
                null,
                List.of(),
                2,
                NOW.minusSeconds(86400L)
        );
    }

    public static TeamView buildTeam1View() {
        return new TeamView(
                TEAM1_ID,
                TEAM1_NAME,
                Discipline.CS2,
                CAPTAIN1_ID,
                1,
                5,
                false,
                List.of(new TeamMemberView(CAPTAIN1_ID, "CyberHawk", true, "76561198012345678", null, null))
        );
    }

    public static TeamView buildTeam2View() {
        return new TeamView(
                TEAM2_ID,
                TEAM2_NAME,
                Discipline.CS2,
                CAPTAIN2_ID,
                1,
                5,
                false,
                List.of(new TeamMemberView(CAPTAIN2_ID, "TitanCaptain", true, "76561198099887766", null, null))
        );
    }

    public static Map<UUID, TeamView> buildTeamsById() {
        return Map.of(TEAM1_ID, buildTeam1View(), TEAM2_ID, buildTeam2View());
    }

    public static ProblemDetail buildMatchNotFoundProblemDetail(UUID matchId, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Match with id '" + matchId + "' was not found."
        );
        problem.setType(URI.create("https://tournado.ua/errors/MATCH_NOT_FOUND"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "MATCH_NOT_FOUND");
        return problem;
    }

    public static ProblemDetail buildOnlyCaptainAllowedProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Action can only be performed by the registered team captain."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ONLY_CAPTAIN_ALLOWED"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "ONLY_CAPTAIN_ALLOWED");
        return problem;
    }

    public static ProblemDetail buildNotYourBanTurnProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Captain attempted to ban a map when it is the opposing team's turn."
        );
        problem.setType(URI.create("https://tournado.ua/errors/NOT_YOUR_BAN_TURN"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "NOT_YOUR_BAN_TURN");
        return problem;
    }

    public static ProblemDetail buildMapAlreadyBannedProblemDetail(String mapName, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Map '" + mapName + "' has already been banned or is not in the tournament map pool."
        );
        problem.setType(URI.create("https://tournado.ua/errors/MAP_ALREADY_BANNED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "MAP_ALREADY_BANNED");
        return problem;
    }

    public static ProblemDetail buildBanPhaseCompletedProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Map veto is not currently open for this match; the decider map may already be determined."
        );
        problem.setType(URI.create("https://tournado.ua/errors/BAN_PHASE_COMPLETED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "BAN_PHASE_COMPLETED");
        return problem;
    }

    public static ProblemDetail buildMatchAccessDeniedProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Only roster members of either team in this match may view its game credentials."
        );
        problem.setType(URI.create("https://tournado.ua/errors/MATCH_ACCESS_DENIED"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "MATCH_ACCESS_DENIED");
        return problem;
    }

    public static ProblemDetail buildMatchNotInProgressProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Game credentials are not available until the pre-match phase (ready check / map veto) is complete."
        );
        problem.setType(URI.create("https://tournado.ua/errors/MATCH_NOT_IN_PROGRESS"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "MATCH_NOT_IN_PROGRESS");
        return problem;
    }

    public static final UUID NEXT_MATCH_ID = UUID.fromString("c01d2345-6789-abcd-ef01-23456789abce");

    public static ScoreSubmissionEntity buildScoreSubmissionEntity(UUID teamId, UUID userId, int team1Score, int team2Score) {
        ScoreSubmissionEntity submission = new ScoreSubmissionEntity();
        submission.setId(UUID.fromString("5c0e0000-0000-4000-8000-000000000001"));
        submission.setMatchId(MATCH_ID);
        submission.setTeamId(teamId);
        submission.setSubmittedByUserId(userId);
        submission.setTeam1Score(team1Score);
        submission.setTeam2Score(team2Score);
        submission.setSubmittedAt(NOW);
        return submission;
    }

    public static MatchEntity buildInProgressMatchEntity(int roundNumber, int matchOrder) {
        MatchEntity match = buildInProgressCs2MatchEntity();
        match.setRoundNumber(roundNumber);
        match.setMatchOrder(matchOrder);
        return match;
    }

    public static MatchEntity buildDisputedMatchEntity() {
        MatchEntity match = buildInProgressCs2MatchEntity();
        match.setStatus(MatchStatus.DISPUTED);
        return match;
    }

    public static MatchEntity buildCompletedMatchEntity(int roundNumber, int matchOrder, UUID winnerTeamId) {
        MatchEntity match = buildInProgressMatchEntity(roundNumber, matchOrder);
        match.setStatus(MatchStatus.COMPLETED);
        match.setWinnerTeamId(winnerTeamId);
        return match;
    }

    public static MatchEntity buildEmptyNextRoundMatchEntity() {
        MatchEntity match = new MatchEntity();
        match.setId(NEXT_MATCH_ID);
        match.setTournamentId(CS2_TOURNAMENT_ID);
        match.setBracketId(BRACKET_ID);
        match.setRoundNumber(2);
        match.setMatchOrder(1);
        match.setStatus(MatchStatus.PENDING);
        match.setCreatedAt(NOW);
        match.setUpdatedAt(NOW);
        match.setVersion(0L);
        return match;
    }

    public static MatchEntity buildNextRoundMatchEntityWithTeam1(UUID team1Id) {
        MatchEntity match = buildEmptyNextRoundMatchEntity();
        match.setTeam1Id(team1Id);
        return match;
    }

    public static MatchEntity buildNextRoundMatchEntityWithTeam2(UUID team2Id) {
        MatchEntity match = buildEmptyNextRoundMatchEntity();
        match.setTeam2Id(team2Id);
        return match;
    }

    public static MatchView buildExpectedCompletedMatchView(UUID winnerTeamId, List<ScoreSubmissionView> submissions) {
        return new MatchView(MATCH_ID, CS2_TOURNAMENT_ID, 1, 1, TEAM1_ID, TEAM2_ID, winnerTeamId, MatchStatus.COMPLETED, submissions);
    }

    public static ProblemDetail buildDomainProblemDetail(HttpStatus status, String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://tournado.ua/errors/" + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", code);
        return problem;
    }
}

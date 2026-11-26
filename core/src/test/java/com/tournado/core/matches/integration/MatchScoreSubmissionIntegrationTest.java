package com.tournado.core.matches.integration;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.api.dto.SubmitScoreResponse;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.support.ApiError;
import com.tournado.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tournado.core.common.domain.Discipline.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * US-27 "Внесення рахунку матчу капітаном" and US-28 "Автоматичне підтвердження та просування переможця"
 * — docs/requirements.md §5, Epic 7.
 *
 * <p>Given: a 4-team Single Elimination bracket (two semifinals in round 1 and an empty final in round 2) whose
 * second semifinal is IN_PROGRESS. Winner of semifinal #2 must land in the team2 slot of the final.
 */
@DisplayName("US-27 / US-28: captain score entry and automatic advancement")
class MatchScoreSubmissionIntegrationTest extends IntegrationTestBase {

    private static final String NO_TEAM = "<none>";
    private static final String SUBMISSIONS_COUNT_SQL = "SELECT COUNT(*) FROM score_submissions WHERE match_id = ?";

    private UUID tournamentId;
    private UUID bracketId;
    private UUID homeCaptain;
    private UUID awayCaptain;
    private UUID homeTeam;
    private UUID awayTeam;
    private UUID semifinalId;
    private UUID finalId;

    @BeforeEach
    void givenCs2SemifinalInProgress() {
        tournamentId = startedTournament(CS2);
        bracketId = db.bracket(tournamentId, 2);
        UUID otherSemifinalTeam1 = db.fullTeam("Semi A1", CS2, db.player("A1 captain"));
        UUID otherSemifinalTeam2 = db.fullTeam("Semi A2", CS2, db.player("A2 captain"));
        db.match(tournamentId, bracketId, 1, 1, otherSemifinalTeam1, otherSemifinalTeam2, "IN_PROGRESS");
        homeCaptain = db.player("Home captain");
        awayCaptain = db.player("Away captain");
        homeTeam = db.fullTeam("Terror Squad", CS2, homeCaptain);
        awayTeam = db.fullTeam("Vegayta Five", CS2, awayCaptain);
        semifinalId = db.match(tournamentId, bracketId, 1, 2, homeTeam, awayTeam, "IN_PROGRESS");
        finalId = db.match(tournamentId, bracketId, 2, 1, null, null, "PENDING");
    }

    @Test
    @DisplayName("US-27 sc.1: captain submits 13:9 -> the score is saved, status is 'waiting for the opponent'")
    void givenFirstSubmission_submitScore_shouldSaveAndWaitForOpponent() throws Exception {
        MvcResult result = submit(semifinalId, homeCaptain, 13, 9)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, SubmitScoreResponse.class))
                .isEqualTo(new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, false, false));
        assertThat(jdbcTemplate.queryForMap("SELECT team_id, team1_score, team2_score FROM score_submissions "
                + "WHERE match_id = ?", semifinalId))
                .isEqualTo(Map.of("team_id", homeTeam, "team1_score", 13, "team2_score", 9));
    }

    @Test
    @DisplayName("US-28 sc.3: only one captain has reported -> the match stays open and nobody advances")
    void givenOnlyOneCaptainReported_submitScore_shouldKeepMatchOpenAndNotAdvance() throws Exception {
        submit(semifinalId, homeCaptain, 13, 9).andExpect(status().isOk());

        assertThat(matchRow(semifinalId)).isEqualTo(Map.of("status", "IN_PROGRESS", "winner_team_id", NO_TEAM,
                "team1_id", homeTeam, "team2_id", awayTeam));
        assertThat(matchRow(finalId)).isEqualTo(Map.of("status", "PENDING", "winner_team_id", NO_TEAM,
                "team1_id", NO_TEAM, "team2_id", NO_TEAM));
    }

    @Test
    @DisplayName("US-27 checklist 5: after the first captain reports, the opponent's Match Room reveals no score")
    void givenFirstCaptainReported_getRoomAsOpponent_shouldNotExposeSubmittedScore() throws Exception {
        submit(semifinalId, homeCaptain, 13, 9).andExpect(status().isOk());

        String roomJson = mockMvc.perform(get("/api/v1/matches/{id}/room", semifinalId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(awayCaptain)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(fieldNames(objectMapper.readTree(roomJson)))
                .isNotEmpty()
                .noneMatch(name -> name.toLowerCase().contains("score") || name.toLowerCase().contains("submission"));
    }

    @ParameterizedTest(name = "US-27 sc.2: invalid score payload {0} -> 400 VALIDATION_FAILED")
    @ValueSource(strings = {
        "{\"team1Score\": -1, \"team2Score\": 9}",
        "{\"team1Score\": \"abc\", \"team2Score\": 9}",
        "{\"team1Score\": 13.5, \"team2Score\": 9}",
        "{\"team2Score\": 9}"
    })
    void givenInvalidScorePayload_submitScore_shouldReturn400AndNotPersist(String payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches/{id}/scores", semifinalId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(homeCaptain))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(400, "VALIDATION_FAILED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-27 sc.3: tied score 5:5 -> 422 SCORE_TIE_NOT_ALLOWED")
    void givenTiedScore_submitScore_shouldReturn422AndNotPersist() throws Exception {
        MvcResult result = submit(semifinalId, homeCaptain, 5, 5)
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "SCORE_TIE_NOT_ALLOWED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-27 sc.4: Chess.com match -> 422 SCORE_ENTRY_NOT_AVAILABLE")
    void givenChessMatch_submitScore_shouldReturn422ScoreEntryNotAvailable() throws Exception {
        UUID chessTournament = startedTournament(CHESS);
        UUID chessBracket = db.bracket(chessTournament, 1);
        UUID whiteCaptain = db.player("White");
        UUID chessMatch = db.match(chessTournament, chessBracket, 1, 1,
                db.team("White Solo", CHESS, whiteCaptain), db.team("Black Solo", CHESS, db.player("Black")),
                "IN_PROGRESS");

        MvcResult result = submit(chessMatch, whiteCaptain, 1, 0)
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "SCORE_ENTRY_NOT_AVAILABLE"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, chessMatch)).isZero();
    }

    @Test
    @DisplayName("US-27 checklist: each captain submits once -> a second submission is 409 SCORE_ALREADY_SUBMITTED")
    void givenCaptainAlreadySubmitted_submitScore_shouldReturn409() throws Exception {
        submit(semifinalId, homeCaptain, 13, 9).andExpect(status().isOk());

        MvcResult result = submit(semifinalId, homeCaptain, 13, 2)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "SCORE_ALREADY_SUBMITTED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-27 checklist 1: an outsider who plays in neither team -> 403 ONLY_CAPTAIN_ALLOWED")
    void givenOutsider_submitScore_shouldReturn403() throws Exception {
        UUID outsider = db.player("Spectator");

        MvcResult result = submit(semifinalId, outsider, 13, 9)
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(403, "ONLY_CAPTAIN_ALLOWED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-27 checklist 1: a roster member of a playing team who is not its captain -> 403")
    void givenRosterMemberWhoIsNotCaptain_submitScore_shouldReturn403() throws Exception {
        UUID teammate = db.player("Home teammate");
        db.addMember(homeTeam, teammate);

        MvcResult result = submit(semifinalId, teammate, 13, 9)
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(403, "ONLY_CAPTAIN_ALLOWED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-27: unknown match -> 404 MATCH_NOT_FOUND; anonymous visitor -> 401 AUTH_UNAUTHORIZED")
    void givenUnknownMatchOrAnonymous_submitScore_shouldReturn404Or401() throws Exception {
        MvcResult notFound = submit(UUID.randomUUID(), homeCaptain, 13, 9)
                .andExpect(status().isNotFound())
                .andReturn();
        MvcResult anonymous = mockMvc.perform(post("/api/v1/matches/{id}/scores", semifinalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("team1Score", 13, "team2Score", 9))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(readError(notFound)).isEqualTo(new ApiError(404, "MATCH_NOT_FOUND"));
        assertThat(readError(anonymous)).isEqualTo(new ApiError(401, "AUTH_UNAUTHORIZED"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-27 checklist: score entry before the game started (READY_CHECK) -> 409 MATCH_NOT_IN_PROGRESS")
    void givenMatchInReadyCheck_submitScore_shouldReturn409MatchNotInProgress() throws Exception {
        jdbcTemplate.update("UPDATE matches SET status = 'READY_CHECK' WHERE id = ?", semifinalId);

        MvcResult result = submit(semifinalId, homeCaptain, 13, 9)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "MATCH_NOT_IN_PROGRESS"));
        assertThat(db.count(SUBMISSIONS_COUNT_SQL, semifinalId)).isZero();
    }

    @Test
    @DisplayName("US-28 sc.1: both captains report 9:13 -> match completed, away team advances to final slot team2")
    void givenBothCaptainsAgree_submitScore_shouldCompleteMatchAndAdvanceWinner() throws Exception {
        submit(semifinalId, homeCaptain, 9, 13).andExpect(status().isOk());

        MvcResult result = submit(semifinalId, awayCaptain, 9, 13)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, SubmitScoreResponse.class))
                .isEqualTo(new SubmitScoreResponse(MatchStatus.COMPLETED, awayTeam, true, true));
        assertThat(matchRow(semifinalId)).isEqualTo(Map.of("status", "COMPLETED", "winner_team_id", awayTeam,
                "team1_id", homeTeam, "team2_id", awayTeam));
        assertThat(matchRow(finalId)).isEqualTo(Map.of("status", "PENDING", "winner_team_id", NO_TEAM,
                "team1_id", NO_TEAM, "team2_id", awayTeam));
        assertThat(tournamentStatus(tournamentId)).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("US-28 sc.2: captains report 13:9 and 9:13 -> match stays open, nobody advances, dispute possible")
    void givenCaptainsDisagree_submitScore_shouldKeepMatchOpenAndNotAdvance() throws Exception {
        submit(semifinalId, homeCaptain, 13, 9).andExpect(status().isOk());

        MvcResult result = submit(semifinalId, awayCaptain, 9, 13)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, SubmitScoreResponse.class))
                .isEqualTo(new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, true, false));
        assertThat(matchRow(semifinalId)).isEqualTo(Map.of("status", "IN_PROGRESS", "winner_team_id", NO_TEAM,
                "team1_id", homeTeam, "team2_id", awayTeam));
        assertThat(matchRow(finalId)).isEqualTo(Map.of("status", "PENDING", "winner_team_id", NO_TEAM,
                "team1_id", NO_TEAM, "team2_id", NO_TEAM));
    }

    @Test
    @DisplayName("US-28 sc.4: captains agree in the final -> champion decided, tournament becomes COMPLETED")
    void givenAgreedScoreInFinal_submitScore_shouldCompleteTournament() throws Exception {
        jdbcTemplate.update("UPDATE matches SET team1_id = ?, team2_id = ?, status = 'IN_PROGRESS' WHERE id = ?",
                homeTeam, awayTeam, finalId);
        submit(finalId, homeCaptain, 16, 14).andExpect(status().isOk());

        MvcResult result = submit(finalId, awayCaptain, 16, 14)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, SubmitScoreResponse.class))
                .isEqualTo(new SubmitScoreResponse(MatchStatus.COMPLETED, homeTeam, true, true));
        assertThat(tournamentStatus(tournamentId)).isEqualTo("COMPLETED");
    }

    private UUID startedTournament(Discipline discipline) {
        return db.tournament(db.player("Organizer " + discipline), discipline, 8, "IN_PROGRESS",
                Instant.now().minus(3, ChronoUnit.HOURS));
    }

    /** Whole match row; NULL columns become NO_TEAM so rows can be compared as a whole with Map.of(...). */
    private Map<String, Object> matchRow(UUID matchId) {
        Map<String, Object> row = new LinkedHashMap<>(jdbcTemplate.queryForMap(
                "SELECT status, winner_team_id, team1_id, team2_id FROM matches WHERE id = ?", matchId));
        row.replaceAll((column, value) -> value == null ? NO_TEAM : value);
        return row;
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fields().forEachRemaining(entry -> {
            names.add(entry.getKey());
            names.addAll(fieldNames(entry.getValue()));
        });
        node.elements().forEachRemaining(child -> {
            if (!node.isObject()) {
                names.addAll(fieldNames(child));
            }
        });
        return names;
    }

    private String tournamentStatus(UUID id) {
        return jdbcTemplate.queryForObject("SELECT status FROM tournaments WHERE id = ?", String.class, id);
    }

    private ResultActions submit(UUID matchId, UUID captain, int team1Score, int team2Score) throws Exception {
        return mockMvc.perform(post("/api/v1/matches/{id}/scores", matchId)
                .header(HttpHeaders.AUTHORIZATION, bearer(captain))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("team1Score", team1Score, "team2Score", team2Score))));
    }
}

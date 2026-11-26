package com.tournado.core.disputes.integration;

import com.tournado.core.disputes.api.dto.ResolveDisputeResponse;
import com.tournado.core.disputes.domain.DisputeResolutionSource;
import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.support.ApiError;
import com.tournado.core.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.tournado.core.common.domain.Discipline.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * US-34 "Закриття спірного матчу організатором" — docs/requirements.md §5, Epic 8.
 *
 * <p>Given: a 4-team CS2 bracket whose first semifinal is DISPUTED with an OPEN dispute (screenshot already stored)
 * and an empty final. The winner of semifinal #1 must land in the team1 slot of the final.
 */
@DisplayName("US-34: organizer closes a disputed match")
class DisputeResolutionIntegrationTest extends IntegrationTestBase {

    private UUID organizer;
    private UUID homeCaptain;
    private UUID homeTeam;
    private UUID awayTeam;
    private UUID semifinalId;
    private UUID finalId;
    private UUID disputeId;

    @BeforeEach
    void givenDisputedSemifinal() {
        organizer = db.player("Organizer");
        UUID tournamentId = db.tournament(organizer, CS2, 8, "IN_PROGRESS", Instant.now().minus(3, ChronoUnit.HOURS));
        UUID bracketId = db.bracket(tournamentId, 2);
        homeCaptain = db.player("Home captain");
        homeTeam = db.fullTeam("QA Terror Squad", CS2, homeCaptain);
        awayTeam = db.fullTeam("QA Vegayta Five", CS2, db.player("Away captain"));
        semifinalId = db.match(tournamentId, bracketId, 1, 1, homeTeam, awayTeam, "DISPUTED");
        db.match(tournamentId, bracketId, 1, 2, db.fullTeam("Semi B1", CS2, db.player("B1")),
                db.fullTeam("Semi B2", CS2, db.player("B2")), "IN_PROGRESS");
        finalId = db.match(tournamentId, bracketId, 2, 1, null, null, "PENDING");
        disputeId = db.dispute(semifinalId, homeTeam, homeCaptain);
    }

    @Test
    @DisplayName("US-34 sc.1: approving the AI hint 13:9 closes the match, the home team wins and advances")
    void givenAiRecognisedScore_resolveWithAiSuggestion_shouldCloseMatchAndAdvanceWinner() throws Exception {
        db.aiAnalysis(disputeId, "SUCCESS", 13, 9);
        ResolveDisputeResponse expected = new ResolveDisputeResponse(disputeId, DisputeStatus.RESOLVED, semifinalId,
                MatchStatus.COMPLETED, homeTeam, 13, 9, DisputeResolutionSource.AI_SUGGESTION, null);

        MvcResult result = resolve(organizer, Map.of("decision", "AI_SUGGESTION"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, ResolveDisputeResponse.class))
                .usingRecursiveComparison()
                .ignoringFields("resolvedAt")
                .isEqualTo(expected);
        assertThat(row("SELECT status, winner_team_id FROM matches WHERE id = ?", semifinalId))
                .isEqualTo(Map.of("status", "COMPLETED", "winner_team_id", homeTeam));
        assertThat(finalTeam1()).isEqualTo(homeTeam);
    }

    @Test
    @DisplayName("US-34 sc.2: manual 9:13 overrides the AI hint, the team with the higher score wins and advances")
    void givenManualScore_resolve_shouldPickHigherScoreAsWinner() throws Exception {
        db.aiAnalysis(disputeId, "SUCCESS", 13, 9);
        ResolveDisputeResponse expected = new ResolveDisputeResponse(disputeId, DisputeStatus.RESOLVED, semifinalId,
                MatchStatus.COMPLETED, awayTeam, 9, 13, DisputeResolutionSource.MANUAL_SCORE, null);

        MvcResult result = resolve(organizer, manual(9, 13))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, ResolveDisputeResponse.class))
                .usingRecursiveComparison()
                .ignoringFields("resolvedAt")
                .isEqualTo(expected);
        assertThat(finalTeam1()).isEqualTo(awayTeam);
        assertThat(row("SELECT status, resolution_team1_score, resolution_team2_score FROM disputes WHERE id = ?",
                disputeId)).isEqualTo(Map.of("status", "RESOLVED", "resolution_team1_score", 9,
                "resolution_team2_score", 13));
    }

    @Test
    @DisplayName("US-34 sc.3: the AI did not recognise a score -> 422 AI_SUGGESTION_UNAVAILABLE, match stays disputed")
    void givenUnrecognisedScore_resolveWithAiSuggestion_shouldReturn422() throws Exception {
        db.aiAnalysis(disputeId, "UNRECOGNIZED", null, null);

        MvcResult result = resolve(organizer, Map.of("decision", "AI_SUGGESTION"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "AI_SUGGESTION_UNAVAILABLE"));
        assertStillDisputed();
    }

    @Test
    @DisplayName("US-34 sc.4: a tied manual score 5:5 -> 422 SCORE_TIE_NOT_ALLOWED, match stays disputed")
    void givenTiedManualScore_resolve_shouldReturn422AndKeepDispute() throws Exception {
        MvcResult result = resolve(organizer, manual(5, 5))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "SCORE_TIE_NOT_ALLOWED"));
        assertStillDisputed();
    }

    @Test
    @DisplayName("US-34 checklist 4: a manual ruling without scores -> 400 VALIDATION_FAILED")
    void givenManualDecisionWithoutScores_resolve_shouldReturn400() throws Exception {
        MvcResult result = resolve(organizer, Map.of("decision", "MANUAL_SCORE"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(400, "VALIDATION_FAILED"));
        assertStillDisputed();
    }

    @Test
    @DisplayName("US-34 checklist 5: a captain (not the organizer) cannot close the dispute -> 403 AUTH_FORBIDDEN")
    void givenCaptainInsteadOfOrganizer_resolve_shouldReturn403() throws Exception {
        MvcResult result = resolve(homeCaptain, manual(13, 0))
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(403, "AUTH_FORBIDDEN"));
        assertStillDisputed();
    }

    @Test
    @DisplayName("US-34 checklist 6: the decision is final -> resolving again is 409 DISPUTE_ALREADY_RESOLVED")
    void givenResolvedDispute_resolveAgain_shouldReturn409AndKeepFirstRuling() throws Exception {
        resolve(organizer, manual(13, 9)).andExpect(status().isOk());

        MvcResult result = resolve(organizer, manual(0, 13))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "DISPUTE_ALREADY_RESOLVED"));
        assertThat(jdbcTemplate.queryForObject("SELECT winner_team_id FROM matches WHERE id = ?", UUID.class,
                semifinalId)).isEqualTo(homeTeam);
    }

    private void assertStillDisputed() {
        assertThat(row("SELECT m.status AS match_status, d.status AS dispute_status FROM matches m "
                + "JOIN disputes d ON d.match_id = m.id WHERE d.id = ?", disputeId))
                .isEqualTo(Map.of("match_status", "DISPUTED", "dispute_status", "OPEN"));
        assertThat(finalTeam1()).isNull();
    }

    private UUID finalTeam1() {
        return jdbcTemplate.queryForObject("SELECT team1_id FROM matches WHERE id = ?", UUID.class, finalId);
    }

    private Map<String, Object> row(String sql, Object... args) {
        return new LinkedHashMap<>(jdbcTemplate.queryForMap(sql, args));
    }

    private Map<String, Object> manual(int team1Score, int team2Score) {
        return Map.of("decision", "MANUAL_SCORE", "team1Score", team1Score, "team2Score", team2Score);
    }

    private ResultActions resolve(UUID actor, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/disputes/{id}/resolve", disputeId)
                .header(HttpHeaders.AUTHORIZATION, bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }
}

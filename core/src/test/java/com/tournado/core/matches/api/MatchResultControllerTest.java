package com.tournado.core.matches.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.matches.api.dto.SubmitScoreRequest;
import com.tournado.core.matches.api.dto.SubmitScoreResponse;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.ScoreAlreadySubmittedException;
import com.tournado.core.matches.exception.ScoreTieNotAllowedException;
import com.tournado.core.matches.service.MatchResultService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.tournado.core.common.security.SecurityTestResources.buildUnauthorizedProblemDetail;
import static com.tournado.core.matches.MatchTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatchResultController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class MatchResultControllerTest {

    private static final String SCORES_URI = "/api/v1/matches/" + MATCH_ID + "/scores";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private MatchResultService matchResultService;

    private Cookie captainCookie() {
        return new Cookie(jwtProperties.cookieName(), jwtTokenService.generateToken(buildCaptain1AuthUser()));
    }

    @Test
    void givenAnonymousUser_submitScore_shouldReturn401ProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(post(SCORES_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitScoreRequest(13, 9))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(readProblemDetail(result)).usingRecursiveComparison().isEqualTo(buildUnauthorizedProblemDetail(SCORES_URI));
        verifyNoInteractions(matchResultService);
    }

    @Test
    void givenCaptainScore_submitScore_shouldReturn200WithConfirmationState() throws Exception {
        SubmitScoreResponse expected = new SubmitScoreResponse(MatchStatus.COMPLETED, TEAM1_ID, true, true);
        when(matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 9)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post(SCORES_URI)
                        .cookie(captainCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitScoreRequest(13, 9))))
                .andExpect(status().isOk())
                .andReturn();

        SubmitScoreResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), SubmitScoreResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNegativeAndMissingScore_submitScore_shouldReturn400WithFieldErrors() throws Exception {
        ProblemDetail expected = buildDomainProblemDetail(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request body contains 2 validation constraint violations", SCORES_URI);
        expected.setProperty("invalidParams", List.of(
                Map.of("name", "team1Score", "reason", "Score must be a non-negative integer"),
                Map.of("name", "team2Score", "reason", "Score for team 2 is required")));

        MvcResult result = mockMvc.perform(post(SCORES_URI)
                        .cookie(captainCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"team1Score\": -1}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readProblemDetail(result)).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
        verifyNoInteractions(matchResultService);
    }

    @Test
    void givenTiedScore_submitScore_shouldReturn422ScoreTieNotAllowed() throws Exception {
        when(matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 5, 5)).thenThrow(new ScoreTieNotAllowedException());
        ProblemDetail expected = buildDomainProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "SCORE_TIE_NOT_ALLOWED",
                "A tied score is not allowed: a Single Elimination match must have a winner.", SCORES_URI);
        expected.setProperty("invalidParams", List.of(Map.of("name", "team2Score", "reason", "Scores must differ so that one team wins")));

        MvcResult result = mockMvc.perform(post(SCORES_URI)
                        .cookie(captainCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitScoreRequest(5, 5))))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readProblemDetail(result)).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenRepeatedSubmission_submitScore_shouldReturn409ScoreAlreadySubmitted() throws Exception {
        when(matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 9)).thenThrow(new ScoreAlreadySubmittedException());
        ProblemDetail expected = buildDomainProblemDetail(HttpStatus.CONFLICT, "SCORE_ALREADY_SUBMITTED",
                "Captain has already submitted a score for this match.", SCORES_URI);

        MvcResult result = mockMvc.perform(post(SCORES_URI)
                        .cookie(captainCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SubmitScoreRequest(13, 9))))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readProblemDetail(result)).usingRecursiveComparison().isEqualTo(expected);
    }

    private ProblemDetail readProblemDetail(MvcResult result) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        return actual;
    }
}

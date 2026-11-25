package com.tournado.core.disputes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.disputes.api.dto.DisputeCardResponse;
import com.tournado.core.disputes.api.dto.OpenDisputeResponse;
import com.tournado.core.disputes.api.dto.ResolveDisputeResponse;
import com.tournado.core.disputes.api.dto.TournamentDisputesResponse;
import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeResolutionSource;
import com.tournado.core.disputes.exception.DisputeAlreadyOpenException;
import com.tournado.core.disputes.exception.InvalidScreenshotFormatException;
import com.tournado.core.disputes.service.DisputeService;
import com.tournado.core.disputes.service.ScreenshotContent;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
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
import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DisputeController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class DisputeControllerTest {

    private static final String OPEN_URI = "/api/v1/matches/" + MATCH_ID + "/disputes";
    private static final String CARD_URI = "/api/v1/disputes/" + DISPUTE_ID;
    private static final String RESOLVE_URI = "/api/v1/disputes/" + DISPUTE_ID + "/resolve";
    private static final String LIST_URI = "/api/v1/tournaments/" + TOURNAMENT_ID + "/disputes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private DisputeService disputeService;

    private Cookie captainCookie() {
        return new Cookie(jwtProperties.cookieName(), jwtTokenService.generateToken(buildCaptain1AuthUser()));
    }

    private Cookie organizerCookie() {
        return new Cookie(jwtProperties.cookieName(), jwtTokenService.generateToken(buildOrganizerAuthUser()));
    }

    @Test
    void givenAnonymousUser_openDispute_shouldReturn401ProblemDetail() throws Exception {
        MvcResult result = mockMvc.perform(multipart(OPEN_URI).file(buildPngScreenshotFile()))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertProblemDetailMatches(result, buildUnauthorizedProblemDetail(OPEN_URI));
        verifyNoInteractions(disputeService);
    }

    @Test
    void givenCaptainWithScreenshot_openDispute_shouldReturn201WithLocationAndPendingAnalysis() throws Exception {
        OpenDisputeResponse expected = buildExpectedOpenDisputeResponse();
        when(disputeService.openDispute(eq(MATCH_ID), eq(CAPTAIN1_ID), any())).thenReturn(expected);

        MvcResult result = mockMvc.perform(multipart(OPEN_URI).file(buildPngScreenshotFile()).cookie(captainCookie()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", CARD_URI))
                .andReturn();

        OpenDisputeResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), OpenDisputeResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenUnsupportedFile_openDispute_shouldReturn400InvalidScreenshotFormat() throws Exception {
        when(disputeService.openDispute(eq(MATCH_ID), eq(CAPTAIN1_ID), any()))
                .thenThrow(new InvalidScreenshotFormatException("Uploaded evidence must be PNG, JPEG, or WEBP."));
        ProblemDetail expected = buildProblemDetail(HttpStatus.BAD_REQUEST, "INVALID_SCREENSHOT_FORMAT",
                "Uploaded evidence must be PNG, JPEG, or WEBP.", OPEN_URI);

        MvcResult result = mockMvc.perform(multipart(OPEN_URI).file(buildPdfScreenshotFile()).cookie(captainCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenDisputeAlreadyOpen_openDispute_shouldReturn409DisputeAlreadyOpen() throws Exception {
        when(disputeService.openDispute(eq(MATCH_ID), eq(CAPTAIN1_ID), any())).thenThrow(new DisputeAlreadyOpenException());
        ProblemDetail expected = buildProblemDetail(HttpStatus.CONFLICT, "DISPUTE_ALREADY_OPEN",
                "An active open dispute already exists for this match.", OPEN_URI);

        MvcResult result = mockMvc.perform(multipart(OPEN_URI).file(buildPngScreenshotFile()).cookie(captainCookie()))
                .andExpect(status().isConflict())
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAnonymousUser_listTournamentDisputes_shouldReturn401EvenThoughTournamentGetsArePublic() throws Exception {
        MvcResult result = mockMvc.perform(get(LIST_URI))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertProblemDetailMatches(result, buildUnauthorizedProblemDetail(LIST_URI));
    }

    @Test
    void givenOrganizer_listTournamentDisputes_shouldReturn200WithDashboardSummary() throws Exception {
        TournamentDisputesResponse expected = buildExpectedTournamentDisputes();
        when(disputeService.listTournamentDisputes(TOURNAMENT_ID, ORGANIZER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get(LIST_URI).cookie(organizerCookie()))
                .andExpect(status().isOk())
                .andReturn();

        TournamentDisputesResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), TournamentDisputesResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenOrganizer_getDispute_shouldReturn200WithAiHintAndRosterCheck() throws Exception {
        DisputeCardResponse expected = buildExpectedCard(AiAnalysisStatus.SUCCESS, buildExpectedSuccessAnalysisResponse(), true);
        when(disputeService.getDisputeCard(DISPUTE_ID, ORGANIZER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get(CARD_URI).cookie(organizerCookie()))
                .andExpect(status().isOk())
                .andReturn();

        DisputeCardResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), DisputeCardResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenCaptain_getDispute_shouldReturn403AuthForbidden() throws Exception {
        when(disputeService.getDisputeCard(DISPUTE_ID, CAPTAIN1_ID)).thenThrow(new OnlyOrganizerAllowedException());
        ProblemDetail expected = buildProblemDetail(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                "Only the tournament organizer can perform this action.", CARD_URI);

        MvcResult result = mockMvc.perform(get(CARD_URI).cookie(captainCookie()))
                .andExpect(status().isForbidden())
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenOrganizer_getScreenshot_shouldStreamImageWithStoredMimeType() throws Exception {
        when(disputeService.getScreenshot(DISPUTE_ID, ORGANIZER_ID)).thenReturn(new ScreenshotContent(PNG_BYTES, "image/png"));

        MvcResult result = mockMvc.perform(get(SCREENSHOT_URL).cookie(organizerCookie()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andReturn();

        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(PNG_BYTES);
    }

    @Test
    void givenOrganizerApprovingAiHint_resolveDispute_shouldReturn200WithClosedMatch() throws Exception {
        ResolveDisputeResponse expected = buildExpectedResolveResponse(DisputeResolutionSource.AI_SUGGESTION, 0, 1, TEAM2_ID);
        when(disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildAcceptAiRequest())).thenReturn(expected);

        MvcResult result = mockMvc.perform(post(RESOLVE_URI)
                        .cookie(organizerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildAcceptAiRequest())))
                .andExpect(status().isOk())
                .andReturn();

        ResolveDisputeResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), ResolveDisputeResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingDecisionAndNegativeScore_resolveDispute_shouldReturn400ValidationFailed() throws Exception {
        ProblemDetail expected = buildProblemDetail(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request body contains 2 validation constraint violations", RESOLVE_URI);
        expected.setProperty("invalidParams", List.of(
                Map.of("name", "decision", "reason", "Decision is required: AI_SUGGESTION or MANUAL_SCORE"),
                Map.of("name", "team1Score", "reason", "Score must be a non-negative integer")));

        MvcResult result = mockMvc.perform(post(RESOLVE_URI)
                        .cookie(organizerCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"team1Score\": -1, \"team2Score\": 3}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        ProblemDetail actual = readProblemDetail(result);
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected);
        verifyNoInteractions(disputeService);
    }

    private ProblemDetail readProblemDetail(MvcResult result) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        return actual;
    }

    private void assertProblemDetailMatches(MvcResult result, ProblemDetail expected) throws Exception {
        assertThat(readProblemDetail(result)).usingRecursiveComparison().isEqualTo(expected);
    }
}

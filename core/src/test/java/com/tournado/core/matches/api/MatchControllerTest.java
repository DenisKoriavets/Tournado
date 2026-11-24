package com.tournado.core.matches.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.matches.api.dto.BanMapRequest;
import com.tournado.core.matches.api.dto.BannedMapResponse;
import com.tournado.core.matches.api.dto.MatchCredentialsResponse;
import com.tournado.core.matches.api.dto.MatchRoomResponse;
import com.tournado.core.matches.api.dto.MatchTeamStateResponse;
import com.tournado.core.matches.api.dto.ReadyResponse;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.MapAlreadyBannedException;
import com.tournado.core.matches.exception.MatchAccessDeniedException;
import com.tournado.core.matches.exception.MatchNotFoundException;
import com.tournado.core.matches.exception.NotYourBanTurnException;
import com.tournado.core.matches.service.MatchFlowService;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.tournado.core.matches.MatchTestResources.*;
import static com.tournado.core.common.security.SecurityTestResources.buildUnauthorizedProblemDetail;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatchController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private MatchFlowService matchFlowService;

    private MatchRoomResponse buildPickBanRoomResponse() {
        return new MatchRoomResponse(
                MATCH_ID,
                MatchStatus.PICK_BAN,
                Discipline.CS2,
                null,
                NOW.plusSeconds(30L),
                new MatchTeamStateResponse(TEAM1_ID, TEAM1_NAME, true),
                new MatchTeamStateResponse(TEAM2_ID, TEAM2_NAME, true),
                TEAM2_ID,
                List.of(new BannedMapResponse("Vertigo", TEAM1_ID, 1)),
                List.of("Mirage", "Inferno", "Nuke", "Ancient", "Anubis", "Dust II"),
                null,
                null
        );
    }

    @Test
    void givenAnonymousUser_getRoom_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/matches/" + MATCH_ID + "/room");

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}/room", MATCH_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(matchFlowService);
    }

    @Test
    void givenAuthenticatedUser_getRoom_shouldReturn200WithRoomSnapshot() throws Exception {
        MatchRoomResponse expected = buildPickBanRoomResponse();
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.getRoom(MATCH_ID, CAPTAIN1_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}/room", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        MatchRoomResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), MatchRoomResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingMatch_getRoom_shouldReturn404NotFoundProblemDetail() throws Exception {
        ProblemDetail expected = buildMatchNotFoundProblemDetail(MATCH_ID, "/api/v1/matches/" + MATCH_ID + "/room");
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.getRoom(MATCH_ID, CAPTAIN1_ID)).thenThrow(new MatchNotFoundException(MATCH_ID));

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}/room", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAnonymousUser_markReady_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/matches/" + MATCH_ID + "/ready");

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ready", MATCH_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(matchFlowService);
    }

    @Test
    void givenAuthenticatedCaptain_markReady_shouldReturn200WithReadyTrue() throws Exception {
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.markReady(MATCH_ID, CAPTAIN1_ID)).thenReturn(new ReadyResponse(true));

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ready", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        ReadyResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), ReadyResponse.class);
        assertThat(actual).isEqualTo(new ReadyResponse(true));
    }

    @Test
    void givenNonCaptain_markReady_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildOnlyCaptainAllowedProblemDetail("/api/v1/matches/" + MATCH_ID + "/ready");
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.markReady(MATCH_ID, CAPTAIN1_ID)).thenThrow(new OnlyCaptainAllowedException());

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ready", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAuthenticatedCaptainOnTurn_banMap_shouldReturn200WithUpdatedRoom() throws Exception {
        MatchRoomResponse expected = buildPickBanRoomResponse();
        BanMapRequest request = new BanMapRequest("Mirage");
        String token = jwtTokenService.generateToken(buildCaptain2AuthUser());
        when(matchFlowService.banMap(MATCH_ID, CAPTAIN2_ID, "Mirage")).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ban", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MatchRoomResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), MatchRoomResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenWrongTurnCaptain_banMap_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildNotYourBanTurnProblemDetail("/api/v1/matches/" + MATCH_ID + "/ban");
        BanMapRequest request = new BanMapRequest("Mirage");
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.banMap(MATCH_ID, CAPTAIN1_ID, "Mirage")).thenThrow(new NotYourBanTurnException());

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ban", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAlreadyBannedMap_banMap_shouldReturn409ConflictProblemDetail() throws Exception {
        ProblemDetail expected = buildMapAlreadyBannedProblemDetail("Vertigo", "/api/v1/matches/" + MATCH_ID + "/ban");
        BanMapRequest request = new BanMapRequest("Vertigo");
        String token = jwtTokenService.generateToken(buildCaptain2AuthUser());
        when(matchFlowService.banMap(MATCH_ID, CAPTAIN2_ID, "Vertigo")).thenThrow(new MapAlreadyBannedException("Vertigo"));

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{matchId}/ban", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenParticipant_getCredentials_shouldReturn200WithCredentials() throws Exception {
        MatchCredentialsResponse expected = new MatchCredentialsResponse(Discipline.CS2, CS2_SERVER_CONNECT, null, null, "Dust II");
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.getCredentials(MATCH_ID, CAPTAIN1_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}/credentials", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        MatchCredentialsResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(), MatchCredentialsResponse.class);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNonParticipant_getCredentials_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildMatchAccessDeniedProblemDetail("/api/v1/matches/" + MATCH_ID + "/credentials");
        String token = jwtTokenService.generateToken(buildCaptain1AuthUser());
        when(matchFlowService.getCredentials(MATCH_ID, CAPTAIN1_ID)).thenThrow(new MatchAccessDeniedException());

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{matchId}/credentials", MATCH_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    private void assertProblemDetailMatches(MvcResult result, ProblemDetail expected) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}

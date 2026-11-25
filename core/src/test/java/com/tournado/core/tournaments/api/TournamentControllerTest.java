package com.tournado.core.tournaments.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.service.TournamentService;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.tournado.core.common.security.SecurityTestResources.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TournamentController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private TournamentService tournamentService;

    @Test
    void givenAnonymousUser_createTournament_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/tournaments");

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(tournamentService);
    }

    @Test
    void givenExpiredJwtCookie_createTournament_shouldReturn401SessionExpiredProblemDetail() throws Exception {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        ProblemDetail expected = buildSessionExpiredProblemDetail("/api/v1/tournaments");
        Instant pastIssuedAt = Instant.now().minusSeconds(jwtProperties.expirationSeconds() + 3600L);
        JwtTokenService expiredService = new JwtTokenService(jwtProperties, Clock.fixed(pastIssuedAt, ZoneOffset.UTC));
        String expiredToken = expiredService.generateToken(ORGANIZER_AUTH_USER);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .cookie(new Cookie(jwtProperties.cookieName(), expiredToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(tournamentService);
    }

    @Test
    void givenAuthenticatedUser_createTournament_shouldReturn201AndPersist() throws Exception {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        TournamentResponse expected = buildExpectedTournamentResponse();
        String token = jwtTokenService.generateToken(ORGANIZER_AUTH_USER);
        when(tournamentService.createTournament(request, ORGANIZER_USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TournamentResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), TournamentResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidScheduleOrder_createTournament_shouldReturn422ProblemDetails() throws Exception {
        CreateTournamentRequest request = buildInvalidScheduleTournamentRequest();
        ProblemDetail expected = buildInvalidScheduleProblemDetail();
        String token = jwtTokenService.generateToken(ORGANIZER_AUTH_USER);
        when(tournamentService.createTournament(request, ORGANIZER_USER_ID))
                .thenThrow(new InvalidScheduleOrderException());

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenExistingTournament_getTournamentById_shouldReturn200AndDetails() throws Exception {
        TournamentResponse expected = buildExpectedTournamentResponse();
        when(tournamentService.getTournamentById(TOURNAMENT_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{id}", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andReturn();

        TournamentResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), TournamentResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNonExistentId_getTournamentById_shouldReturn404ProblemDetails() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        ProblemDetail expected = buildTournamentNotFoundProblemDetail(nonExistentId);
        when(tournamentService.getTournamentById(nonExistentId))
                .thenThrow(new TournamentNotFoundException(nonExistentId));

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenExistingTournaments_getTournaments_shouldReturn200AndSummaryList() throws Exception {
        List<TournamentSummaryResponse> expected = List.of(buildExpectedTournamentSummaryResponse());
        when(tournamentService.getTournaments()).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments"))
                .andExpect(status().isOk())
                .andReturn();

        List<TournamentSummaryResponse> actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    private void assertProblemDetailMatches(MvcResult result, ProblemDetail expected) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}

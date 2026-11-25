package com.tournado.core.tournaments.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.tournaments.api.dto.LockRostersResponse;
import com.tournado.core.tournaments.api.dto.RegisterTeamRequest;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.exception.AlreadyRegisteredException;
import com.tournado.core.tournaments.exception.MissingGameIdentifierException;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.exception.RosterNotFullException;
import com.tournado.core.tournaments.service.RegistrationService;
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

import static com.tournado.core.common.security.SecurityTestResources.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TournamentRegistrationController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class TournamentRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private RegistrationService registrationService;

    @Test
    void givenAnonymousUser_registerTeam_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        RegisterTeamRequest request = buildRegisterTeamRequest();
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/tournaments/" + TOURNAMENT_ID + "/register");

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/register", TOURNAMENT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(registrationService);
    }

    @Test
    void givenAuthenticatedCaptain_registerTeam_shouldReturn201CreatedWithResponse() throws Exception {
        RegisterTeamRequest request = buildRegisterTeamRequest();
        RegisterTeamResponse expected = buildExpectedRegisterTeamResponse();
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        when(registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/register", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RegisterTeamResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                RegisterTeamResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingGameIdentifiers_registerTeam_shouldReturn422ProblemDetailWithInvalidParams() throws Exception {
        RegisterTeamRequest request = buildRegisterTeamRequest();
        ProblemDetail expected = buildMissingGameIdentifierProblemDetail();
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        when(registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new MissingGameIdentifierException(
                        "All roster members must have a valid SteamID64 to register.",
                        List.of(
                                new InvalidParam("steamId", "Player 's1mple_kma' is missing required SteamID64."),
                                new InvalidParam("steamId", "Player 'electro' is missing required SteamID64.")
                        )
                ));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/register", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenIncompleteRoster_registerTeam_shouldReturn422RosterNotFullProblemDetail() throws Exception {
        RegisterTeamRequest request = buildRegisterTeamRequest();
        ProblemDetail expected = buildRosterNotFullProblemDetail();
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        when(registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new RosterNotFullException(4, 5, Discipline.CS2));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/register", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAlreadyRegisteredTeam_registerTeam_shouldReturn409AlreadyRegisteredProblemDetail() throws Exception {
        RegisterTeamRequest request = buildRegisterTeamRequest();
        ProblemDetail expected = buildAlreadyRegisteredProblemDetail();
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        when(registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new AlreadyRegisteredException(TEAM_ID, TOURNAMENT_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/register", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenPublicRequest_getRegistrations_shouldReturn200WithTeamsList() throws Exception {
        TournamentRegistrationsResponse expected = buildExpectedTournamentRegistrationsResponse(false);
        when(registrationService.getRegistrations(TOURNAMENT_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{tournamentId}/registrations", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andReturn();

        TournamentRegistrationsResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TournamentRegistrationsResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenOrganizerUser_removeTeamRegistration_shouldReturn204NoContent() throws Exception {
        String token = jwtTokenService.generateToken(ORGANIZER_AUTH_USER);

        mockMvc.perform(delete("/api/v1/tournaments/{tournamentId}/registrations/{teamId}", TOURNAMENT_ID, TEAM_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isNoContent());

        verify(registrationService).removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, ORGANIZER_USER_ID);
    }

    @Test
    void givenNonOrganizerUser_removeTeamRegistration_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildOnlyOrganizerForbiddenProblemDetail(
                "/api/v1/tournaments/" + TOURNAMENT_ID + "/registrations/" + TEAM_ID
        );
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        doThrow(new OnlyOrganizerAllowedException())
                .when(registrationService)
                .removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID);

        MvcResult result = mockMvc.perform(
                delete("/api/v1/tournaments/{tournamentId}/registrations/{teamId}", TOURNAMENT_ID, TEAM_ID)
                        .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenNonOrganizerUser_lockRosters_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildOnlyOrganizerForbiddenProblemDetail(
                "/api/v1/tournaments/" + TOURNAMENT_ID + "/lock-rosters"
        );
        String token = jwtTokenService.generateToken(CAPTAIN_AUTH_USER);
        when(registrationService.lockRosters(TOURNAMENT_ID, CAPTAIN_USER_ID))
                .thenThrow(new OnlyOrganizerAllowedException());

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/lock-rosters", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenOrganizerUser_lockRosters_shouldReturn200WithLockedCount() throws Exception {
        LockRostersResponse expected = buildExpectedLockRostersResponse(80);
        String token = jwtTokenService.generateToken(ORGANIZER_AUTH_USER);
        when(registrationService.lockRosters(TOURNAMENT_ID, ORGANIZER_USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/lock-rosters", TOURNAMENT_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        LockRostersResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LockRostersResponse.class
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

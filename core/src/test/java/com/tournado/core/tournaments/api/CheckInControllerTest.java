package com.tournado.core.tournaments.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.tournaments.api.dto.CheckInResponse;
import com.tournado.core.tournaments.exception.CheckInAlreadyConfirmedException;
import com.tournado.core.tournaments.exception.CheckInExpiredException;
import com.tournado.core.tournaments.exception.CheckInNotOpenException;
import com.tournado.core.tournaments.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.tournado.core.tournaments.CheckInTestResources.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckInController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CheckInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void givenCaptainAndActiveWindow_checkIn_shouldReturn200WithCheckedInStatus() throws Exception {
        CheckInResponse expected = buildExpectedCheckInResponse();
        when(currentUserProvider.requireCurrentUser()).thenReturn(CAPTAIN_AUTH_USER);
        when(registrationService.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/check-in", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCheckInRequest())))
                .andExpect(status().isOk())
                .andReturn();

        CheckInResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CheckInResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenCheckInNotOpen_checkIn_shouldReturn409ProblemDetail() throws Exception {
        ProblemDetail expectedProblem = buildCheckInNotOpenProblemDetail();
        when(currentUserProvider.requireCurrentUser()).thenReturn(CAPTAIN_AUTH_USER);
        when(registrationService.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new CheckInNotOpenException(TOURNAMENT_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/check-in", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCheckInRequest())))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actualProblem = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        if (actualProblem.getProperties() != null) {
            actualProblem.getProperties().remove("timestamp");
        }
        assertThat(actualProblem).usingRecursiveComparison().isEqualTo(expectedProblem);
    }

    @Test
    void givenCheckInExpired_checkIn_shouldReturn409ProblemDetail() throws Exception {
        ProblemDetail expectedProblem = buildCheckInExpiredProblemDetail();
        when(currentUserProvider.requireCurrentUser()).thenReturn(CAPTAIN_AUTH_USER);
        when(registrationService.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new CheckInExpiredException(TOURNAMENT_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/check-in", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCheckInRequest())))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actualProblem = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        if (actualProblem.getProperties() != null) {
            actualProblem.getProperties().remove("timestamp");
        }
        assertThat(actualProblem).usingRecursiveComparison().isEqualTo(expectedProblem);
    }

    @Test
    void givenAlreadyConfirmed_checkIn_shouldReturn409ProblemDetail() throws Exception {
        ProblemDetail expectedProblem = buildCheckInAlreadyConfirmedProblemDetail();
        when(currentUserProvider.requireCurrentUser()).thenReturn(CAPTAIN_AUTH_USER);
        when(registrationService.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .thenThrow(new CheckInAlreadyConfirmedException(TEAM_ID, TOURNAMENT_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/check-in", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCheckInRequest())))
                .andExpect(status().isConflict())
                .andReturn();

        ProblemDetail actualProblem = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        if (actualProblem.getProperties() != null) {
            actualProblem.getProperties().remove("timestamp");
        }
        assertThat(actualProblem).usingRecursiveComparison().isEqualTo(expectedProblem);
    }
}
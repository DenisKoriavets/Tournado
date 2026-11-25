package com.tournado.core.bracket.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.bracket.api.dto.BracketResponse;
import com.tournado.core.bracket.api.dto.GenerateBracketResponse;
import com.tournado.core.bracket.api.dto.StandingsResponse;
import com.tournado.core.bracket.exception.BracketAlreadyGeneratedException;
import com.tournado.core.bracket.exception.InsufficientTeamsForBracketException;
import com.tournado.core.bracket.service.BracketService;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.CurrentUserProvider;
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

import static com.tournado.core.bracket.BracketTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BracketController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class BracketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BracketService bracketService;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void givenOrganizerAndValidTournament_generateBracket_shouldReturn201WithBracketSummary() throws Exception {
        GenerateBracketResponse expected = buildExpectedSixTeamsGenerateResponse();
        when(currentUserProvider.requireCurrentUser()).thenReturn(ORGANIZER_AUTH_USER);
        when(bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/generate-bracket", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        GenerateBracketResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GenerateBracketResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

  @Test
    void givenInsufficientCheckedInTeams_generateBracket_shouldReturn422ProblemDetail() throws Exception {
        ProblemDetail expectedProblem = buildInsufficientTeamsProblemDetail();
        when(currentUserProvider.requireCurrentUser()).thenReturn(ORGANIZER_AUTH_USER);
        when(bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .thenThrow(new InsufficientTeamsForBracketException(3));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/generate-bracket", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
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
    void givenAlreadyGeneratedBracket_generateBracket_shouldReturn409ProblemDetail() throws Exception {
        ProblemDetail expectedProblem = buildBracketAlreadyGeneratedProblemDetail();
        when(currentUserProvider.requireCurrentUser()).thenReturn(ORGANIZER_AUTH_USER);
        when(bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .thenThrow(new BracketAlreadyGeneratedException(TOURNAMENT_ID));

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{tournamentId}/generate-bracket", TOURNAMENT_ID)
                        .contentType(MediaType.APPLICATION_JSON))
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
    void givenExistingTournament_getBracket_shouldReturn200WithBracketTree() throws Exception {
        BracketResponse expected = buildExpectedFourTeamBracketResponse();
        when(bracketService.getBracket(TOURNAMENT_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{tournamentId}/bracket", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andReturn();

        BracketResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BracketResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenCompletedTournament_getStandings_shouldReturn200WithTopThreePlaces() throws Exception {
        StandingsResponse expected = buildExpectedStandingsResponse();
        when(bracketService.getStandings(TOURNAMENT_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{tournamentId}/standings", TOURNAMENT_ID))
                .andExpect(status().isOk())
                .andReturn();

        StandingsResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                StandingsResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
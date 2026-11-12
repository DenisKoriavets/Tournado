package com.tournado.core.tournaments.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.service.TournamentService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;

@WebMvcTest(controllers = TournamentController.class)
@Import(GlobalExceptionHandler.class)
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TournamentService tournamentService;

    @Test
    void givenValidTournamentRequest_createTournament_shouldReturn201AndPersist() throws Exception {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        TournamentResponse expected = buildExpectedTournamentResponse();
        when(tournamentService.createTournament(any(CreateTournamentRequest.class))).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        TournamentResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), TournamentResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        verify(tournamentService).createTournament(any(CreateTournamentRequest.class));
    }

    @Test
    void givenInvalidScheduleOrder_createTournament_shouldReturn422ProblemDetails() throws Exception {
        CreateTournamentRequest request = buildInvalidScheduleTournamentRequest();
        when(tournamentService.createTournament(any(CreateTournamentRequest.class)))
                .thenThrow(new InvalidScheduleOrderException());

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getStatus()).isEqualTo(422);
        assertThat(actual.getType()).hasToString("https://tournado.ua/errors/INVALID_SCHEDULE_ORDER");
        assertThat(actual.getTitle()).isEqualTo("Unprocessable Entity");
        assertThat(actual.getProperties().get("code")).isEqualTo("INVALID_SCHEDULE_ORDER");
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
        when(tournamentService.getTournamentById(nonExistentId))
                .thenThrow(new TournamentNotFoundException(nonExistentId));

        MvcResult result = mockMvc.perform(get("/api/v1/tournaments/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andReturn();

        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getStatus()).isEqualTo(404);
        assertThat(actual.getType()).hasToString("https://tournado.ua/errors/TOURNAMENT_NOT_FOUND");
        assertThat(actual.getTitle()).isEqualTo("Not Found");
        assertThat(actual.getProperties().get("code")).isEqualTo("TOURNAMENT_NOT_FOUND");
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
}

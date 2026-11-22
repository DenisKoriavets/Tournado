package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.InvalidSlotLimitException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        TournamentMapPoolProvider mapPoolProvider = new TournamentMapPoolProvider();
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        tournamentService = new TournamentService(tournamentRepository, mapPoolProvider, fixedClock);
    }

    @Test
    void givenValidCS2TournamentRequest_createTournament_shouldPersistAndReturnDtoWithMapPool() {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        TournamentEntity expectedEntity = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TournamentResponse expectedResponse = buildExpectedTournamentResponse();
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request, ORGANIZER_USER_ID);

        ArgumentCaptor<TournamentEntity> captor = ArgumentCaptor.forClass(TournamentEntity.class);
        verify(tournamentRepository).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id", "mapPool.id", "mapPool.tournament")
                .isEqualTo(expectedEntity);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("id", "publicSlug")
                .isEqualTo(expectedResponse);
    }

    @Test
    void givenValidBrawlStarsRequest_createTournament_shouldPersistWithBrawlStarsMapPool() {
        CreateTournamentRequest request = buildBrawlStarsTournamentRequest();
        TournamentResponse expected = buildExpectedBrawlStarsResponse();
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request, ORGANIZER_USER_ID);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("id", "publicSlug")
                .isEqualTo(expected);
    }

    @Test
    void givenValidChessRequest_createTournament_shouldPersistWithChessMapPool() {
        CreateTournamentRequest request = buildChessTournamentRequest();
        TournamentResponse expected = buildExpectedChessResponse();
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request, ORGANIZER_USER_ID);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("id", "publicSlug")
                .isEqualTo(expected);
    }

    @Test
    void givenInvalidScheduleOrder_createTournament_shouldThrowInvalidScheduleOrderException() {
        CreateTournamentRequest request = buildInvalidScheduleTournamentRequest();

        assertThatThrownBy(() -> tournamentService.createTournament(request, ORGANIZER_USER_ID))
                .isInstanceOf(InvalidScheduleOrderException.class)
                .hasMessageContaining("Schedule order violated");
    }

    @Test
    void givenInvalidSlotLimit_createTournament_shouldThrowInvalidSlotLimitException() {
        CreateTournamentRequest request = buildInvalidSlotLimitTournamentRequest();

        assertThatThrownBy(() -> tournamentService.createTournament(request, ORGANIZER_USER_ID))
                .isInstanceOf(InvalidSlotLimitException.class)
                .hasMessageContaining("Slot limit must be one of {8, 16, 32}");
    }

    @Test
    void givenExistingTournamentId_getTournamentById_shouldReturnTournamentResponse() {
        TournamentEntity entity = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TournamentResponse expected = buildExpectedTournamentResponse();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(entity));

        TournamentResponse response = tournamentService.getTournamentById(TOURNAMENT_ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNonExistentId_getTournamentById_shouldThrowTournamentNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(tournamentRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tournamentService.getTournamentById(nonExistentId))
                .isInstanceOf(TournamentNotFoundException.class)
                .hasMessageContaining(nonExistentId.toString());
    }

    @Test
    void givenTournamentsInRepository_getTournaments_shouldReturnSummaryList() {
        TournamentEntity entity = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        List<TournamentSummaryResponse> expected = List.of(buildExpectedTournamentSummaryResponse());
        when(tournamentRepository.findAll()).thenReturn(List.of(entity));

        List<TournamentSummaryResponse> summaries = tournamentService.getTournaments();

        assertThat(summaries).usingRecursiveComparison().isEqualTo(expected);
    }
}

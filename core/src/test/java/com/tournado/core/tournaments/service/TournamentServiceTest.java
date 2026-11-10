package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.InvalidScheduleOrderException;
import com.tournado.core.tournaments.exception.InvalidSlotLimitException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    private TournamentMapPoolProvider mapPoolProvider;
    private TournamentService tournamentService;

    @BeforeEach
    void setUp() {
        mapPoolProvider = new TournamentMapPoolProvider();
        tournamentService = new TournamentService(tournamentRepository, mapPoolProvider);
    }

    @Test
    void givenValidCS2TournamentRequest_createTournament_shouldPersistAndReturnDtoWithMapPool() {
        CreateTournamentRequest request = buildValidCreateTournamentRequest();
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request);

        ArgumentCaptor<TournamentEntity> captor = ArgumentCaptor.forClass(TournamentEntity.class);
        verify(tournamentRepository).save(captor.capture());
        TournamentEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getName()).isEqualTo("KMA CS2 Autumn Cup 2026");
        assertThat(savedEntity.getDiscipline()).isEqualTo(Discipline.CS2);
        assertThat(savedEntity.getFormat()).isEqualTo(TournamentFormat.SINGLE_ELIMINATION);
        assertThat(savedEntity.getSlotLimit()).isEqualTo(16);
        assertThat(savedEntity.getStatus()).isEqualTo(TournamentStatus.REGISTRATION_OPEN);
        assertThat(savedEntity.getCreatedByUserId()).isEqualTo(SYSTEM_ORGANIZER_ID);
        assertThat(savedEntity.getMapNames()).containsExactly(
                "de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo"
        );
        assertThat(response.mapPool()).containsExactly(
                "de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo"
        );
        assertThat(response.teamSize()).isEqualTo(5);
        assertThat(response.registeredTeamsCount()).isZero();
    }

    @Test
    void givenValidBrawlStarsRequest_createTournament_shouldPersistWithBrawlStarsMapPool() {
        CreateTournamentRequest request = new CreateTournamentRequest(
                "Brawl Stars Faculty Clash",
                "3v3 Gem Grab and Brawl Ball",
                Discipline.BRAWL_STARS,
                8,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request);

        assertThat(response.discipline()).isEqualTo(Discipline.BRAWL_STARS);
        assertThat(response.teamSize()).isEqualTo(3);
        assertThat(response.mapPool()).containsExactly(
                "Shooting Star", "Backyard Bowl", "Hard Rock Mine", "Dueling Beetles", "Ring of Fire"
        );
    }

    @Test
    void givenValidChessRequest_createTournament_shouldPersistWithChessMapPool() {
        CreateTournamentRequest request = new CreateTournamentRequest(
                "KMA Chess Championship",
                "1v1 blitz and rapid",
                Discipline.CHESS,
                32,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );
        when(tournamentRepository.save(any(TournamentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentResponse response = tournamentService.createTournament(request);

        assertThat(response.discipline()).isEqualTo(Discipline.CHESS);
        assertThat(response.teamSize()).isEqualTo(1);
        assertThat(response.mapPool()).containsExactly("Blitz 3+2", "Rapid 10+0");
    }

    @Test
    void givenInvalidScheduleOrder_createTournament_shouldThrowInvalidScheduleOrderException() {
        CreateTournamentRequest request = buildInvalidScheduleTournamentRequest();

        assertThatThrownBy(() -> tournamentService.createTournament(request))
                .isInstanceOf(InvalidScheduleOrderException.class)
                .hasMessageContaining("Schedule order violated");
    }

    @Test
    void givenInvalidSlotLimit_createTournament_shouldThrowInvalidSlotLimitException() {
        CreateTournamentRequest request = new CreateTournamentRequest(
                "Invalid Slots Cup",
                "Testing invalid slot count",
                Discipline.CS2,
                24,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                null
        );

        assertThatThrownBy(() -> tournamentService.createTournament(request))
                .isInstanceOf(InvalidSlotLimitException.class)
                .hasMessageContaining("Slot limit must be one of {8, 16, 32}");
    }

    @Test
    void givenExistingTournamentId_getTournamentById_shouldReturnTournamentResponse() {
        TournamentEntity entity = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(entity));

        TournamentResponse response = tournamentService.getTournamentById(TOURNAMENT_ID);

        assertThat(response.id()).isEqualTo(TOURNAMENT_ID);
        assertThat(response.name()).isEqualTo("KMA CS2 Autumn Cup 2026");
        assertThat(response.discipline()).isEqualTo(Discipline.CS2);
        assertThat(response.teamSize()).isEqualTo(5);
        assertThat(response.slotLimit()).isEqualTo(16);
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
        when(tournamentRepository.findAll()).thenReturn(List.of(entity));

        List<TournamentSummaryResponse> summaries = tournamentService.getTournaments();

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).id()).isEqualTo(TOURNAMENT_ID);
        assertThat(summaries.get(0).discipline()).isEqualTo(Discipline.CS2);
        assertThat(summaries.get(0).teamSize()).isEqualTo(5);
        assertThat(summaries.get(0).slotLimit()).isEqualTo(16);
    }
}

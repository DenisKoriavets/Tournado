package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.LockRostersResponse;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.RosterSnapshotEntity;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.AlreadyRegisteredException;
import com.tournado.core.tournaments.exception.MissingGameIdentifierException;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.exception.RegistrationCaptainForbiddenException;
import com.tournado.core.tournaments.exception.RegistrationClosedException;
import com.tournado.core.tournaments.exception.RegistrationDisciplineMismatchException;
import com.tournado.core.tournaments.exception.RegistrationNotFoundException;
import com.tournado.core.tournaments.exception.RosterNotFullException;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
class RegistrationServiceTest {

    private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.CHECKED_IN
    );

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RosterSnapshotRepository rosterSnapshotRepository;

    @Mock
    private TeamQueryService teamQueryService;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        GameIdentifierExtractor gameIdentifierExtractor = new GameIdentifierExtractor();
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        registrationService = new RegistrationService(
                tournamentRepository,
                registrationRepository,
                rosterSnapshotRepository,
                teamQueryService,
                gameIdentifierExtractor,
                fixedClock
        );
    }

    @Test
    void givenValidFullCs2TeamAndCaptain_registerTeam_shouldPersistAndReturnResponse() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView team = buildFullCs2TeamView();
        RegistrationEntity expectedEntity = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        RegisterTeamResponse expectedResponse = buildExpectedRegisterTeamResponse();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(team));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(0L);
        when(registrationRepository.save(any(RegistrationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterTeamResponse response = registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID);

        ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
        verify(registrationRepository).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedEntity);
        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("registrationId")
                .isEqualTo(expectedResponse);
    }

    @Test
    void givenNonCaptainUser_registerTeam_shouldThrowRegistrationCaptainForbiddenException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView team = buildFullCs2TeamView();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(RegistrationCaptainForbiddenException.class)
                .hasMessageContaining("captain");
    }

    @Test
    void givenDisciplineMismatch_registerTeam_shouldThrowRegistrationDisciplineMismatchException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView chessTeam = buildFullChessTeamView();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(SECOND_TEAM_ID)).thenReturn(Optional.of(chessTeam));

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, SECOND_TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(RegistrationDisciplineMismatchException.class)
                .hasMessageContaining("CHESS")
                .hasMessageContaining("CS2");
    }

    @Test
    void givenAlreadyRegisteredTeam_registerTeam_shouldThrowAlreadyRegisteredException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView team = buildFullCs2TeamView();
        RegistrationEntity existing = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(team));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(AlreadyRegisteredException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void givenFullTournamentSlots_registerTeam_shouldThrowRegistrationClosedException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView team = buildFullCs2TeamView();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(team));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(16L);

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(RegistrationClosedException.class)
                .hasMessageContaining("slot limit");
    }

    @Test
    void givenPassedRegistrationDeadline_registerTeam_shouldThrowRegistrationClosedException() {
        RegistrationService lateService = new RegistrationService(
                tournamentRepository,
                registrationRepository,
                rosterSnapshotRepository,
                teamQueryService,
                new GameIdentifierExtractor(),
                Clock.fixed(REGISTRATION_DEADLINE.plus(5, ChronoUnit.MINUTES), ZoneOffset.UTC)
        );
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> lateService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(RegistrationClosedException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void givenIncompleteRoster_registerTeam_shouldThrowRosterNotFullException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView incompleteTeam = buildIncompleteCs2TeamView();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(incompleteTeam));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(2L);

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(RosterNotFullException.class)
                .hasMessageContaining("4 players");
    }

    @Test
    void givenMissingSteamIds_registerTeam_shouldThrowMissingGameIdentifierExceptionWithInvalidParams() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        TeamView teamWithMissingIds = buildFullCs2TeamViewMissingSteamId();
        List<InvalidParam> expectedParams = buildExpectedMissingSteamIdParams();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(teamWithMissingIds));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID)).thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(2L);

        assertThatThrownBy(() -> registrationService.registerTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(MissingGameIdentifierException.class)
                .satisfies(ex -> assertThat(((MissingGameIdentifierException) ex).getInvalidParams())
                        .usingRecursiveComparison()
                        .isEqualTo(expectedParams));
    }

    @Test
    void givenActiveRegistrations_getRegistrations_shouldReturnEnrichedResponse() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        TeamView team = buildFullCs2TeamView();
        TournamentRegistrationsResponse expected = buildExpectedTournamentRegistrationsResponse(false);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(TOURNAMENT_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(teamQueryService.findTeamsByIds(List.of(TEAM_ID))).thenReturn(Map.of(TEAM_ID, team));
        when(rosterSnapshotRepository.findSnapshottedRegistrationIds(List.of(REGISTRATION_ID))).thenReturn(Set.of());

        TournamentRegistrationsResponse response = registrationService.getRegistrations(TOURNAMENT_ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenOrganizerAndActiveRegistration_removeTeamRegistration_shouldMarkExcluded() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        RegistrationEntity expectedExcluded = buildRegistrationEntity(RegistrationStatus.EXCLUDED);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentIdAndTeamIdAndStatusIn(TOURNAMENT_ID, TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(Optional.of(registration));

        registrationService.removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, ORGANIZER_USER_ID);

        ArgumentCaptor<RegistrationEntity> captor = ArgumentCaptor.forClass(RegistrationEntity.class);
        verify(registrationRepository).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(expectedExcluded);
    }

    @Test
    void givenNonOrganizer_removeTeamRegistration_shouldThrowOnlyOrganizerAllowedException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> registrationService.removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(OnlyOrganizerAllowedException.class);
    }

    @Test
    void givenStartedTournament_removeTeamRegistration_shouldThrowRegistrationClosedException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> registrationService.removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, ORGANIZER_USER_ID))
                .isInstanceOf(RegistrationClosedException.class)
                .hasMessageContaining("after tournament has started");
    }

    @Test
    void givenUnregisteredTeam_removeTeamRegistration_shouldThrowRegistrationNotFoundException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentIdAndTeamIdAndStatusIn(TOURNAMENT_ID, TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.removeTeamRegistration(TOURNAMENT_ID, TEAM_ID, ORGANIZER_USER_ID))
                .isInstanceOf(RegistrationNotFoundException.class);
    }

    @Test
    void givenOrganizerAndUnsnapshottedRegistrations_lockRosters_shouldCreateImmutableSnapshots() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        TeamView team = buildFullCs2TeamView();
        List<RosterSnapshotEntity> expectedSnapshots = buildExpectedCs2Snapshots();
        LockRostersResponse expectedResponse = buildExpectedLockRostersResponse(5);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(TOURNAMENT_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.findByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(List.of());
        when(teamQueryService.findTeamsByIds(List.of(TEAM_ID))).thenReturn(Map.of(TEAM_ID, team));

        LockRostersResponse response = registrationService.lockRosters(TOURNAMENT_ID, ORGANIZER_USER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RosterSnapshotEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(rosterSnapshotRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedSnapshots);
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    @Test
    void givenAlreadySnapshottedRegistrations_lockRosters_shouldBeIdempotentWithoutDuplicates() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        List<RosterSnapshotEntity> existingSnapshots = buildExpectedCs2Snapshots();
        LockRostersResponse expectedResponse = buildExpectedLockRostersResponse(5);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentIdAndStatusInOrderByRegisteredAtAsc(TOURNAMENT_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.findByRegistrationIdIn(List.of(REGISTRATION_ID)))
                .thenReturn(existingSnapshots);

        LockRostersResponse response = registrationService.lockRosters(TOURNAMENT_ID, ORGANIZER_USER_ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(expectedResponse);
        verify(rosterSnapshotRepository, never()).saveAll(any());
        verifyNoInteractions(teamQueryService);
    }

    @Test
    void givenNonOrganizer_lockRosters_shouldThrowOnlyOrganizerAllowedException() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> registrationService.lockRosters(TOURNAMENT_ID, CAPTAIN_USER_ID))
                .isInstanceOf(OnlyOrganizerAllowedException.class);
    }

    @Test
    void givenValidBrawlStarsTeam_registerTeam_shouldValidateSupercellTagsAndPersist() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.BRAWL_STARS, 8);
        TeamView team = buildFullBrawlStarsTeamView();
        RegisterTeamResponse expectedResponse = buildExpectedRegisterSecondTeamResponse();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(SECOND_TEAM_ID)).thenReturn(Optional.of(team));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, SECOND_TEAM_ID))
                .thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(0L);
        when(registrationRepository.save(any(RegistrationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterTeamResponse response = registrationService.registerTeam(TOURNAMENT_ID, SECOND_TEAM_ID, CAPTAIN_USER_ID);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("registrationId")
                .isEqualTo(expectedResponse);
    }

    @Test
    void givenValidChessTeam_registerTeam_shouldValidateChessUsernameAndPersist() {
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CHESS, 32);
        TeamView team = buildFullChessTeamView();
        RegisterTeamResponse expectedResponse = buildExpectedRegisterSecondTeamResponse();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(SECOND_TEAM_ID)).thenReturn(Optional.of(team));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, SECOND_TEAM_ID))
                .thenReturn(Optional.empty());
        when(registrationRepository.countByTournamentIdAndStatusIn(TOURNAMENT_ID, ACTIVE_STATUSES)).thenReturn(0L);
        when(registrationRepository.save(any(RegistrationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterTeamResponse response = registrationService.registerTeam(TOURNAMENT_ID, SECOND_TEAM_ID, CAPTAIN_USER_ID);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("registrationId")
                .isEqualTo(expectedResponse);
    }
}

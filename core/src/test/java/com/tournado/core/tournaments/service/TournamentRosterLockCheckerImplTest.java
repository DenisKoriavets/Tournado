package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentRosterLockCheckerImplTest {

    private static final List<RegistrationStatus> ACTIVE_STATUSES = List.of(
            RegistrationStatus.REGISTERED,
            RegistrationStatus.CHECKED_IN
    );

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RosterSnapshotRepository rosterSnapshotRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    private Clock fixedClock;
    private TournamentRosterLockCheckerImpl checker;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        checker = new TournamentRosterLockCheckerImpl(
                registrationRepository,
                rosterSnapshotRepository,
                tournamentRepository,
                fixedClock
        );
    }

    @Test
    void givenNoActiveRegistrations_isTeamRosterLocked_shouldReturnFalse() {
        when(registrationRepository.findByTeamIdAndStatusIn(TEAM_ID, ACTIVE_STATUSES)).thenReturn(List.of());

        boolean locked = checker.isTeamRosterLocked(TEAM_ID);

        assertThat(locked).isFalse();
        verifyNoInteractions(rosterSnapshotRepository, tournamentRepository);
    }

    @Test
    void givenActiveRegistrationBeforeDeadlineWithoutSnapshot_isTeamRosterLocked_shouldReturnFalse() {
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(registrationRepository.findByTeamIdAndStatusIn(TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.existsByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(false);
        when(tournamentRepository.findAllById(List.of(TOURNAMENT_ID))).thenReturn(List.of(tournament));

        boolean locked = checker.isTeamRosterLocked(TEAM_ID);

        assertThat(locked).isFalse();
    }

    @Test
    void givenActiveRegistrationWithSnapshot_isTeamRosterLocked_shouldReturnTrue() {
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        when(registrationRepository.findByTeamIdAndStatusIn(TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.existsByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(true);

        boolean locked = checker.isTeamRosterLocked(TEAM_ID);

        assertThat(locked).isTrue();
        verifyNoInteractions(tournamentRepository);
    }

    @Test
    void givenActiveRegistrationAfterRegistrationDeadline_isTeamRosterLocked_shouldReturnTrue() {
        Instant afterDeadline = REGISTRATION_DEADLINE.plus(1, ChronoUnit.SECONDS);
        TournamentRosterLockCheckerImpl afterDeadlineChecker = new TournamentRosterLockCheckerImpl(
                registrationRepository,
                rosterSnapshotRepository,
                tournamentRepository,
                Clock.fixed(afterDeadline, ZoneOffset.UTC)
        );
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(registrationRepository.findByTeamIdAndStatusIn(TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.existsByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(false);
        when(tournamentRepository.findAllById(List.of(TOURNAMENT_ID))).thenReturn(List.of(tournament));

        boolean locked = afterDeadlineChecker.isTeamRosterLocked(TEAM_ID);

        assertThat(locked).isTrue();
    }

    @Test
    void givenCheckedInRegistrationBeforeDeadlineWithoutSnapshot_isTeamRosterLocked_shouldReturnFalse() {
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.CHECKED_IN);
        TournamentEntity tournament = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        when(registrationRepository.findByTeamIdAndStatusIn(TEAM_ID, ACTIVE_STATUSES))
                .thenReturn(List.of(registration));
        when(rosterSnapshotRepository.existsByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(false);
        when(tournamentRepository.findAllById(List.of(TOURNAMENT_ID))).thenReturn(List.of(tournament));

        boolean locked = checker.isTeamRosterLocked(TEAM_ID);

        assertThat(locked).isFalse();
    }
}

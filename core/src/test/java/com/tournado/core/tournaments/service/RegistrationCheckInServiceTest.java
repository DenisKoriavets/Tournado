package com.tournado.core.tournaments.service;

import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.api.dto.CheckInResponse;
import com.tournado.core.tournaments.domain.RegistrationEntity;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.exception.CheckInAlreadyConfirmedException;
import com.tournado.core.tournaments.exception.CheckInExpiredException;
import com.tournado.core.tournaments.exception.CheckInNotOpenException;
import com.tournado.core.tournaments.exception.RegistrationCaptainForbiddenException;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.tournaments.CheckInTestResources.*;
import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationCheckInServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RosterSnapshotRepository rosterSnapshotRepository;

    @Mock
    private TeamQueryService teamQueryService;

    private RegistrationService createServiceAt(Instant currentInstant) {
        Clock fixedClock = Clock.fixed(currentInstant, ZoneOffset.UTC);
        return new RegistrationService(
                tournamentRepository,
                registrationRepository,
                rosterSnapshotRepository,
                teamQueryService,
                new GameIdentifierExtractor(),
                fixedClock
        );
    }

    @Test
    void givenActiveCheckInWindowAndRegisteredTeam_checkInTeam_shouldConfirmCheckInAndReturnResponse() {
        RegistrationService service = createServiceAt(ACTIVE_CHECKIN_TIME);
        TournamentEntity tournament = buildTournamentWithStatus(TournamentStatus.REGISTRATION_OPEN);
        RegistrationEntity registration = buildRegistrationEntity(RegistrationStatus.REGISTERED);
        CheckInResponse expected = buildExpectedCheckInResponse();
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(buildFullCs2TeamView()));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID))
                .thenReturn(Optional.of(registration));

        CheckInResponse actual = service.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        ArgumentCaptor<RegistrationEntity> regCaptor = ArgumentCaptor.forClass(RegistrationEntity.class);
        verify(registrationRepository).save(regCaptor.capture());
        assertThat(regCaptor.getValue().getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(regCaptor.getValue().getCheckedInAt()).isEqualTo(ACTIVE_CHECKIN_TIME);
    }

    @Test
    void givenTimeBeforeCheckInOpens_checkInTeam_shouldThrowCheckInNotOpenException() {
        RegistrationService service = createServiceAt(BEFORE_CHECKIN_TIME);
        TournamentEntity tournament = buildTournamentWithStatus(TournamentStatus.REGISTRATION_OPEN);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> service.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(CheckInNotOpenException.class);
    }

    @Test
    void givenTimeAfterTournamentStart_checkInTeam_shouldThrowCheckInExpiredException() {
        RegistrationService service = createServiceAt(AFTER_START_TIME);
        TournamentEntity tournament = buildTournamentWithStatus(TournamentStatus.CHECK_IN_ACTIVE);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> service.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(CheckInExpiredException.class);
    }

    @Test
    void givenAlreadyCheckedInTeam_checkInTeam_shouldThrowCheckInAlreadyConfirmedException() {
        RegistrationService service = createServiceAt(ACTIVE_CHECKIN_TIME);
        TournamentEntity tournament = buildTournamentWithStatus(TournamentStatus.CHECK_IN_ACTIVE);
        RegistrationEntity alreadyCheckedIn = buildRegistrationEntity(RegistrationStatus.CHECKED_IN);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(buildFullCs2TeamView()));
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID))
                .thenReturn(Optional.of(alreadyCheckedIn));

        assertThatThrownBy(() -> service.checkInTeam(TOURNAMENT_ID, TEAM_ID, CAPTAIN_USER_ID))
                .isInstanceOf(CheckInAlreadyConfirmedException.class);
    }

    @Test
    void givenNonCaptainUser_checkInTeam_shouldThrowRegistrationCaptainForbiddenException() {
        RegistrationService service = createServiceAt(ACTIVE_CHECKIN_TIME);
        TournamentEntity tournament = buildTournamentWithStatus(TournamentStatus.CHECK_IN_ACTIVE);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        when(teamQueryService.findTeamById(TEAM_ID)).thenReturn(Optional.of(buildFullCs2TeamView()));

        assertThatThrownBy(() -> service.checkInTeam(TOURNAMENT_ID, TEAM_ID, OTHER_USER_ID))
                .isInstanceOf(RegistrationCaptainForbiddenException.class);
    }
}
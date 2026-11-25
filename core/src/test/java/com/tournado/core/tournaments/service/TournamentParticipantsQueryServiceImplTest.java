package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.repository.RegistrationRepository;
import com.tournado.core.tournaments.repository.RosterSnapshotRepository;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentParticipantsQueryServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RosterSnapshotRepository rosterSnapshotRepository;

    private TournamentParticipantsQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TournamentParticipantsQueryServiceImpl(tournamentRepository, registrationRepository, rosterSnapshotRepository);
    }

    @Test
    void givenExistingTournament_findOrganizerId_shouldReturnCreator() {
        when(tournamentRepository.findById(TOURNAMENT_ID))
                .thenReturn(Optional.of(buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16)));

        Optional<UUID> organizerId = service.findOrganizerId(TOURNAMENT_ID);

        assertThat(organizerId).contains(ORGANIZER_USER_ID);
    }

    @Test
    void givenLockedRoster_findLockedRoster_shouldReturnSnapshotCaptainFirstThenByName() {
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID))
                .thenReturn(Optional.of(buildRegistrationEntity(RegistrationStatus.CHECKED_IN)));
        when(rosterSnapshotRepository.findByRegistrationIdIn(List.of(REGISTRATION_ID))).thenReturn(buildExpectedCs2Snapshots());

        List<RosterEntryView> roster = service.findLockedRoster(TOURNAMENT_ID, TEAM_ID);

        assertThat(roster).usingRecursiveComparison().isEqualTo(buildExpectedLockedRosterViews());
    }

    @Test
    void givenTeamNotRegistered_findLockedRoster_shouldReturnEmptyList() {
        when(registrationRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, TEAM_ID)).thenReturn(Optional.empty());

        List<RosterEntryView> roster = service.findLockedRoster(TOURNAMENT_ID, TEAM_ID);

        assertThat(roster).isEmpty();
        verifyNoInteractions(rosterSnapshotRepository);
    }
}

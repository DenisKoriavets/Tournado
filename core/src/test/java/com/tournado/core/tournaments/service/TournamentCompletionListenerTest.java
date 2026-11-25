package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.service.MatchCompletedEvent;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.repository.TournamentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.tournaments.TournamentTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentCompletionListenerTest {

    private static final UUID MATCH_ID = UUID.fromString("c01d2345-6789-abcd-ef01-23456789abcd");

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentCompletionListener listener;

    @Test
    void givenFinalMatchCompleted_onMatchCompleted_shouldMarkTournamentCompleted() {
        when(tournamentRepository.findById(TOURNAMENT_ID))
                .thenReturn(Optional.of(buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16)));
        TournamentEntity expected = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        expected.setStatus(TournamentStatus.COMPLETED);

        listener.onMatchCompleted(new MatchCompletedEvent(MATCH_ID, TOURNAMENT_ID, TEAM_ID, true));

        ArgumentCaptor<TournamentEntity> captor = ArgumentCaptor.forClass(TournamentEntity.class);
        verify(tournamentRepository).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().ignoringFields("mapPool.id").isEqualTo(expected);
    }

    @Test
    void givenNonFinalMatchCompleted_onMatchCompleted_shouldLeaveTournamentUntouched() {
        listener.onMatchCompleted(new MatchCompletedEvent(MATCH_ID, TOURNAMENT_ID, TEAM_ID, false));

        verifyNoInteractions(tournamentRepository);
    }
}

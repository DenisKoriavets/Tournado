package com.tournado.core.matches.service;

import com.tournado.core.matches.api.dto.SubmitScoreResponse;
import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.domain.ScoreSubmissionEntity;
import com.tournado.core.matches.exception.MatchNotInProgressException;
import com.tournado.core.matches.exception.ScoreAlreadySubmittedException;
import com.tournado.core.matches.exception.ScoreEntryNotAvailableException;
import com.tournado.core.matches.exception.ScoreTieNotAllowedException;
import com.tournado.core.matches.repository.MatchRepository;
import com.tournado.core.matches.repository.ScoreSubmissionRepository;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static com.tournado.core.matches.MatchTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private ScoreSubmissionRepository scoreSubmissionRepository;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TournamentQueryService tournamentQueryService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MatchResultService matchResultService;

    @BeforeEach
    void setUp() {
        matchResultService = new MatchResultService(
                matchRepository,
                scoreSubmissionRepository,
                teamQueryService,
                tournamentQueryService,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void stubScoreEntryContext(MatchEntity match) {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2TournamentResponse()));
    }

    @Test
    void givenFirstCaptainSubmission_submitScore_shouldStoreScoreAndAwaitOpponentConfirmation() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 1));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID)).thenReturn(List.of());

        SubmitScoreResponse response = matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 9);

        ArgumentCaptor<ScoreSubmissionEntity> captor = ArgumentCaptor.forClass(ScoreSubmissionEntity.class);
        verify(scoreSubmissionRepository).save(captor.capture());
        verify(matchRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        assertThat(captor.getValue()).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 13, 9));
        assertThat(response).isEqualTo(new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, false, false));
    }

    @Test
    void givenMatchingOpponentScoreInOddSlot_submitScore_shouldCompleteMatchAndSeatWinnerAsTeam1OfNextRound() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 1));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID))
                .thenReturn(List.of(buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 13, 9)));
        when(matchRepository.findBracketSlotForUpdate(CS2_TOURNAMENT_ID, 2, 1)).thenReturn(Optional.of(buildEmptyNextRoundMatchEntity()));

        SubmitScoreResponse response = matchResultService.submitScore(MATCH_ID, CAPTAIN2_ID, 13, 9);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository, times(2)).save(captor.capture());
        verify(eventPublisher).publishEvent(new MatchCompletedEvent(MATCH_ID, CS2_TOURNAMENT_ID, TEAM1_ID, false));
        assertThat(captor.getAllValues()).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(buildCompletedMatchEntity(1, 1, TEAM1_ID), buildNextRoundMatchEntityWithTeam1(TEAM1_ID));
        assertThat(response).isEqualTo(new SubmitScoreResponse(MatchStatus.COMPLETED, TEAM1_ID, true, true));
    }

    @Test
    void givenMatchingScoresInEvenSlot_submitScore_shouldSeatWinnerAsTeam2OfNextRound() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 2));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID))
                .thenReturn(List.of(buildScoreSubmissionEntity(TEAM2_ID, CAPTAIN2_ID, 9, 13)));
        when(matchRepository.findBracketSlotForUpdate(CS2_TOURNAMENT_ID, 2, 1)).thenReturn(Optional.of(buildEmptyNextRoundMatchEntity()));

        SubmitScoreResponse response = matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 9, 13);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues()).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(buildCompletedMatchEntity(1, 2, TEAM2_ID), buildNextRoundMatchEntityWithTeam2(TEAM2_ID));
        assertThat(response).isEqualTo(new SubmitScoreResponse(MatchStatus.COMPLETED, TEAM2_ID, true, true));
    }

    @Test
    void givenMatchingScoresInFinal_submitScore_shouldPublishFinalMatchCompletedEvent() {
        stubScoreEntryContext(buildInProgressMatchEntity(3, 1));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID))
                .thenReturn(List.of(buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 2, 0)));
        when(matchRepository.findBracketSlotForUpdate(CS2_TOURNAMENT_ID, 4, 1)).thenReturn(Optional.empty());

        matchResultService.submitScore(MATCH_ID, CAPTAIN2_ID, 2, 0);

        verify(eventPublisher).publishEvent(new MatchCompletedEvent(MATCH_ID, CS2_TOURNAMENT_ID, TEAM1_ID, true));
    }

    @Test
    void givenMismatchingOpponentScore_submitScore_shouldKeepMatchOpenAndAdvanceNobody() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 1));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID))
                .thenReturn(List.of(buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 13, 9)));

        SubmitScoreResponse response = matchResultService.submitScore(MATCH_ID, CAPTAIN2_ID, 9, 13);

        verify(matchRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
        assertThat(response).isEqualTo(new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, true, false));
    }

    @Test
    void givenTiedScore_submitScore_shouldThrowScoreTieNotAllowedException() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 1));

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 5, 5))
                .isInstanceOf(ScoreTieNotAllowedException.class);
        verifyNoInteractions(scoreSubmissionRepository);
    }

    @Test
    void givenChessMatch_submitScore_shouldThrowScoreEntryNotAvailableException() {
        MatchEntity match = buildInProgressMatchEntity(1, 1);
        match.setTournamentId(CHESS_TOURNAMENT_ID);
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CHESS_TOURNAMENT_ID)).thenReturn(Optional.of(buildChessTournamentResponse()));

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 1, 0))
                .isInstanceOf(ScoreEntryNotAvailableException.class);
    }

    @Test
    void givenMatchStillInVeto_submitScore_shouldThrowMatchNotInProgressException() {
        stubScoreEntryContext(buildPickBanMatchEntity(TEAM1_ID));

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 9))
                .isInstanceOf(MatchNotInProgressException.class);
    }

    @Test
    void givenDisputedMatch_submitScore_shouldThrowMatchNotInProgressException() {
        stubScoreEntryContext(buildDisputedMatchEntity());

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 9))
                .isInstanceOf(MatchNotInProgressException.class);
    }

    @Test
    void givenCaptainAlreadySubmitted_submitScore_shouldThrowScoreAlreadySubmittedException() {
        stubScoreEntryContext(buildInProgressMatchEntity(1, 1));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID))
                .thenReturn(List.of(buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 13, 9)));

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, CAPTAIN1_ID, 13, 10))
                .isInstanceOf(ScoreAlreadySubmittedException.class);
        verify(scoreSubmissionRepository, never()).save(any());
    }

    @Test
    void givenNonCaptain_submitScore_shouldThrowOnlyCaptainAllowedException() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildInProgressMatchEntity(1, 1)));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> matchResultService.submitScore(MATCH_ID, OUTSIDER_USER_ID, 13, 9))
                .isInstanceOf(OnlyCaptainAllowedException.class);
        verifyNoInteractions(scoreSubmissionRepository);
    }

    @Test
    void givenInProgressMatch_markDisputed_shouldFreezeMatchInDisputedStatus() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildInProgressMatchEntity(1, 1)));

        matchResultService.markDisputed(MATCH_ID);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(buildDisputedMatchEntity());
    }

    @Test
    void givenAlreadyDisputedMatch_markDisputed_shouldThrowMatchNotInProgressException() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatchEntity()));

        assertThatThrownBy(() -> matchResultService.markDisputed(MATCH_ID)).isInstanceOf(MatchNotInProgressException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void givenDisputedMatch_completeMatch_shouldCloseMatchAdvanceWinnerAndReturnView() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatchEntity()));
        when(matchRepository.findBracketSlotForUpdate(CS2_TOURNAMENT_ID, 2, 1)).thenReturn(Optional.of(buildEmptyNextRoundMatchEntity()));
        when(scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(MATCH_ID)).thenReturn(List.of(
                buildScoreSubmissionEntity(TEAM1_ID, CAPTAIN1_ID, 13, 9)));

        MatchView view = matchResultService.completeMatch(MATCH_ID, TEAM2_ID);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository, times(2)).save(captor.capture());
        verify(eventPublisher).publishEvent(new MatchCompletedEvent(MATCH_ID, CS2_TOURNAMENT_ID, TEAM2_ID, false));
        assertThat(captor.getAllValues()).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(buildCompletedMatchEntity(1, 1, TEAM2_ID), buildNextRoundMatchEntityWithTeam1(TEAM2_ID));
        assertThat(view).usingRecursiveComparison().isEqualTo(buildExpectedCompletedMatchView(TEAM2_ID,
                List.of(new ScoreSubmissionView(TEAM1_ID, 13, 9, NOW))));
    }

    @Test
    void givenCompletedMatch_completeMatch_shouldThrowMatchNotInProgressException() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildCompletedMatchEntity(1, 1, TEAM1_ID)));

        assertThatThrownBy(() -> matchResultService.completeMatch(MATCH_ID, TEAM2_ID))
                .isInstanceOf(MatchNotInProgressException.class);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void givenWinnerNotPlayingInMatch_completeMatch_shouldRejectRuling() {
        when(matchRepository.findByIdForUpdate(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatchEntity()));

        assertThatThrownBy(() -> matchResultService.completeMatch(MATCH_ID, NEXT_MATCH_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(matchRepository, never()).save(any());
    }
}

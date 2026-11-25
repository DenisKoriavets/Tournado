package com.tournado.core.disputes.service;

import com.tournado.core.disputes.api.dto.DisputeCardResponse;
import com.tournado.core.disputes.api.dto.OpenDisputeResponse;
import com.tournado.core.disputes.api.dto.ResolveDisputeResponse;
import com.tournado.core.disputes.api.dto.TournamentDisputesResponse;
import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeEntity;
import com.tournado.core.disputes.domain.DisputeResolutionSource;
import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.disputes.exception.AiSuggestionUnavailableException;
import com.tournado.core.disputes.exception.DisputeAccessDeniedException;
import com.tournado.core.disputes.exception.DisputeAlreadyOpenException;
import com.tournado.core.disputes.exception.DisputeAlreadyResolvedException;
import com.tournado.core.disputes.exception.DisputeNotAllowedException;
import com.tournado.core.disputes.exception.InvalidScreenshotFormatException;
import com.tournado.core.disputes.exception.ResolutionScoreRequiredException;
import com.tournado.core.disputes.infra.ScreenshotStorage;
import com.tournado.core.disputes.repository.AiAnalysisResultRepository;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.ScoreTieNotAllowedException;
import com.tournado.core.matches.service.MatchOutcomeService;
import com.tournado.core.matches.service.MatchQueryService;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.service.TournamentParticipantsQueryService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    @Mock
    private MatchQueryService matchQueryService;

    @Mock
    private MatchOutcomeService matchOutcomeService;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TournamentQueryService tournamentQueryService;

    @Mock
    private TournamentParticipantsQueryService tournamentParticipantsQueryService;

    @Mock
    private ScreenshotStorage screenshotStorage;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DisputeService disputeService;

    @BeforeEach
    void setUp() {
        disputeService = new DisputeService(
                disputeRepository,
                aiAnalysisResultRepository,
                matchQueryService,
                matchOutcomeService,
                teamQueryService,
                tournamentQueryService,
                tournamentParticipantsQueryService,
                new ScreenshotValidator(),
                screenshotStorage,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void stubOpenableMatch() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildInProgressMatchWithMismatch()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2Tournament()));
    }

    private void stubOrganizer() {
        when(tournamentParticipantsQueryService.findOrganizerId(TOURNAMENT_ID)).thenReturn(Optional.of(ORGANIZER_ID));
    }

    @Test
    void givenMismatchedScoresAndPngEvidence_openDispute_shouldPersistDisputeFreezeMatchStoreFileAndPublishEvent() {
        stubOpenableMatch();
        when(disputeRepository.existsByMatchIdAndStatus(MATCH_ID, DisputeStatus.OPEN)).thenReturn(false);

        OpenDisputeResponse response = disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, buildPngScreenshotFile());

        ArgumentCaptor<DisputeEntity> disputeCaptor = ArgumentCaptor.forClass(DisputeEntity.class);
        ArgumentCaptor<DisputeLifecycleEvent> eventCaptor = ArgumentCaptor.forClass(DisputeLifecycleEvent.class);
        verify(disputeRepository).saveAndFlush(disputeCaptor.capture());
        verify(matchOutcomeService).markDisputed(MATCH_ID);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UUID disputeId = disputeCaptor.getValue().getId();
        verify(screenshotStorage).store("disputes/" + disputeId + ".png", PNG_BYTES);
        assertThat(disputeCaptor.getValue()).usingRecursiveComparison().ignoringFields("id", "screenshotFilePath")
                .isEqualTo(buildExpectedNewDisputeEntity());
        assertThat(response).usingRecursiveComparison().ignoringFields("disputeId", "screenshotUrl")
                .isEqualTo(buildExpectedOpenDisputeResponse());
        assertThat(eventCaptor.getValue()).isEqualTo(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_OPENED, disputeId, TOURNAMENT_ID, MATCH_ID));
    }

    @Test
    void givenNonCaptain_openDispute_shouldThrowOnlyCaptainAllowedException() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildInProgressMatchWithMismatch()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, OUTSIDER_ID, buildPngScreenshotFile()))
                .isInstanceOf(OnlyCaptainAllowedException.class);
        verifyNoInteractions(disputeRepository, matchOutcomeService, screenshotStorage);
    }

    @Test
    void givenChessTournament_openDispute_shouldThrowDisputeNotAllowedException() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildInProgressMatchWithMismatch()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildChessTournament()));

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, buildPngScreenshotFile()))
                .isInstanceOf(DisputeNotAllowedException.class);
    }

    @Test
    void givenAlreadyDisputedMatch_openDispute_shouldThrowDisputeAlreadyOpenException() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2Tournament()));

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN2_ID, buildPngScreenshotFile()))
                .isInstanceOf(DisputeAlreadyOpenException.class);
        verify(disputeRepository, never()).saveAndFlush(any());
    }

    @Test
    void givenNoScoreReported_openDispute_shouldThrowDisputeNotAllowedException() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildMatchView(MatchStatus.IN_PROGRESS, List.of())));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2Tournament()));

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, buildPngScreenshotFile()))
                .isInstanceOf(DisputeNotAllowedException.class);
    }

    @Test
    void givenCompletedMatch_openDispute_shouldThrowDisputeNotAllowedException() {
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildCompletedMatch(TEAM1_ID)));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2Tournament()));

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, buildPngScreenshotFile()))
                .isInstanceOf(DisputeNotAllowedException.class);
    }

    @Test
    void givenMissingScreenshot_openDispute_shouldThrowInvalidScreenshotFormatException() {
        stubOpenableMatch();

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, null))
                .isInstanceOf(InvalidScreenshotFormatException.class);
        verify(disputeRepository, never()).saveAndFlush(any());
    }

    @Test
    void givenConcurrentDisputeRace_openDispute_shouldTranslateUniqueIndexViolationToDisputeAlreadyOpen() {
        stubOpenableMatch();
        when(disputeRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_match_active_dispute"));

        assertThatThrownBy(() -> disputeService.openDispute(MATCH_ID, CAPTAIN1_ID, buildPngScreenshotFile()))
                .isInstanceOf(DisputeAlreadyOpenException.class);
        verifyNoInteractions(matchOutcomeService, screenshotStorage);
    }

    @Test
    void givenOrganizer_listTournamentDisputes_shouldReturnSummariesWithOpenCounter() {
        stubOrganizer();
        when(matchQueryService.findMatchesByTournamentId(TOURNAMENT_ID)).thenReturn(List.of(buildDisputedMatch()));
        when(disputeRepository.findByMatchIdInOrderByCreatedAtDesc(Set.of(MATCH_ID))).thenReturn(List.of(buildOpenDisputeEntity()));
        when(aiAnalysisResultRepository.findByDisputeIdIn(List.of(DISPUTE_ID))).thenReturn(List.of(buildSuccessAnalysisEntity()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        TournamentDisputesResponse response = disputeService.listTournamentDisputes(TOURNAMENT_ID, ORGANIZER_ID);

        assertThat(response).usingRecursiveComparison().isEqualTo(buildExpectedTournamentDisputes());
    }

    @Test
    void givenNonOrganizer_listTournamentDisputes_shouldThrowOnlyOrganizerAllowedException() {
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.listTournamentDisputes(TOURNAMENT_ID, CAPTAIN1_ID))
                .isInstanceOf(OnlyOrganizerAllowedException.class);
        verifyNoInteractions(disputeRepository);
    }

    @Test
    void givenSuccessfulAiAnalysis_getDisputeCard_shouldExposeAiHintRosterCheckAndEnableApproval() {
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(aiAnalysisResultRepository.findByDisputeId(DISPUTE_ID)).thenReturn(Optional.of(buildSuccessAnalysisEntity()));

        DisputeCardResponse card = disputeService.getDisputeCard(DISPUTE_ID, ORGANIZER_ID);

        assertThat(card).usingRecursiveComparison()
                .isEqualTo(buildExpectedCard(AiAnalysisStatus.SUCCESS, buildExpectedSuccessAnalysisResponse(), true));
    }

    @Test
    void givenAnalysisStillRunning_getDisputeCard_shouldReportPendingWithoutApproval() {
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(aiAnalysisResultRepository.findByDisputeId(DISPUTE_ID)).thenReturn(Optional.empty());

        DisputeCardResponse card = disputeService.getDisputeCard(DISPUTE_ID, ORGANIZER_ID);

        assertThat(card).usingRecursiveComparison().isEqualTo(buildExpectedCard(AiAnalysisStatus.PENDING, null, false));
    }

    @Test
    void givenResolvedDispute_getDisputeCard_shouldExposeFinalRulingAndDisableApproval() {
        DisputeEntity resolved = buildExpectedResolvedDisputeEntity(
                DisputeResolutionSource.MANUAL_SCORE, 13, 9, TEAM1_ID, "Replay checked");
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(resolved));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildCompletedMatch(TEAM1_ID)));
        stubOrganizer();
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(aiAnalysisResultRepository.findByDisputeId(DISPUTE_ID)).thenReturn(Optional.of(buildSuccessAnalysisEntity()));

        DisputeCardResponse card = disputeService.getDisputeCard(DISPUTE_ID, ORGANIZER_ID);

        assertThat(card).usingRecursiveComparison().isEqualTo(buildExpectedResolvedCard());
    }

    @Test
    void givenCaptain_getDisputeCard_shouldThrowOnlyOrganizerAllowedException() {
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.getDisputeCard(DISPUTE_ID, CAPTAIN1_ID))
                .isInstanceOf(OnlyOrganizerAllowedException.class);
    }

    @Test
    void givenMatchParticipant_getScreenshot_shouldReturnStoredEvidence() {
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(screenshotStorage.load(SCREENSHOT_PATH)).thenReturn(PNG_BYTES);

        ScreenshotContent content = disputeService.getScreenshot(DISPUTE_ID, PLAYER2_ID);

        assertThat(content).usingRecursiveComparison().isEqualTo(new ScreenshotContent(PNG_BYTES, "image/png"));
    }

    @Test
    void givenOutsider_getScreenshot_shouldThrowDisputeAccessDeniedException() {
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> disputeService.getScreenshot(DISPUTE_ID, OUTSIDER_ID))
                .isInstanceOf(DisputeAccessDeniedException.class);
        verifyNoInteractions(screenshotStorage);
    }

    @Test
    void givenRecognizedAiScore_resolveDispute_shouldCloseMatchWithAiScoreAndAdvanceWinner() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(aiAnalysisResultRepository.findByDisputeId(DISPUTE_ID)).thenReturn(Optional.of(buildSuccessAnalysisEntity()));
        when(matchOutcomeService.completeMatch(MATCH_ID, TEAM2_ID)).thenReturn(buildCompletedMatch(TEAM2_ID));

        ResolveDisputeResponse response = disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildAcceptAiRequest());

        ArgumentCaptor<DisputeEntity> captor = ArgumentCaptor.forClass(DisputeEntity.class);
        verify(disputeRepository).save(captor.capture());
        verify(eventPublisher).publishEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_RESOLVED, DISPUTE_ID, TOURNAMENT_ID, MATCH_ID));
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(buildExpectedResolvedDisputeEntity(
                DisputeResolutionSource.AI_SUGGESTION, 0, 1, TEAM2_ID, "AI hint confirmed"));
        assertThat(response).usingRecursiveComparison()
                .isEqualTo(buildExpectedResolveResponse(DisputeResolutionSource.AI_SUGGESTION, 0, 1, TEAM2_ID));
    }

    @Test
    void givenManualScore_resolveDispute_shouldCloseMatchWithManualScore() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(matchOutcomeService.completeMatch(MATCH_ID, TEAM1_ID)).thenReturn(buildCompletedMatch(TEAM1_ID));

        ResolveDisputeResponse response = disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildManualRequest(13, 9));

        ArgumentCaptor<DisputeEntity> captor = ArgumentCaptor.forClass(DisputeEntity.class);
        verify(disputeRepository).save(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(buildExpectedResolvedDisputeEntity(
                DisputeResolutionSource.MANUAL_SCORE, 13, 9, TEAM1_ID, "Replay checked"));
        assertThat(response).usingRecursiveComparison()
                .isEqualTo(buildExpectedResolveResponse(DisputeResolutionSource.MANUAL_SCORE, 13, 9, TEAM1_ID));
    }

    @Test
    void givenUnrecognizedAiAnalysis_resolveDispute_shouldRejectOneClickApproval() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();
        when(aiAnalysisResultRepository.findByDisputeId(DISPUTE_ID)).thenReturn(Optional.of(buildUnrecognizedAnalysisEntity()));

        assertThatThrownBy(() -> disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildAcceptAiRequest()))
                .isInstanceOf(AiSuggestionUnavailableException.class);
        verifyNoInteractions(matchOutcomeService);
    }

    @Test
    void givenTiedManualScore_resolveDispute_shouldThrowScoreTieNotAllowedAndKeepDisputeOpen() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildManualRequest(5, 5)))
                .isInstanceOf(ScoreTieNotAllowedException.class);
        verify(disputeRepository, never()).save(any());
        verifyNoInteractions(matchOutcomeService);
    }

    @Test
    void givenManualDecisionWithoutScores_resolveDispute_shouldThrowResolutionScoreRequiredException() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildManualRequest(13, null)))
                .isInstanceOf(ResolutionScoreRequiredException.class);
    }

    @Test
    void givenAlreadyResolvedDispute_resolveDispute_shouldThrowDisputeAlreadyResolvedException() {
        DisputeEntity resolved = buildExpectedResolvedDisputeEntity(
                DisputeResolutionSource.MANUAL_SCORE, 13, 9, TEAM1_ID, "Replay checked");
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(resolved));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildCompletedMatch(TEAM1_ID)));
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.resolveDispute(DISPUTE_ID, ORGANIZER_ID, buildManualRequest(9, 13)))
                .isInstanceOf(DisputeAlreadyResolvedException.class);
        verifyNoInteractions(matchOutcomeService);
    }

    @Test
    void givenCaptain_resolveDispute_shouldThrowOnlyOrganizerAllowedException() {
        when(disputeRepository.findByIdForUpdate(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        stubOrganizer();

        assertThatThrownBy(() -> disputeService.resolveDispute(DISPUTE_ID, CAPTAIN1_ID, buildManualRequest(13, 9)))
                .isInstanceOf(OnlyOrganizerAllowedException.class);
        verifyNoInteractions(matchOutcomeService);
    }
}

package com.tournado.core.disputes.service;

import com.tournado.core.disputes.domain.AiAnalysisResultEntity;
import com.tournado.core.disputes.exception.AiServiceUnavailableException;
import com.tournado.core.disputes.infra.AiAnalysisClient;
import com.tournado.core.disputes.infra.AiAnalysisRequest;
import com.tournado.core.disputes.infra.AiRosterEntry;
import com.tournado.core.disputes.infra.ScreenshotStorage;
import com.tournado.core.disputes.repository.AiAnalysisResultRepository;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.service.MatchQueryService;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.service.TournamentParticipantsQueryService;
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

import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeAiAnalysisServiceTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private AiAnalysisResultRepository aiAnalysisResultRepository;

    @Mock
    private MatchQueryService matchQueryService;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TournamentQueryService tournamentQueryService;

    @Mock
    private TournamentParticipantsQueryService tournamentParticipantsQueryService;

    @Mock
    private ScreenshotStorage screenshotStorage;

    @Mock
    private AiAnalysisClient aiAnalysisClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DisputeAiAnalysisService disputeAiAnalysisService;

    @BeforeEach
    void setUp() {
        disputeAiAnalysisService = new DisputeAiAnalysisService(
                disputeRepository,
                aiAnalysisResultRepository,
                matchQueryService,
                teamQueryService,
                tournamentQueryService,
                tournamentParticipantsQueryService,
                screenshotStorage,
                aiAnalysisClient,
                eventPublisher,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    private void stubDisputeContext() {
        when(aiAnalysisResultRepository.existsByDisputeId(DISPUTE_ID)).thenReturn(false);
        when(disputeRepository.findById(DISPUTE_ID)).thenReturn(Optional.of(buildOpenDisputeEntity()));
        when(matchQueryService.findMatchById(MATCH_ID)).thenReturn(Optional.of(buildDisputedMatch()));
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID)).thenReturn(Optional.of(buildCs2Tournament()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentParticipantsQueryService.findLockedRoster(TOURNAMENT_ID, TEAM2_ID)).thenReturn(buildLockedRoster2());
        when(screenshotStorage.load(SCREENSHOT_PATH)).thenReturn(PNG_BYTES);
    }

    @Test
    void givenSuccessfulAiReply_analyze_shouldSendLockedRostersAndStoreAdvisoryVerdict() {
        stubDisputeContext();
        when(tournamentParticipantsQueryService.findLockedRoster(TOURNAMENT_ID, TEAM1_ID)).thenReturn(buildLockedRoster1());
        when(aiAnalysisClient.analyze(any())).thenReturn(buildSuccessAiReply());

        disputeAiAnalysisService.analyze(DISPUTE_ID);

        ArgumentCaptor<AiAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(AiAnalysisRequest.class);
        ArgumentCaptor<AiAnalysisResultEntity> resultCaptor = ArgumentCaptor.forClass(AiAnalysisResultEntity.class);
        verify(aiAnalysisClient).analyze(requestCaptor.capture());
        verify(aiAnalysisResultRepository).save(resultCaptor.capture());
        verify(eventPublisher).publishEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_AI_ANALYZED, DISPUTE_ID, TOURNAMENT_ID, MATCH_ID));
        assertThat(requestCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(buildExpectedAiRequest(buildExpectedLockedTeam1AiRoster()));
        assertThat(resultCaptor.getValue()).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(buildSuccessAnalysisEntity());
    }

    @Test
    void givenRosterNotLocked_analyze_shouldFallBackToCurrentTeamMembers() {
        stubDisputeContext();
        when(tournamentParticipantsQueryService.findLockedRoster(TOURNAMENT_ID, TEAM1_ID)).thenReturn(List.of());
        when(aiAnalysisClient.analyze(any())).thenReturn(buildSuccessAiReply());

        disputeAiAnalysisService.analyze(DISPUTE_ID);

        ArgumentCaptor<AiAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(AiAnalysisRequest.class);
        verify(aiAnalysisClient).analyze(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).usingRecursiveComparison()
                .isEqualTo(buildExpectedAiRequest(List.of(new AiRosterEntry("BOT Mayer", "76561190000000001"))));
    }

    @Test
    void givenAiServiceDown_analyze_shouldStoreFailedResultSoOrganizerCanDecideManually() {
        stubDisputeContext();
        when(tournamentParticipantsQueryService.findLockedRoster(TOURNAMENT_ID, TEAM1_ID)).thenReturn(buildLockedRoster1());
        when(aiAnalysisClient.analyze(any())).thenThrow(new AiServiceUnavailableException("connection refused", null));

        disputeAiAnalysisService.analyze(DISPUTE_ID);

        ArgumentCaptor<AiAnalysisResultEntity> resultCaptor = ArgumentCaptor.forClass(AiAnalysisResultEntity.class);
        verify(aiAnalysisResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue()).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(buildExpectedFailedAnalysisEntity());
    }

    @Test
    void givenMalformedSuccessReply_analyze_shouldTreatAiOutputAsUntrustedAndStoreUnrecognized() {
        stubDisputeContext();
        when(tournamentParticipantsQueryService.findLockedRoster(TOURNAMENT_ID, TEAM1_ID)).thenReturn(buildLockedRoster1());
        when(aiAnalysisClient.analyze(any())).thenReturn(buildMalformedSuccessAiReply());

        disputeAiAnalysisService.analyze(DISPUTE_ID);

        ArgumentCaptor<AiAnalysisResultEntity> resultCaptor = ArgumentCaptor.forClass(AiAnalysisResultEntity.class);
        verify(aiAnalysisResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue()).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(buildExpectedMalformedReplyEntity());
    }

    @Test
    void givenAnalysisAlreadyStored_analyze_shouldNotCallAiServiceAgain() {
        when(aiAnalysisResultRepository.existsByDisputeId(DISPUTE_ID)).thenReturn(true);

        disputeAiAnalysisService.analyze(DISPUTE_ID);

        verifyNoInteractions(aiAnalysisClient, disputeRepository, eventPublisher);
    }
}

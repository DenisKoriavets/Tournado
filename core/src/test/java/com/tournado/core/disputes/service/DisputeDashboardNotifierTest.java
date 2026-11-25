package com.tournado.core.disputes.service;

import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.service.MatchQueryService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeDashboardNotifierTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private MatchQueryService matchQueryService;

    private DisputeDashboardNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new DisputeDashboardNotifier(messagingTemplate, disputeRepository, matchQueryService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void givenOpenedDispute_onDisputeLifecycleEvent_shouldBroadcastOpenCounterToOrganizerDashboard() {
        when(matchQueryService.findMatchesByTournamentId(TOURNAMENT_ID)).thenReturn(List.of(buildDisputedMatch()));
        when(disputeRepository.countByMatchIdInAndStatus(List.of(MATCH_ID), DisputeStatus.OPEN)).thenReturn(1L);

        notifier.onDisputeLifecycleEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_OPENED, DISPUTE_ID, TOURNAMENT_ID, MATCH_ID));

        verify(messagingTemplate).convertAndSend(
                "/topic/tournaments/" + TOURNAMENT_ID + "/dashboard",
                new DashboardEvent("DISPUTE_OPENED", TOURNAMENT_ID, MATCH_ID, DISPUTE_ID, 1L, NOW));
    }

    @Test
    void givenBrokerFailure_onDisputeLifecycleEvent_shouldSwallowErrorAndNotBreakDisputeFlow() {
        when(matchQueryService.findMatchesByTournamentId(TOURNAMENT_ID)).thenReturn(List.of(buildDisputedMatch()));
        when(disputeRepository.countByMatchIdInAndStatus(List.of(MATCH_ID), DisputeStatus.OPEN)).thenReturn(0L);
        doThrow(new MessagingException("broker down")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        assertThatCode(() -> notifier.onDisputeLifecycleEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_RESOLVED, DISPUTE_ID, TOURNAMENT_ID, MATCH_ID)))
                .doesNotThrowAnyException();
    }
}

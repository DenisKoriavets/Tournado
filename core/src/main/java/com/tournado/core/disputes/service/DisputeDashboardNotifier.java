package com.tournado.core.disputes.service;

import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.service.MatchQueryService;
import com.tournado.core.matches.service.MatchView;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * US-31: pushes the live open-dispute counter to the organizer dashboard after the change is committed.
 * A broker failure never affects the dispute itself.
 */
@Component
public class DisputeDashboardNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(DisputeDashboardNotifier.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final DisputeRepository disputeRepository;
    private final MatchQueryService matchQueryService;
    private final Clock clock;

    public DisputeDashboardNotifier(
            SimpMessageSendingOperations messagingTemplate,
            DisputeRepository disputeRepository,
            MatchQueryService matchQueryService,
            Clock clock) {
        this.messagingTemplate = messagingTemplate;
        this.disputeRepository = disputeRepository;
        this.matchQueryService = matchQueryService;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDisputeLifecycleEvent(DisputeLifecycleEvent event) {
        try {
            List<UUID> matchIds = matchQueryService.findMatchesByTournamentId(event.tournamentId()).stream()
                    .map(MatchView::id)
                    .toList();
            long openCount = matchIds.isEmpty() ? 0 : disputeRepository.countByMatchIdInAndStatus(matchIds, DisputeStatus.OPEN);
            DashboardEvent payload = new DashboardEvent(
                    event.type().name(),
                    event.tournamentId(),
                    event.matchId(),
                    event.disputeId(),
                    openCount,
                    Instant.now(clock));
            messagingTemplate.convertAndSend("/topic/tournaments/" + event.tournamentId() + "/dashboard", payload);
        } catch (RuntimeException ex) {
            LOG.warn("Failed to broadcast dashboard event {} for dispute {}", event.type(), event.disputeId(), ex);
        }
    }
}

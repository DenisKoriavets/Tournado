package com.tournado.core.disputes.service;

import com.tournado.core.common.config.AsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ADR-0006: the AI call runs in the background after the dispute is committed, so opening a dispute
 * never waits for (or fails because of) the AI service.
 */
@Component
public class DisputeAiAnalysisTrigger {

    private final DisputeAiAnalysisService disputeAiAnalysisService;

    public DisputeAiAnalysisTrigger(DisputeAiAnalysisService disputeAiAnalysisService) {
        this.disputeAiAnalysisService = disputeAiAnalysisService;
    }

    @Async(AsyncConfig.AI_ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDisputeOpened(DisputeLifecycleEvent event) {
        if (event.type() == DisputeLifecycleEvent.Type.DISPUTE_OPENED) {
            disputeAiAnalysisService.analyze(event.disputeId());
        }
    }
}

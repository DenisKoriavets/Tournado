package com.tournado.core.disputes.service;

import java.util.UUID;

public record DisputeLifecycleEvent(
        Type type,
        UUID disputeId,
        UUID tournamentId,
        UUID matchId
) {

    public enum Type {
        DISPUTE_OPENED,
        DISPUTE_AI_ANALYZED,
        DISPUTE_RESOLVED
    }
}

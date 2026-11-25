package com.tournado.core.disputes.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload broadcast on {@code /topic/tournaments/{tournamentId}/dashboard} (US-31, ADR-0005).
 */
public record DashboardEvent(
        String type,
        UUID tournamentId,
        UUID matchId,
        UUID disputeId,
        long openDisputesCount,
        Instant occurredAt
) {
}

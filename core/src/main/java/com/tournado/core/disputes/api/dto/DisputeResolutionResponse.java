package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.DisputeResolutionSource;
import java.time.Instant;
import java.util.UUID;

public record DisputeResolutionResponse(
        DisputeResolutionSource decision,
        UUID winnerTeamId,
        Integer team1Score,
        Integer team2Score,
        String resolutionNotes,
        UUID resolvedByUserId,
        Instant resolvedAt
) {
}

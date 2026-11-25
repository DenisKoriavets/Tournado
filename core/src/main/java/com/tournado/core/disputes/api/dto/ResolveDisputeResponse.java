package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.DisputeResolutionSource;
import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.matches.domain.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record ResolveDisputeResponse(
        UUID disputeId,
        DisputeStatus status,
        UUID matchId,
        MatchStatus matchStatus,
        UUID winnerTeamId,
        int team1Score,
        int team2Score,
        DisputeResolutionSource decision,
        Instant resolvedAt
) {
}

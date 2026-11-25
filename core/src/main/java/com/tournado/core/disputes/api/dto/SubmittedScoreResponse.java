package com.tournado.core.disputes.api.dto;

import java.time.Instant;
import java.util.UUID;

public record SubmittedScoreResponse(
        UUID teamId,
        int team1Score,
        int team2Score,
        Instant submittedAt
) {
}

package com.tournado.core.matches.service;

import java.time.Instant;
import java.util.UUID;

public record ScoreSubmissionView(
        UUID teamId,
        int team1Score,
        int team2Score,
        Instant submittedAt
) {
}

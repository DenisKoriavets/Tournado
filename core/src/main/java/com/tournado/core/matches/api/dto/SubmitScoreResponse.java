package com.tournado.core.matches.api.dto;

import com.tournado.core.matches.domain.MatchStatus;
import java.util.UUID;

public record SubmitScoreResponse(
        MatchStatus matchStatus,
        UUID winnerTeamId,
        boolean bothSubmitted,
        boolean scoresMatched
) {
}

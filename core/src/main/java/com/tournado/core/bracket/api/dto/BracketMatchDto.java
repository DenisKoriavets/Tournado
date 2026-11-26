// BracketMatchDto.java
package com.tournado.core.bracket.api.dto;

import com.tournado.core.matches.domain.MatchStatus;
import java.util.UUID;

public record BracketMatchDto(
        UUID matchId,
        int matchOrder,
        MatchStatus status,
        BracketTeamDto team1,
        BracketTeamDto team2,
        UUID winnerTeamId,
        boolean isBye
) {
}
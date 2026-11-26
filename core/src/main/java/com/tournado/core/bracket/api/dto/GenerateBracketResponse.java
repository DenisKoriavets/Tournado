// GenerateBracketResponse.java
package com.tournado.core.bracket.api.dto;

import java.util.UUID;

public record GenerateBracketResponse(
        UUID bracketId,
        int totalRounds,
        int activeTeamsCount,
        int byesAssignedCount,
        int firstRoundMatchesCount
) {
}
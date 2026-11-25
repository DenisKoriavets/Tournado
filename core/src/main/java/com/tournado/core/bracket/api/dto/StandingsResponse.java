// StandingsResponse.java
package com.tournado.core.bracket.api.dto;

import java.util.List;
import java.util.UUID;

public record StandingsResponse(
        UUID tournamentId,
        List<StandingItemDto> standings
) {
}
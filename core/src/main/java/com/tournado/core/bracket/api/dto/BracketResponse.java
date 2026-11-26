// BracketResponse.java
package com.tournado.core.bracket.api.dto;

import com.tournado.core.tournaments.domain.TournamentStatus;
import java.util.List;
import java.util.UUID;

public record BracketResponse(
        UUID tournamentId,
        TournamentStatus status,
        List<BracketRoundDto> rounds
) {
}
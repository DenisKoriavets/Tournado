package com.tournado.core.disputes.api.dto;

import java.util.List;
import java.util.UUID;

public record TournamentDisputesResponse(
        UUID tournamentId,
        long openDisputesCount,
        List<DisputeSummaryResponse> disputes
) {
}

package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputeSummaryResponse(
        UUID disputeId,
        UUID matchId,
        int roundNumber,
        int matchOrder,
        DisputeStatus status,
        DisputeTeamResponse team1,
        DisputeTeamResponse team2,
        UUID openedByTeamId,
        AiAnalysisStatus aiAnalysisStatus,
        Integer aiRecognizedScoreTeam1,
        Integer aiRecognizedScoreTeam2,
        BigDecimal aiConfidence,
        Boolean allRosterNicknamesFound,
        Instant createdAt,
        Instant resolvedAt
) {
}

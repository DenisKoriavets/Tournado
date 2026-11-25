package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeCardResponse(
        UUID disputeId,
        UUID matchId,
        UUID tournamentId,
        DisputeStatus status,
        String screenshotUrl,
        int roundNumber,
        int matchOrder,
        DisputeTeamResponse team1,
        DisputeTeamResponse team2,
        UUID openedByTeamId,
        List<SubmittedScoreResponse> submittedScores,
        AiAnalysisStatus aiAnalysisStatus,
        AiAnalysisResponse aiAnalysis,
        boolean canApproveAiSuggestion,
        DisputeResolutionResponse resolution,
        Instant createdAt
) {
}

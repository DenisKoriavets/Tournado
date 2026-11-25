package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeStatus;
import java.util.UUID;

public record OpenDisputeResponse(
        UUID disputeId,
        UUID matchId,
        DisputeStatus status,
        String screenshotUrl,
        AiAnalysisStatus aiAnalysisStatus
) {
}

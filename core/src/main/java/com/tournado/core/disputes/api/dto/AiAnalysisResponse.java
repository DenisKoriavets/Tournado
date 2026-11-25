package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.AiAnalysisStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AiAnalysisResponse(
        AiAnalysisStatus status,
        Integer recognizedScoreTeam1,
        Integer recognizedScoreTeam2,
        BigDecimal confidence,
        boolean allRosterNicknamesFound,
        List<String> detectedNicknames,
        List<NicknameCheckResponse> nicknameChecks,
        Instant analyzedAt
) {
}

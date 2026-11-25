package com.tournado.core.disputes.infra;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response of the AI service (docs/contracts/api-contracts.md §4.2). Treated as untrusted data (NFR-22).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisReport(
        String status,
        RecognizedScore recognizedScore,
        Double confidence,
        List<String> nicknamesDetected,
        List<NicknameCheck> nicknameChecks,
        boolean allRosterNicknamesFound,
        String reason,
        String provider,
        Long processingTimeMs
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecognizedScore(Integer team1, Integer team2) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NicknameCheck(
            String nickname,
            String side,
            boolean inRoster,
            String matchedTeam,
            String matchedRosterName
    ) {
    }
}

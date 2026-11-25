package com.tournado.core.disputes.infra;

import com.tournado.core.common.domain.Discipline;
import java.util.List;

/**
 * Form fields of {@code POST /api/v1/ai/analyze-dispute} (docs/contracts/api-contracts.md §4.1).
 */
public record AiAnalysisRequest(
        Discipline discipline,
        String team1Name,
        String team2Name,
        List<AiRosterEntry> team1Roster,
        List<AiRosterEntry> team2Roster,
        byte[] screenshot,
        String screenshotFilename,
        String screenshotMimeType
) {
}

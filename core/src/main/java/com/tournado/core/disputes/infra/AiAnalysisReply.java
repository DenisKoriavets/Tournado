package com.tournado.core.disputes.infra;

import java.util.Map;

/**
 * Parsed AI report plus the raw JSON payload kept for audit ({@code ai_analysis_results.raw_response_payload}).
 */
public record AiAnalysisReply(AiAnalysisReport report, Map<String, Object> rawPayload) {
}

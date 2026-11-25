package com.tournado.core.disputes.infra;

import com.tournado.core.disputes.exception.AiServiceUnavailableException;

public interface AiAnalysisClient {

    /**
     * Synchronous call to the internal AI service (ADR-0006).
     *
     * @throws AiServiceUnavailableException when the service is unreachable, times out or answers garbage
     */
    AiAnalysisReply analyze(AiAnalysisRequest request);
}

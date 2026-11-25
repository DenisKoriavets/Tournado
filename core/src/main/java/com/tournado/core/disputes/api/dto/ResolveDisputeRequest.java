package com.tournado.core.disputes.api.dto;

import com.tournado.core.disputes.domain.DisputeResolutionSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveDisputeRequest(
        @NotNull(message = "Decision is required: AI_SUGGESTION or MANUAL_SCORE")
        DisputeResolutionSource decision,

        @Min(value = 0, message = "Score must be a non-negative integer")
        @Max(value = 999, message = "Score must not exceed 999")
        Integer team1Score,

        @Min(value = 0, message = "Score must be a non-negative integer")
        @Max(value = 999, message = "Score must not exceed 999")
        Integer team2Score,

        @Size(max = 2000, message = "Resolution notes must not exceed 2000 characters")
        String resolutionNotes
) {
}

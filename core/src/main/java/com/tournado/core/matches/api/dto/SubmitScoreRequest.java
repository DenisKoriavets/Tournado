package com.tournado.core.matches.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SubmitScoreRequest(
        @NotNull(message = "Score for team 1 is required")
        @Min(value = 0, message = "Score must be a non-negative integer")
        @Max(value = 999, message = "Score must not exceed 999")
        Integer team1Score,

        @NotNull(message = "Score for team 2 is required")
        @Min(value = 0, message = "Score must be a non-negative integer")
        @Max(value = 999, message = "Score must not exceed 999")
        Integer team2Score
) {
}

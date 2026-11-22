package com.tournado.core.teams.api.dto;

import com.tournado.core.common.domain.Discipline;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(
        @NotBlank(message = "Team name cannot be blank")
        @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters")
        String name,

        @NotNull(message = "Discipline is required")
        Discipline discipline
) {
}

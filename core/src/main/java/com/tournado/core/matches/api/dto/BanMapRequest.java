package com.tournado.core.matches.api.dto;

import jakarta.validation.constraints.NotBlank;

public record BanMapRequest(
        @NotBlank(message = "Map name must not be blank")
        String mapName
) {
}

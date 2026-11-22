package com.tournado.core.teams.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record TeamMemberResponse(
        UUID userId,
        String displayName,
        @JsonProperty("isCaptain") boolean isCaptain,
        String gameIdentifier
) {
}

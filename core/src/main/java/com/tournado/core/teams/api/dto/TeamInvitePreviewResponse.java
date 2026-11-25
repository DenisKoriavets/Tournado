package com.tournado.core.teams.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tournado.core.common.domain.Discipline;
import java.util.UUID;

public record TeamInvitePreviewResponse(
        String token,
        UUID teamId,
        String teamName,
        Discipline discipline,
        int memberCount,
        int maxMembers,
        @JsonProperty("isFull") boolean isFull
) {
}

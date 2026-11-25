package com.tournado.core.teams.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tournado.core.common.domain.Discipline;
import java.util.List;
import java.util.UUID;

public record TeamDetailsResponse(
        UUID id,
        String name,
        Discipline discipline,
        UUID captainId,
        int memberCount,
        int maxMembers,
        @JsonProperty("isFull") boolean isFull,
        List<TeamMemberResponse> members
) {
}

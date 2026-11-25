package com.tournado.core.disputes.api.dto;

import java.util.UUID;

public record DisputeTeamResponse(
        UUID id,
        String name
) {
}

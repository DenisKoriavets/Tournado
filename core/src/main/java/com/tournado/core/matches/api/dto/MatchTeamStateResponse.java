package com.tournado.core.matches.api.dto;

import java.util.UUID;

public record MatchTeamStateResponse(
        UUID id,
        String name,
        boolean isReady
) {
}

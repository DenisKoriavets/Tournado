package com.tournado.core.matches.api.dto;

import java.util.UUID;

public record BannedMapResponse(
        String mapName,
        UUID bannedByTeamId,
        int order
) {
}

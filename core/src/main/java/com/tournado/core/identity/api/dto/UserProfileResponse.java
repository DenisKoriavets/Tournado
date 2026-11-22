package com.tournado.core.identity.api.dto;

import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String steamId,
        String displayName,
        String supercellPlayerTag,
        String chessUsername,
        String avatarUrl
) {
}

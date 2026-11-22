package com.tournado.core.identity.service;

import java.util.UUID;

public record UserProfileSummary(
        UUID id,
        String email,
        String steamId,
        String displayName,
        String supercellPlayerTag,
        String chessUsername,
        String avatarUrl
) {
}

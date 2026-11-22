package com.tournado.core.teams.api.dto;

import java.time.Instant;

public record TeamInviteResponse(
        String token,
        String inviteUrl,
        Instant expiresAt
) {
}

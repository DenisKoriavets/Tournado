package com.tournado.core.common.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String displayName,
        String email,
        String steamId
) {
}

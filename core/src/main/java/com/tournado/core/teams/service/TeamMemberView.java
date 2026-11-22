package com.tournado.core.teams.service;

import java.util.UUID;

public record TeamMemberView(
        UUID userId,
        String displayName,
        boolean isCaptain,
        String steamId,
        String supercellPlayerTag,
        String chessUsername
) {
}

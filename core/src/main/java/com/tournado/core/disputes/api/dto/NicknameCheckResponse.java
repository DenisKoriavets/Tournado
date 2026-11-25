package com.tournado.core.disputes.api.dto;

import java.util.UUID;

public record NicknameCheckResponse(
        String nickname,
        UUID sideTeamId,
        boolean inRoster,
        UUID matchedTeamId,
        String matchedRosterName
) {
}

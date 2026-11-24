package com.tournado.core.matches.api.dto;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.domain.MatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchRoomResponse(
        UUID matchId,
        MatchStatus status,
        Discipline discipline,
        Instant readyDeadlineAt,
        Instant banDeadlineAt,
        MatchTeamStateResponse team1,
        MatchTeamStateResponse team2,
        UUID currentBanTurnTeamId,
        List<BannedMapResponse> bannedMaps,
        List<String> remainingMaps,
        String decidedMap,
        MatchCredentialsResponse credentials
) {
}

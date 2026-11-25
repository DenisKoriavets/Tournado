package com.tournado.core.matches.api.dto;

import com.tournado.core.common.domain.Discipline;

public record MatchCredentialsResponse(
        Discipline discipline,
        String cs2ConnectString,
        String brawlStarsCode,
        String chessGameUrl,
        String decidedMap
) {
}

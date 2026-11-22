package com.tournado.core.identity.api.dto;

public record UpdateGameIdentifiersRequest(
        String steamId,
        String supercellPlayerTag,
        String chessUsername
) {
}

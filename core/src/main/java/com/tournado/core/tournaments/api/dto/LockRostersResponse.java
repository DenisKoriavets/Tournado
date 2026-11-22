package com.tournado.core.tournaments.api.dto;

public record LockRostersResponse(
        String status,
        int totalSnapshottedPlayers
) {
}

// BracketTeamDto.java
package com.tournado.core.bracket.api.dto;

import java.util.UUID;

public record BracketTeamDto(
        UUID id,
        String name
) {
}
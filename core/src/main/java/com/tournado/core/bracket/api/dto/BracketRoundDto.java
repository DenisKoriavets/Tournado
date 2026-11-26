// BracketRoundDto.java
package com.tournado.core.bracket.api.dto;

import java.util.List;

public record BracketRoundDto(
        int roundNumber,
        String roundName,
        List<BracketMatchDto> matches
) {
}
package com.tournado.core.tournaments.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.teams.service.TeamMemberView;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class GameIdentifierExtractor {

    private record IdentifierSpec(
            String fieldName,
            String requirementLabel,
            Function<TeamMemberView, String> accessor
    ) {
    }

    private final Map<Discipline, IdentifierSpec> specs;

    public GameIdentifierExtractor() {
        EnumMap<Discipline, IdentifierSpec> map = new EnumMap<>(Discipline.class);
        map.put(Discipline.CS2, new IdentifierSpec("steamId", "SteamID64", TeamMemberView::steamId));
        map.put(Discipline.BRAWL_STARS, new IdentifierSpec(
                "supercellPlayerTag",
                "Supercell Player Tag",
                TeamMemberView::supercellPlayerTag
        ));
        map.put(Discipline.CHESS, new IdentifierSpec(
                "chessUsername",
                "Chess.com Username",
                TeamMemberView::chessUsername
        ));
        this.specs = Map.copyOf(map);
    }

    public Optional<String> extract(Discipline discipline, TeamMemberView member) {
        Objects.requireNonNull(member, "member must not be null");
        String raw = specFor(discipline).accessor().apply(member);
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }

    public String getFieldName(Discipline discipline) {
        return specFor(discipline).fieldName();
    }

    public String getRequirementLabel(Discipline discipline) {
        return specFor(discipline).requirementLabel();
    }

    private IdentifierSpec specFor(Discipline discipline) {
        Objects.requireNonNull(discipline, "discipline must not be null");
        IdentifierSpec spec = specs.get(discipline);
        if (spec == null) {
            throw new IllegalArgumentException("Unsupported discipline: " + discipline);
        }
        return spec;
    }
}

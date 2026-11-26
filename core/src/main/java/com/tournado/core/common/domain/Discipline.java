package com.tournado.core.common.domain;

import java.util.List;

public enum Discipline {
    CS2(5, List.of("de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo")),
    BRAWL_STARS(3, List.of("Shooting Star", "Backyard Bowl", "Hard Rock Mine", "Dueling Beetles", "Ring of Fire")),
    // US-08 checklist 4 / sc.2: Chess.com has no map pool (time controls are not maps and are never banned).
    CHESS(1, List.of());

    private final int teamSize;
    private final List<String> defaultMapPool;

    Discipline(int teamSize, List<String> defaultMapPool) {
        this.teamSize = teamSize;
        this.defaultMapPool = defaultMapPool;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public List<String> getDefaultMapPool() {
        return defaultMapPool;
    }
}

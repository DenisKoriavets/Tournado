package com.tournado.core.tournaments.domain;

import java.util.List;

public enum Discipline {
    CS2(5, List.of("de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo")),
    BRAWL_STARS(3, List.of("Shooting Star", "Backyard Bowl", "Hard Rock Mine", "Dueling Beetles", "Ring of Fire")),
    CHESS(1, List.of("Blitz 3+2", "Rapid 10+0"));

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

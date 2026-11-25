package com.tournado.core.disputes.domain;

/**
 * One nickname read by the AI from the screenshot, compared to the locked rosters (US-33).
 * {@code side} / {@code matchedTeam} are "TEAM1" / "TEAM2" (or null), relative to the match slots.
 */
public record StoredNicknameCheck(
        String nickname,
        String side,
        boolean inRoster,
        String matchedTeam,
        String matchedRosterName
) {
}

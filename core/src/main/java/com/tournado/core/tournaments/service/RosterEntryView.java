package com.tournado.core.tournaments.service;

import java.util.UUID;

/**
 * One player of a roster frozen at Roster Lock (US-14) - the reference list for the AI nickname check (US-33).
 */
public record RosterEntryView(
        UUID userId,
        String displayName,
        String gameIdentifier,
        boolean isCaptain
) {
}

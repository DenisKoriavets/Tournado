package com.tournado.core.teams.service;

import java.util.UUID;

public interface TournamentRosterLockChecker {

    boolean isTeamRosterLocked(UUID teamId);
}

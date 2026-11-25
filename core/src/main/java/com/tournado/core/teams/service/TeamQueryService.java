package com.tournado.core.teams.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TeamQueryService {

    Optional<TeamView> findTeamById(UUID teamId);

    Map<UUID, TeamView> findTeamsByIds(Collection<UUID> teamIds);
}

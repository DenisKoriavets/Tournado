package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentQueryService {

    Optional<TournamentResponse> findTournamentById(UUID id);

    List<TournamentSummaryResponse> findAllTournaments();
}

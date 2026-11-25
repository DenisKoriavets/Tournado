package com.tournado.core.tournaments.service;

import com.tournado.core.matches.service.MatchCompletedEvent;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.repository.TournamentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * US-28 / US-34 -> US-35 hook: once the final match is closed the tournament becomes COMPLETED (BR-51).
 * Runs synchronously inside the transaction that closed the match. Standings/CSV (US-35, US-36) are out of scope here.
 */
@Component
public class TournamentCompletionListener {

    private final TournamentRepository tournamentRepository;

    public TournamentCompletionListener(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    @EventListener
    public void onMatchCompleted(MatchCompletedEvent event) {
        if (!event.finalMatch()) {
            return;
        }
        tournamentRepository.findById(event.tournamentId()).ifPresent(tournament -> {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournamentRepository.save(tournament);
        });
    }
}

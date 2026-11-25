package com.tournado.core.bracket.service;

import com.tournado.core.matches.service.BracketMatchView;
import com.tournado.core.matches.service.MatchBracketService;
import com.tournado.core.matches.service.MatchCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BracketMatchEventListener {

    private final BracketService bracketService;
    private final MatchBracketService matchBracketService;

    public BracketMatchEventListener(BracketService bracketService, MatchBracketService matchBracketService) {
        this.bracketService = bracketService;
        this.matchBracketService = matchBracketService;
    }

    @EventListener
    public void onMatchCompleted(MatchCompletedEvent event) {
        BracketMatchView match = matchBracketService.findMatchById(event.matchId())
                .orElseThrow(() -> new IllegalStateException(
                        "Completed match not found: '" + event.matchId() + "'."
                ));
        bracketService.advanceWinnerOnMatchCompleted(
                event.tournamentId(),
                match.roundNumber(),
                match.matchOrder(),
                event.winnerTeamId()
        );
    }
}
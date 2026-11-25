package com.tournado.core.bracket.service;

import com.tournado.core.matches.service.MatchCompletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BracketMatchEventListener {

    private final BracketService bracketService;

    public BracketMatchEventListener(BracketService bracketService) {
        this.bracketService = bracketService;
    }

    @EventListener
    public void onMatchCompleted(MatchCompletedEvent event) {
        bracketService.advanceWinnerOnMatchCompleted(
                event.tournamentId(),
                event.roundNumber(),
                event.matchOrder(),
                event.winnerTeamId()
        );
    }
}
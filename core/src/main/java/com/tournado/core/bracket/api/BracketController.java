package com.tournado.core.bracket.api;

import com.tournado.core.bracket.api.dto.BracketResponse;
import com.tournado.core.bracket.api.dto.GenerateBracketResponse;
import com.tournado.core.bracket.api.dto.StandingsResponse;
import com.tournado.core.bracket.service.BracketService;
import com.tournado.core.common.security.CurrentUserProvider;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments")
public class BracketController {

    private final BracketService bracketService;
    private final CurrentUserProvider currentUserProvider;

    public BracketController(BracketService bracketService, CurrentUserProvider currentUserProvider) {
        this.bracketService = bracketService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{tournamentId}/generate-bracket")
    public ResponseEntity<GenerateBracketResponse> generateBracket(@PathVariable UUID tournamentId) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        GenerateBracketResponse response = bracketService.generateBracket(tournamentId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{tournamentId}/bracket")
    public ResponseEntity<BracketResponse> getBracket(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(bracketService.getBracket(tournamentId));
    }

    @GetMapping("/{tournamentId}/standings")
    public ResponseEntity<StandingsResponse> getStandings(@PathVariable UUID tournamentId) {
        return ResponseEntity.ok(bracketService.getStandings(tournamentId));
    }
}
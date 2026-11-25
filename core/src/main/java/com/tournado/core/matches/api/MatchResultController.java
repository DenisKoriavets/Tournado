package com.tournado.core.matches.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.matches.api.dto.SubmitScoreRequest;
import com.tournado.core.matches.api.dto.SubmitScoreResponse;
import com.tournado.core.matches.service.MatchResultService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchResultController {

    private final MatchResultService matchResultService;
    private final CurrentUserProvider currentUserProvider;

    public MatchResultController(MatchResultService matchResultService, CurrentUserProvider currentUserProvider) {
        this.matchResultService = matchResultService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{matchId}/scores")
    public ResponseEntity<SubmitScoreResponse> submitScore(
            @PathVariable UUID matchId,
            @Valid @RequestBody SubmitScoreRequest request) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(matchResultService.submitScore(matchId, userId, request.team1Score(), request.team2Score()));
    }
}

package com.tournado.core.matches.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.matches.api.dto.BanMapRequest;
import com.tournado.core.matches.api.dto.MatchCredentialsResponse;
import com.tournado.core.matches.api.dto.MatchRoomResponse;
import com.tournado.core.matches.api.dto.ReadyResponse;
import com.tournado.core.matches.service.MatchFlowService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchFlowService matchFlowService;
    private final CurrentUserProvider currentUserProvider;

    public MatchController(MatchFlowService matchFlowService, CurrentUserProvider currentUserProvider) {
        this.matchFlowService = matchFlowService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{matchId}/room")
    public ResponseEntity<MatchRoomResponse> getRoom(@PathVariable UUID matchId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(matchFlowService.getRoom(matchId, userId));
    }

    @PostMapping("/{matchId}/ready")
    public ResponseEntity<ReadyResponse> markReady(@PathVariable UUID matchId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(matchFlowService.markReady(matchId, userId));
    }

    @PostMapping("/{matchId}/ban")
    public ResponseEntity<MatchRoomResponse> banMap(
            @PathVariable UUID matchId,
            @Valid @RequestBody BanMapRequest request) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(matchFlowService.banMap(matchId, userId, request.mapName()));
    }

    @GetMapping("/{matchId}/credentials")
    public ResponseEntity<MatchCredentialsResponse> getCredentials(@PathVariable UUID matchId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(matchFlowService.getCredentials(matchId, userId));
    }
}

package com.tournado.core.disputes.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.disputes.api.dto.DisputeCardResponse;
import com.tournado.core.disputes.api.dto.OpenDisputeResponse;
import com.tournado.core.disputes.api.dto.ResolveDisputeRequest;
import com.tournado.core.disputes.api.dto.ResolveDisputeResponse;
import com.tournado.core.disputes.api.dto.TournamentDisputesResponse;
import com.tournado.core.disputes.service.DisputeService;
import com.tournado.core.disputes.service.ScreenshotContent;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DisputeController {

    private final DisputeService disputeService;
    private final CurrentUserProvider currentUserProvider;

    public DisputeController(DisputeService disputeService, CurrentUserProvider currentUserProvider) {
        this.disputeService = disputeService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/api/v1/matches/{matchId}/disputes")
    public ResponseEntity<OpenDisputeResponse> openDispute(
            @PathVariable UUID matchId,
            @RequestParam(name = "screenshot", required = false) MultipartFile screenshot) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        OpenDisputeResponse response = disputeService.openDispute(matchId, userId, screenshot);
        return ResponseEntity.created(URI.create("/api/v1/disputes/" + response.disputeId())).body(response);
    }

    @GetMapping("/api/v1/tournaments/{tournamentId}/disputes")
    public ResponseEntity<TournamentDisputesResponse> listTournamentDisputes(@PathVariable UUID tournamentId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(disputeService.listTournamentDisputes(tournamentId, userId));
    }

    @GetMapping("/api/v1/disputes/{disputeId}")
    public ResponseEntity<DisputeCardResponse> getDispute(@PathVariable UUID disputeId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(disputeService.getDisputeCard(disputeId, userId));
    }

    @GetMapping("/api/v1/disputes/{disputeId}/screenshot")
    public ResponseEntity<byte[]> getScreenshot(@PathVariable UUID disputeId) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        ScreenshotContent screenshot = disputeService.getScreenshot(disputeId, userId);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(screenshot.mimeType()))
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(screenshot.content());
    }

    @PostMapping("/api/v1/disputes/{disputeId}/resolve")
    public ResponseEntity<ResolveDisputeResponse> resolveDispute(
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(disputeService.resolveDispute(disputeId, userId, request));
    }
}

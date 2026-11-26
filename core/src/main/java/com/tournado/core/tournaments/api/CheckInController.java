package com.tournado.core.tournaments.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.tournaments.api.dto.CheckInRequest;
import com.tournado.core.tournaments.api.dto.CheckInResponse;
import com.tournado.core.tournaments.service.RegistrationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments")
public class CheckInController {

    private final RegistrationService registrationService;
    private final CurrentUserProvider currentUserProvider;

    public CheckInController(RegistrationService registrationService, CurrentUserProvider currentUserProvider) {
        this.registrationService = registrationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{tournamentId}/check-in")
    public ResponseEntity<CheckInResponse> checkIn(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody CheckInRequest request) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        CheckInResponse response = registrationService.checkInTeam(tournamentId, request.teamId(), actorUserId);
        return ResponseEntity.ok(response);
    }
}
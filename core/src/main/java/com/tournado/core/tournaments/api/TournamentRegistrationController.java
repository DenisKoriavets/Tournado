package com.tournado.core.tournaments.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.tournaments.api.dto.LockRostersResponse;
import com.tournado.core.tournaments.api.dto.RegisterTeamRequest;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.service.RegistrationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tournaments/{tournamentId}")
public class TournamentRegistrationController {

    private final RegistrationService registrationService;
    private final CurrentUserProvider currentUserProvider;

    public TournamentRegistrationController(
            RegistrationService registrationService,
            CurrentUserProvider currentUserProvider) {
        this.registrationService = registrationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterTeamResponse> registerTeam(
            @PathVariable UUID tournamentId,
            @Valid @RequestBody RegisterTeamRequest request) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        RegisterTeamResponse response = registrationService.registerTeam(tournamentId, request.teamId(), actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/registrations")
    public ResponseEntity<TournamentRegistrationsResponse> getRegistrations(@PathVariable UUID tournamentId) {
        TournamentRegistrationsResponse response = registrationService.getRegistrations(tournamentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/registrations/{teamId}")
    public ResponseEntity<Void> removeTeamRegistration(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        registrationService.removeTeamRegistration(tournamentId, teamId, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lock-rosters")
    public ResponseEntity<LockRostersResponse> lockRosters(@PathVariable UUID tournamentId) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        LockRostersResponse response = registrationService.lockRosters(tournamentId, actorUserId);
        return ResponseEntity.ok(response);
    }
}

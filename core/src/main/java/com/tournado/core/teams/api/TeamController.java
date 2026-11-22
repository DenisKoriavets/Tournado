package com.tournado.core.teams.api;

import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.teams.api.dto.CreateTeamRequest;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInvitePreviewResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.api.dto.TeamSummaryResponse;
import com.tournado.core.teams.service.TeamInviteService;
import com.tournado.core.teams.service.TeamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;
    private final TeamInviteService teamInviteService;
    private final CurrentUserProvider currentUserProvider;

    public TeamController(
            TeamService teamService,
            TeamInviteService teamInviteService,
            CurrentUserProvider currentUserProvider) {
        this.teamService = teamService;
        this.teamInviteService = teamInviteService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    public ResponseEntity<TeamSummaryResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        UUID creatorUserId = currentUserProvider.requireCurrentUser().userId();
        TeamSummaryResponse response = teamService.createTeam(request, creatorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<TeamSummaryResponse>> getMyTeams() {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(teamService.getMyTeams(userId));
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetailsResponse> getTeamById(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamById(teamId));
    }

    @PostMapping("/{teamId}/invites")
    public ResponseEntity<TeamInviteResponse> createInvite(@PathVariable UUID teamId) {
        UUID actorUserId = currentUserProvider.requireCurrentUser().userId();
        TeamInviteResponse response = teamInviteService.createInvite(teamId, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invites/{token}")
    public ResponseEntity<TeamInvitePreviewResponse> previewInvite(@PathVariable String token) {
        return ResponseEntity.ok(teamInviteService.previewInvite(token));
    }

    @PostMapping("/join/{token}")
    public ResponseEntity<TeamDetailsResponse> joinTeamByToken(@PathVariable String token) {
        UUID userId = currentUserProvider.requireCurrentUser().userId();
        return ResponseEntity.ok(teamInviteService.joinTeamByToken(token, userId));
    }
}

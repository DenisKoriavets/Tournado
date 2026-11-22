package com.tournado.core.teams.service;

import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInvitePreviewResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.domain.TeamEntity;
import com.tournado.core.teams.domain.TeamInviteEntity;
import com.tournado.core.teams.domain.TeamMemberEntity;
import com.tournado.core.teams.exception.InviteLinkExpiredException;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.exception.PlayerAlreadyInTeamException;
import com.tournado.core.teams.exception.RosterFullException;
import com.tournado.core.teams.exception.RosterLockedException;
import com.tournado.core.teams.exception.TeamNotFoundException;
import com.tournado.core.teams.repository.TeamInviteRepository;
import com.tournado.core.teams.repository.TeamMemberRepository;
import com.tournado.core.teams.repository.TeamRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamInviteService {

    private static final int INVITE_TOKEN_BYTE_LENGTH = 9;
    private static final long INVITE_TTL_DAYS = 7L;
    private static final String INVITE_TOKEN_PREFIX = "inv_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final TournamentRosterLockChecker rosterLockChecker;
    private final TeamService teamService;
    private final AuthProperties authProperties;
    private final Clock clock;

    public TeamInviteService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamInviteRepository teamInviteRepository,
            TournamentRosterLockChecker rosterLockChecker,
            TeamService teamService,
            AuthProperties authProperties,
            Clock clock) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.rosterLockChecker = rosterLockChecker;
        this.teamService = teamService;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    @Transactional
    public TeamInviteResponse createInvite(UUID teamId, UUID actorUserId) {
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        if (!team.getCaptainId().equals(actorUserId)) {
            throw new OnlyCaptainAllowedException();
        }
        if (rosterLockChecker.isTeamRosterLocked(teamId)) {
            throw new RosterLockedException();
        }

        int memberCount = teamMemberRepository.countByTeamId(teamId);
        if (memberCount >= team.getDiscipline().getTeamSize()) {
            throw new RosterFullException(team.getName(), team.getDiscipline());
        }

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(INVITE_TTL_DAYS, ChronoUnit.DAYS);
        String token = generateInviteToken();

        TeamInviteEntity invite = new TeamInviteEntity();
        invite.setId(UUID.randomUUID());
        invite.setTeamId(team.getId());
        invite.setToken(token);
        invite.setCreatedBy(actorUserId);
        invite.setActive(true);
        invite.setExpiresAt(expiresAt);
        invite.setCreatedAt(now);
        teamInviteRepository.save(invite);

        String inviteUrl = trimTrailingSlash(authProperties.publicBaseUrl()) + "/join/" + token;
        return new TeamInviteResponse(token, inviteUrl, expiresAt);
    }

    @Transactional(readOnly = true)
    public TeamInvitePreviewResponse previewInvite(String token) {
        TeamInviteEntity invite = requireValidInvite(token);
        TeamEntity team = teamRepository.findById(invite.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException(invite.getTeamId()));

        int memberCount = teamMemberRepository.countByTeamId(team.getId());
        int maxMembers = team.getDiscipline().getTeamSize();
        return new TeamInvitePreviewResponse(
                invite.getToken(),
                team.getId(),
                team.getName(),
                team.getDiscipline(),
                memberCount,
                maxMembers,
                memberCount >= maxMembers
        );
    }

    @Transactional
    public TeamDetailsResponse joinTeamByToken(String token, UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        TeamInviteEntity invite = requireValidInvite(token);
        TeamEntity team = teamRepository.findById(invite.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException(invite.getTeamId()));

        if (rosterLockChecker.isTeamRosterLocked(team.getId())) {
            throw new RosterLockedException();
        }
        if (teamMemberRepository.existsByTeamIdAndUserId(team.getId(), userId)) {
            throw new PlayerAlreadyInTeamException();
        }

        int currentCount = teamMemberRepository.countByTeamId(team.getId());
        if (currentCount >= team.getDiscipline().getTeamSize()) {
            throw new RosterFullException(team.getName(), team.getDiscipline());
        }

        TeamMemberEntity member = new TeamMemberEntity();
        member.setId(UUID.randomUUID());
        member.setTeamId(team.getId());
        member.setUserId(userId);
        member.setJoinedAt(Instant.now(clock));
        teamMemberRepository.save(member);

        return teamService.getTeamById(team.getId());
    }

    private TeamInviteEntity requireValidInvite(String token) {
        if (token == null || token.isBlank()) {
            throw new InviteLinkExpiredException();
        }
        TeamInviteEntity invite = teamInviteRepository.findByToken(token.trim())
                .orElseThrow(InviteLinkExpiredException::new);
        Instant now = Instant.now(clock);
        if (!invite.isActive() || invite.getExpiresAt().isBefore(now)) {
            throw new InviteLinkExpiredException();
        }
        return invite;
    }

    private String generateInviteToken() {
        byte[] bytes = new byte[INVITE_TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return INVITE_TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

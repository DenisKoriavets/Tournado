package com.tournado.core.teams.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.identity.service.UserProfileSummary;
import com.tournado.core.identity.service.UserQueryService;
import com.tournado.core.teams.api.dto.CreateTeamRequest;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamMemberResponse;
import com.tournado.core.teams.api.dto.TeamSummaryResponse;
import com.tournado.core.teams.domain.TeamEntity;
import com.tournado.core.teams.domain.TeamMemberEntity;
import com.tournado.core.teams.exception.TeamNameAlreadyExistsException;
import com.tournado.core.teams.exception.TeamNotFoundException;
import com.tournado.core.teams.repository.TeamMemberRepository;
import com.tournado.core.teams.repository.TeamRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService implements TeamQueryService {

    private static final String UNKNOWN_PLAYER_NAME = "Unknown Player";

    private static final Map<Discipline, Function<UserProfileSummary, String>> GAME_IDENTIFIER_EXTRACTORS = Map.of(
            Discipline.CS2, UserProfileSummary::steamId,
            Discipline.BRAWL_STARS, UserProfileSummary::supercellPlayerTag,
            Discipline.CHESS, UserProfileSummary::chessUsername
    );

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserQueryService userQueryService;
    private final Clock clock;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserQueryService userQueryService,
            Clock clock) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userQueryService = userQueryService;
        this.clock = clock;
    }

    @Transactional
    public TeamSummaryResponse createTeam(CreateTeamRequest request, UUID creatorUserId) {
        Objects.requireNonNull(creatorUserId, "creatorUserId must not be null");
        String normalizedName = request.name().trim();
        if (teamRepository.existsByNameIgnoreCaseAndDiscipline(normalizedName, request.discipline())) {
            throw new TeamNameAlreadyExistsException(normalizedName, request.discipline());
        }

        Instant now = Instant.now(clock);
        TeamEntity team = new TeamEntity();
        team.setId(UUID.randomUUID());
        team.setName(normalizedName);
        team.setDiscipline(request.discipline());
        team.setCaptainId(creatorUserId);
        team.setCreatedAt(now);
        team.setUpdatedAt(now);
        team.setVersion(0L);

        TeamEntity savedTeam = teamRepository.save(team);

        TeamMemberEntity captainMember = new TeamMemberEntity();
        captainMember.setId(UUID.randomUUID());
        captainMember.setTeamId(savedTeam.getId());
        captainMember.setUserId(creatorUserId);
        captainMember.setJoinedAt(now);
        teamMemberRepository.save(captainMember);

        return toSummaryResponse(savedTeam, 1);
    }

    @Transactional(readOnly = true)
    public TeamDetailsResponse getTeamById(UUID teamId) {
        TeamEntity team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException(teamId));
        List<TeamMemberEntity> members = teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(teamId);
        Map<UUID, UserProfileSummary> usersById = loadUsersByIds(members);

        List<TeamMemberResponse> memberResponses = members.stream()
                .map(member -> toMemberResponse(team, member, usersById.get(member.getUserId())))
                .toList();

        int memberCount = members.size();
        int maxMembers = team.getDiscipline().getTeamSize();
        return new TeamDetailsResponse(
                team.getId(),
                team.getName(),
                team.getDiscipline(),
                team.getCaptainId(),
                memberCount,
                maxMembers,
                memberCount >= maxMembers,
                memberResponses
        );
    }

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> getMyTeams(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        List<TeamMemberEntity> memberships = teamMemberRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<UUID> teamIds = memberships.stream()
                .map(TeamMemberEntity::getTeamId)
                .distinct()
                .toList();
        List<TeamEntity> teams = teamRepository.findByIdIn(teamIds);
        if (teams.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> memberCountsByTeamId = teamMemberRepository.findByTeamIdIn(teamIds).stream()
                .collect(Collectors.groupingBy(TeamMemberEntity::getTeamId, Collectors.counting()));

        return teams.stream()
                .map(team -> toSummaryResponse(team, memberCountsByTeamId.getOrDefault(team.getId(), 0L).intValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeamView> findTeamById(UUID teamId) {
        return teamRepository.findById(teamId).map(team -> {
            List<TeamMemberEntity> members = teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(teamId);
            Map<UUID, UserProfileSummary> usersById = loadUsersByIds(members);
            return toTeamView(team, members, usersById);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, TeamView> findTeamsByIds(Collection<UUID> teamIds) {
        if (teamIds == null || teamIds.isEmpty()) {
            return Map.of();
        }

        List<TeamEntity> teams = teamRepository.findByIdIn(teamIds);
        if (teams.isEmpty()) {
            return Map.of();
        }

        List<UUID> foundTeamIds = teams.stream().map(TeamEntity::getId).toList();
        List<TeamMemberEntity> allMembers = teamMemberRepository.findByTeamIdIn(foundTeamIds);
        Map<UUID, UserProfileSummary> usersById = loadUsersByIds(allMembers);

        Map<UUID, List<TeamMemberEntity>> membersByTeamId = allMembers.stream()
                .collect(Collectors.groupingBy(TeamMemberEntity::getTeamId));

        return teams.stream()
                .map(team -> {
                    List<TeamMemberEntity> teamMembers = membersByTeamId.getOrDefault(team.getId(), List.of()).stream()
                            .sorted(Comparator.comparing(
                                    TeamMemberEntity::getJoinedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            ))
                            .toList();
                    return toTeamView(team, teamMembers, usersById);
                })
                .collect(Collectors.toMap(TeamView::id, Function.identity()));
    }

    private Map<UUID, UserProfileSummary> loadUsersByIds(List<TeamMemberEntity> members) {
        List<UUID> userIds = members.stream()
                .map(TeamMemberEntity::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userQueryService.findUsersByIds(userIds);
    }

    private TeamSummaryResponse toSummaryResponse(TeamEntity team, int memberCount) {
        int maxMembers = team.getDiscipline().getTeamSize();
        return new TeamSummaryResponse(
                team.getId(),
                team.getName(),
                team.getDiscipline(),
                team.getCaptainId(),
                memberCount,
                maxMembers,
                memberCount >= maxMembers
        );
    }

    private TeamMemberResponse toMemberResponse(TeamEntity team, TeamMemberEntity member, UserProfileSummary profile) {
        String displayName = profile != null ? profile.displayName() : UNKNOWN_PLAYER_NAME;
        boolean isCaptain = member.getUserId().equals(team.getCaptainId());
        String gameIdentifier = extractGameIdentifier(profile, team.getDiscipline());
        return new TeamMemberResponse(member.getUserId(), displayName, isCaptain, gameIdentifier);
    }

    private String extractGameIdentifier(UserProfileSummary profile, Discipline discipline) {
        if (profile == null) {
            return null;
        }
        Function<UserProfileSummary, String> extractor = GAME_IDENTIFIER_EXTRACTORS.get(discipline);
        return extractor != null ? extractor.apply(profile) : null;
    }

    private TeamView toTeamView(
            TeamEntity team,
            List<TeamMemberEntity> members,
            Map<UUID, UserProfileSummary> usersById) {
        List<TeamMemberView> memberViews = members.stream()
                .map(member -> toTeamMemberView(team, member, usersById.get(member.getUserId())))
                .toList();
        int memberCount = members.size();
        int maxMembers = team.getDiscipline().getTeamSize();
        return new TeamView(
                team.getId(),
                team.getName(),
                team.getDiscipline(),
                team.getCaptainId(),
                memberCount,
                maxMembers,
                memberCount >= maxMembers,
                memberViews
        );
    }

    private TeamMemberView toTeamMemberView(TeamEntity team, TeamMemberEntity member, UserProfileSummary profile) {
        String displayName = profile != null ? profile.displayName() : UNKNOWN_PLAYER_NAME;
        boolean isCaptain = member.getUserId().equals(team.getCaptainId());
        return new TeamMemberView(
                member.getUserId(),
                displayName,
                isCaptain,
                profile != null ? profile.steamId() : null,
                profile != null ? profile.supercellPlayerTag() : null,
                profile != null ? profile.chessUsername() : null
        );
    }
}

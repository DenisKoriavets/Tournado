package com.tournado.core.matches.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.api.dto.BannedMapResponse;
import com.tournado.core.matches.api.dto.MatchCredentialsResponse;
import com.tournado.core.matches.api.dto.MatchRoomResponse;
import com.tournado.core.matches.api.dto.MatchTeamStateResponse;
import com.tournado.core.matches.api.dto.ReadyResponse;
import com.tournado.core.matches.domain.MatchBanEntity;
import com.tournado.core.matches.domain.MatchCredentialsEntity;
import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.BanPhaseCompletedException;
import com.tournado.core.matches.exception.MapAlreadyBannedException;
import com.tournado.core.matches.exception.MatchAccessDeniedException;
import com.tournado.core.matches.exception.MatchNotFoundException;
import com.tournado.core.matches.exception.MatchNotInProgressException;
import com.tournado.core.matches.exception.NotYourBanTurnException;
import com.tournado.core.matches.repository.MatchBanRepository;
import com.tournado.core.matches.repository.MatchCredentialsRepository;
import com.tournado.core.matches.repository.MatchRepository;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchFlowService {

    private static final Duration READY_CHECK_WINDOW = Duration.ofMinutes(10);
    private static final Duration BAN_TURN_WINDOW = Duration.ofSeconds(30);
    private static final String ROOM_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final String FALLBACK_CS2_CONNECT = "connect 127.0.0.1:27015";

    private final MatchRepository matchRepository;
    private final MatchBanRepository matchBanRepository;
    private final MatchCredentialsRepository matchCredentialsRepository;
    private final TeamQueryService teamQueryService;
    private final TournamentQueryService tournamentQueryService;
    private final Clock clock;

    public MatchFlowService(
            MatchRepository matchRepository,
            MatchBanRepository matchBanRepository,
            MatchCredentialsRepository matchCredentialsRepository,
            TeamQueryService teamQueryService,
            TournamentQueryService tournamentQueryService,
            Clock clock) {
        this.matchRepository = matchRepository;
        this.matchBanRepository = matchBanRepository;
        this.matchCredentialsRepository = matchCredentialsRepository;
        this.teamQueryService = teamQueryService;
        this.tournamentQueryService = tournamentQueryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MatchRoomResponse getRoom(UUID matchId, UUID requestingUserId) {
        MatchEntity match = findMatchOrThrow(matchId);
        TournamentResponse tournament = findTournamentOrThrow(match.getTournamentId());
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(
                Set.of(match.getTeam1Id(), match.getTeam2Id()));
        return toRoomResponse(match, tournament, teamsById, requestingUserId);
    }

    @Transactional
    public ReadyResponse markReady(UUID matchId, UUID userId) {
        MatchEntity match = findMatchOrThrow(matchId);
        UUID captainTeamId = resolveCaptainTeamId(match, userId);
        boolean isTeam1 = captainTeamId.equals(match.getTeam1Id());

        if (match.getStatus() == MatchStatus.PENDING || match.getStatus() == MatchStatus.READY_CHECK) {
            Instant now = Instant.now(clock);
            if (match.getStatus() == MatchStatus.PENDING) {
                match.setStatus(MatchStatus.READY_CHECK);
                match.setReadyDeadlineAt(now.plus(READY_CHECK_WINDOW));
            }
            if (isTeam1) {
                match.setTeam1Ready(true);
            } else {
                match.setTeam2Ready(true);
            }
            if (match.isTeam1Ready() && match.isTeam2Ready()) {
                TournamentResponse tournament = findTournamentOrThrow(match.getTournamentId());
                startPreMatchPhase(match, tournament, now);
            }
            matchRepository.save(match);
        }

        return new ReadyResponse(isTeam1 ? match.isTeam1Ready() : match.isTeam2Ready());
    }

    @Transactional
    public MatchRoomResponse banMap(UUID matchId, UUID userId, String mapName) {
        MatchEntity match = findMatchOrThrow(matchId);
        UUID captainTeamId = resolveCaptainTeamId(match, userId);

        if (match.getStatus() != MatchStatus.PICK_BAN) {
            throw new BanPhaseCompletedException();
        }
        if (!captainTeamId.equals(match.getCurrentBanTurnTeamId())) {
            throw new NotYourBanTurnException();
        }

        TournamentResponse tournament = findTournamentOrThrow(match.getTournamentId());
        List<MatchBanEntity> existingBans = matchBanRepository.findByMatchIdOrderByBanOrderAsc(matchId);
        Set<String> bannedMapNames = existingBans.stream().map(MatchBanEntity::getMapName).collect(Collectors.toSet());

        String normalizedMapName = mapName.trim();
        if (!tournament.mapPool().contains(normalizedMapName) || bannedMapNames.contains(normalizedMapName)) {
            throw new MapAlreadyBannedException(normalizedMapName);
        }

        MatchBanEntity ban = new MatchBanEntity();
        ban.setMatchId(matchId);
        ban.setTeamId(captainTeamId);
        ban.setMapName(normalizedMapName);
        ban.setBanOrder(existingBans.size() + 1);
        matchBanRepository.save(ban);

        List<String> remainingMaps = tournament.mapPool().stream()
                .filter(map -> !bannedMapNames.contains(map) && !map.equals(normalizedMapName))
                .toList();

        if (remainingMaps.size() == 1) {
            match.setDecidedMap(remainingMaps.get(0));
            match.setStatus(MatchStatus.IN_PROGRESS);
            match.setCurrentBanTurnTeamId(null);
            match.setBanDeadlineAt(null);
            generateCredentials(match, tournament);
        } else {
            UUID otherTeamId = captainTeamId.equals(match.getTeam1Id()) ? match.getTeam2Id() : match.getTeam1Id();
            match.setCurrentBanTurnTeamId(otherTeamId);
            match.setBanDeadlineAt(Instant.now(clock).plus(BAN_TURN_WINDOW));
        }
        matchRepository.save(match);

        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.getTeam1Id(), match.getTeam2Id()));
        return toRoomResponse(match, tournament, teamsById, userId);
    }

    @Transactional(readOnly = true)
    public MatchCredentialsResponse getCredentials(UUID matchId, UUID requestingUserId) {
        MatchEntity match = findMatchOrThrow(matchId);
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.getTeam1Id(), match.getTeam2Id()));
        if (!isParticipant(teamsById, match, requestingUserId)) {
            throw new MatchAccessDeniedException();
        }

        MatchCredentialsEntity credentials = matchCredentialsRepository.findByMatchId(matchId)
                .orElseThrow(MatchNotInProgressException::new);
        TournamentResponse tournament = findTournamentOrThrow(match.getTournamentId());

        return new MatchCredentialsResponse(
                tournament.discipline(),
                credentials.getCs2ConnectString(),
                credentials.getBrawlStarsCode(),
                credentials.getChessGameUrl(),
                match.getDecidedMap()
        );
    }

    private void startPreMatchPhase(MatchEntity match, TournamentResponse tournament, Instant now) {
        if (tournament.discipline() == Discipline.CHESS) {
            match.setStatus(MatchStatus.IN_PROGRESS);
            generateCredentials(match, tournament);
            return;
        }

        match.setStatus(MatchStatus.PICK_BAN);
        UUID firstTurnTeamId = ThreadLocalRandom.current().nextBoolean() ? match.getTeam1Id() : match.getTeam2Id();
        match.setCurrentBanTurnTeamId(firstTurnTeamId);
        match.setBanDeadlineAt(now.plus(BAN_TURN_WINDOW));
    }

    private void generateCredentials(MatchEntity match, TournamentResponse tournament) {
        if (matchCredentialsRepository.findByMatchId(match.getId()).isPresent()) {
            return;
        }

        MatchCredentialsEntity credentials = new MatchCredentialsEntity();
        credentials.setMatchId(match.getId());
        switch (tournament.discipline()) {
            case CS2 -> {
                String serverConnect = tournament.cs2ServerConnect();
                credentials.setCs2ConnectString(
                        serverConnect == null || serverConnect.isBlank() ? FALLBACK_CS2_CONNECT : serverConnect.trim());
            }
            case BRAWL_STARS -> credentials.setBrawlStarsCode(generateRoomCode());
            case CHESS -> credentials.setChessGameUrl("https://www.chess.com/live/" + match.getId());
        }
        matchCredentialsRepository.save(credentials);
    }

    private String generateRoomCode() {
        StringBuilder code = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            int index = ThreadLocalRandom.current().nextInt(ROOM_CODE_ALPHABET.length());
            code.append(ROOM_CODE_ALPHABET.charAt(index));
        }
        return code.toString();
    }

    private UUID resolveCaptainTeamId(MatchEntity match, UUID userId) {
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.getTeam1Id(), match.getTeam2Id()));
        TeamView team1 = teamsById.get(match.getTeam1Id());
        TeamView team2 = teamsById.get(match.getTeam2Id());
        if (team1 != null && userId.equals(team1.captainId())) {
            return match.getTeam1Id();
        }
        if (team2 != null && userId.equals(team2.captainId())) {
            return match.getTeam2Id();
        }
        throw new OnlyCaptainAllowedException();
    }

    private boolean isParticipant(Map<UUID, TeamView> teamsById, MatchEntity match, UUID userId) {
        return isMember(teamsById.get(match.getTeam1Id()), userId) || isMember(teamsById.get(match.getTeam2Id()), userId);
    }

    private boolean isMember(TeamView team, UUID userId) {
        return team != null && team.members().stream().anyMatch(member -> member.userId().equals(userId));
    }

    private MatchRoomResponse toRoomResponse(
            MatchEntity match,
            TournamentResponse tournament,
            Map<UUID, TeamView> teamsById,
            UUID requestingUserId) {
        List<MatchBanEntity> bans = matchBanRepository.findByMatchIdOrderByBanOrderAsc(match.getId());
        List<BannedMapResponse> bannedMaps = bans.stream()
                .map(ban -> new BannedMapResponse(ban.getMapName(), ban.getTeamId(), ban.getBanOrder()))
                .toList();
        Set<String> bannedMapNames = bans.stream().map(MatchBanEntity::getMapName).collect(Collectors.toSet());
        List<String> remainingMaps = tournament.mapPool().stream()
                .filter(map -> !bannedMapNames.contains(map))
                .toList();

        MatchCredentialsResponse credentials = null;
        if (isParticipant(teamsById, match, requestingUserId)) {
            credentials = matchCredentialsRepository.findByMatchId(match.getId())
                    .map(entity -> new MatchCredentialsResponse(
                            tournament.discipline(),
                            entity.getCs2ConnectString(),
                            entity.getBrawlStarsCode(),
                            entity.getChessGameUrl(),
                            match.getDecidedMap()
                    ))
                    .orElse(null);
        }

        return new MatchRoomResponse(
                match.getId(),
                match.getStatus(),
                tournament.discipline(),
                match.getReadyDeadlineAt(),
                match.getBanDeadlineAt(),
                toTeamState(teamsById.get(match.getTeam1Id()), match.getTeam1Id(), match.isTeam1Ready()),
                toTeamState(teamsById.get(match.getTeam2Id()), match.getTeam2Id(), match.isTeam2Ready()),
                match.getCurrentBanTurnTeamId(),
                bannedMaps,
                remainingMaps,
                match.getDecidedMap(),
                credentials
        );
    }

    private MatchTeamStateResponse toTeamState(TeamView team, UUID teamId, boolean isReady) {
        String name = team != null ? team.name() : null;
        return new MatchTeamStateResponse(teamId, name, isReady);
    }

    private MatchEntity findMatchOrThrow(UUID matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    private TournamentResponse findTournamentOrThrow(UUID tournamentId) {
        return tournamentQueryService.findTournamentById(tournamentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Match references tournament '" + tournamentId + "' which no longer exists."));
    }
}

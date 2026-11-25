package com.tournado.core.bracket.service;

import com.tournado.core.bracket.api.dto.BracketMatchDto;
import com.tournado.core.bracket.api.dto.BracketResponse;
import com.tournado.core.bracket.api.dto.BracketRoundDto;
import com.tournado.core.bracket.api.dto.BracketTeamDto;
import com.tournado.core.bracket.api.dto.GenerateBracketResponse;
import com.tournado.core.bracket.api.dto.StandingItemDto;
import com.tournado.core.bracket.api.dto.StandingsResponse;
import com.tournado.core.bracket.domain.BracketEntity;
import com.tournado.core.bracket.domain.TournamentStandingEntity;
import com.tournado.core.bracket.exception.BracketAlreadyGeneratedException;
import com.tournado.core.bracket.exception.InsufficientTeamsForBracketException;
import com.tournado.core.bracket.repository.BracketRepository;
import com.tournado.core.bracket.repository.TournamentStandingRepository;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.service.BracketMatchSeed;
import com.tournado.core.matches.service.BracketMatchView;
import com.tournado.core.matches.service.MatchBracketService;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.service.TournamentBracketService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BracketService {

    private static final int MIN_TEAMS_FOR_BRACKET = 4;
    private static final Duration READY_CHECK_WINDOW = Duration.ofMinutes(10);

    private final BracketRepository bracketRepository;
    private final TournamentStandingRepository standingRepository;
    private final TournamentBracketService tournamentBracketService;
    private final TournamentQueryService tournamentQueryService;
    private final MatchBracketService matchBracketService;
    private final TeamQueryService teamQueryService;
    private final Clock clock;
    private final Random random;

    @Autowired
    public BracketService(
            BracketRepository bracketRepository,
            TournamentStandingRepository standingRepository,
            TournamentBracketService tournamentBracketService,
            TournamentQueryService tournamentQueryService,
            MatchBracketService matchBracketService,
            TeamQueryService teamQueryService,
            Clock clock) {
        this(
                bracketRepository,
                standingRepository,
                tournamentBracketService,
                tournamentQueryService,
                matchBracketService,
                teamQueryService,
                clock,
                new Random()
        );
    }

    public BracketService(
            BracketRepository bracketRepository,
            TournamentStandingRepository standingRepository,
            TournamentBracketService tournamentBracketService,
            TournamentQueryService tournamentQueryService,
            MatchBracketService matchBracketService,
            TeamQueryService teamQueryService,
            Clock clock,
            Random random) {
        this.bracketRepository = bracketRepository;
        this.standingRepository = standingRepository;
        this.tournamentBracketService = tournamentBracketService;
        this.tournamentQueryService = tournamentQueryService;
        this.matchBracketService = matchBracketService;
        this.teamQueryService = teamQueryService;
        this.clock = clock;
        this.random = random;
    }

    @Transactional
    public GenerateBracketResponse generateBracket(UUID tournamentId, UUID actorUserId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(actorUserId, "actorUserId must not be null");

        if (bracketRepository.existsByTournamentId(tournamentId)) {
            throw new BracketAlreadyGeneratedException(tournamentId);
        }

        List<UUID> checkedInTeamIds = tournamentBracketService.prepareAndStartTournament(tournamentId, actorUserId);
        int activeTeamsCount = checkedInTeamIds.size();
        if (activeTeamsCount < MIN_TEAMS_FOR_BRACKET) {
            throw new InsufficientTeamsForBracketException(activeTeamsCount);
        }

        List<UUID> shuffledTeams = new ArrayList<>(checkedInTeamIds);
        Collections.shuffle(shuffledTeams, random);

        int bracketSize = nextPowerOfTwo(activeTeamsCount);
        int totalRounds = Integer.numberOfTrailingZeros(bracketSize);
        int firstRoundMatchesCount = bracketSize / 2;
        int byesAssignedCount = bracketSize - activeTeamsCount;

        Instant now = Instant.now(clock);
        Instant readyDeadline = now.plus(READY_CHECK_WINDOW);

        BracketEntity bracket = new BracketEntity();
        bracket.setId(UUID.randomUUID());
        bracket.setTournamentId(tournamentId);
        bracket.setType(TournamentFormat.SINGLE_ELIMINATION);
        bracket.setTotalRounds(totalRounds);
        bracket.setGeneratedAt(now);
        BracketEntity savedBracket = bracketRepository.save(bracket);

        List<Integer> seedOrder = buildStandardSeedOrder(bracketSize);
        Map<String, UUID> round2PrePopulatedSlots = new HashMap<>();
        List<BracketMatchSeed> allMatches = new ArrayList<>(bracketSize - 1);

        // Round 1: pair seeds according to standard tournament seeding and auto-advance BYEs
        for (int matchOrder = 1; matchOrder <= firstRoundMatchesCount; matchOrder++) {
            int seed1 = seedOrder.get((matchOrder - 1) * 2);
            int seed2 = seedOrder.get((matchOrder - 1) * 2 + 1);

            UUID team1Id = seed1 <= activeTeamsCount ? shuffledTeams.get(seed1 - 1) : null;
            UUID team2Id = seed2 <= activeTeamsCount ? shuffledTeams.get(seed2 - 1) : null;

            boolean isBye = (team1Id == null) ^ (team2Id == null);
            UUID winnerTeamId = isBye ? (team1Id != null ? team1Id : team2Id) : null;
            MatchStatus status = isBye ? MatchStatus.COMPLETED : MatchStatus.READY_CHECK;
            Instant matchReadyDeadline = isBye ? null : readyDeadline;

            allMatches.add(new BracketMatchSeed(
                    UUID.randomUUID(),
                    tournamentId,
                    savedBracket.getId(),
                    1,
                    matchOrder,
                    team1Id,
                    team2Id,
                    winnerTeamId,
                    status,
                    isBye,
                    matchReadyDeadline
            ));

            if (isBye && totalRounds >= 2) {
                int nextMatchOrder = (matchOrder + 1) / 2;
                boolean isNextTeam1Slot = (matchOrder % 2) == 1;
                round2PrePopulatedSlots.put(slotKey(2, nextMatchOrder, isNextTeam1Slot), winnerTeamId);
            }
        }

        // Rounds 2..totalRounds
        for (int round = 2; round <= totalRounds; round++) {
            int matchesInRound = bracketSize >> round;
            for (int matchOrder = 1; matchOrder <= matchesInRound; matchOrder++) {
                UUID team1Id = round2PrePopulatedSlots.get(slotKey(round, matchOrder, true));
                UUID team2Id = round2PrePopulatedSlots.get(slotKey(round, matchOrder, false));

                boolean bothReadyToStart = team1Id != null && team2Id != null;
                MatchStatus status = bothReadyToStart ? MatchStatus.READY_CHECK : MatchStatus.PENDING;
                Instant matchReadyDeadline = bothReadyToStart ? readyDeadline : null;

                allMatches.add(new BracketMatchSeed(
                        UUID.randomUUID(),
                        tournamentId,
                        savedBracket.getId(),
                        round,
                        matchOrder,
                        team1Id,
                        team2Id,
                        null,
                        status,
                        false,
                        matchReadyDeadline
                ));
            }
        }

        matchBracketService.createBracketMatches(allMatches);

        return new GenerateBracketResponse(
                savedBracket.getId(),
                totalRounds,
                activeTeamsCount,
                byesAssignedCount,
                firstRoundMatchesCount
        );
    }

    @Transactional(readOnly = true)
    public BracketResponse getBracket(UUID tournamentId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        TournamentResponse tournament = tournamentQueryService.findTournamentById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        return bracketRepository.findByTournamentId(tournamentId)
                .map(bracket -> buildPopulatedBracketResponse(tournament, bracket))
                .orElseGet(() -> new BracketResponse(tournament.id(), tournament.status(), List.of()));
    }

    @Transactional
    public void advanceWinnerOnMatchCompleted(
            UUID tournamentId,
            int roundNumber,
            int matchOrder,
            UUID winnerTeamId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        Objects.requireNonNull(winnerTeamId, "winnerTeamId must not be null");

        BracketEntity bracket = bracketRepository.findByTournamentId(tournamentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Bracket not found for tournament '" + tournamentId + "'."
                ));

        if (roundNumber < bracket.getTotalRounds()) {
            int nextRound = roundNumber + 1;
            int nextMatchOrder = (matchOrder + 1) / 2;
            boolean isTeam1Slot = (matchOrder % 2) == 1;
            Instant readyDeadline = Instant.now(clock).plus(READY_CHECK_WINDOW);

            matchBracketService.advanceTeamToSlot(
                    tournamentId,
                    nextRound,
                    nextMatchOrder,
                    isTeam1Slot,
                    winnerTeamId,
                    readyDeadline
            );
            return;
        }

        // Final match completed -> compute 1st, 2nd, and 3rd places and complete tournament
        finalizeTournamentStandings(tournamentId, bracket.getTotalRounds(), winnerTeamId);
        tournamentBracketService.completeTournament(tournamentId);
    }

    @Transactional(readOnly = true)
    public StandingsResponse getStandings(UUID tournamentId) {
        Objects.requireNonNull(tournamentId, "tournamentId must not be null");
        tournamentQueryService.findTournamentById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));

        List<TournamentStandingEntity> standings = standingRepository.findByTournamentIdOrderByRankPlaceAsc(tournamentId);
        if (standings.isEmpty()) {
            return new StandingsResponse(tournamentId, List.of());
        }

        List<UUID> teamIds = standings.stream()
                .map(TournamentStandingEntity::getTeamId)
                .toList();
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(teamIds);

        List<StandingItemDto> items = standings.stream()
                .map(standing -> {
                    TeamView team = teamsById.get(standing.getTeamId());
                    String teamName = team != null ? team.name() : "Unknown Team";
                    return new StandingItemDto(standing.getRankPlace(), teamName);
                })
                .toList();

        return new StandingsResponse(tournamentId, items);
    }

    private void finalizeTournamentStandings(UUID tournamentId, int totalRounds, UUID championTeamId) {
        if (standingRepository.existsByTournamentId(tournamentId)) {
            return;
        }

        List<BracketMatchView> matches = matchBracketService.findMatchesByTournamentId(tournamentId);
        BracketMatchView finalMatch = matches.stream()
                .filter(m -> m.roundNumber() == totalRounds && m.matchOrder() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Final match not found for tournament " + tournamentId));

        UUID runnerUpTeamId = championTeamId.equals(finalMatch.team1Id())
                ? finalMatch.team2Id()
                : finalMatch.team1Id();

        List<UUID> semifinalLosers = matches.stream()
                .filter(m -> m.roundNumber() == totalRounds - 1)
                .sorted(Comparator.comparingInt(BracketMatchView::matchOrder))
                .map(m -> resolveLoserTeamId(m))
                .filter(Objects::nonNull)
                .toList();

        Instant now = Instant.now(clock);
        List<TournamentStandingEntity> entries = new ArrayList<>();
        entries.add(buildStandingEntity(tournamentId, championTeamId, 1, now));
        if (runnerUpTeamId != null) {
            entries.add(buildStandingEntity(tournamentId, runnerUpTeamId, 2, now));
        }
        if (!semifinalLosers.isEmpty()) {
            entries.add(buildStandingEntity(tournamentId, semifinalLosers.get(0), 3, now));
        }

        standingRepository.saveAll(entries);
    }

    private UUID resolveLoserTeamId(BracketMatchView match) {
        if (match.winnerTeamId() == null) {
            return null;
        }
        return match.winnerTeamId().equals(match.team1Id()) ? match.team2Id() : match.team1Id();
    }

    private TournamentStandingEntity buildStandingEntity(
            UUID tournamentId,
            UUID teamId,
            int rankPlace,
            Instant createdAt) {
        TournamentStandingEntity entity = new TournamentStandingEntity();
        entity.setId(UUID.randomUUID());
        entity.setTournamentId(tournamentId);
        entity.setTeamId(teamId);
        entity.setRankPlace(rankPlace);
        entity.setCreatedAt(createdAt);
        return entity;
    }

    private BracketResponse buildPopulatedBracketResponse(TournamentResponse tournament, BracketEntity bracket) {
        List<BracketMatchView> matches = matchBracketService.findMatchesByTournamentId(tournament.id());

        Set<UUID> allTeamIds = new HashSet<>();
        for (BracketMatchView match : matches) {
            if (match.team1Id() != null) {
                allTeamIds.add(match.team1Id());
            }
            if (match.team2Id() != null) {
                allTeamIds.add(match.team2Id());
            }
        }
        Map<UUID, TeamView> teamsById = allTeamIds.isEmpty()
                ? Map.of()
                : teamQueryService.findTeamsByIds(allTeamIds);

        Map<Integer, List<BracketMatchDto>> matchesByRound = new LinkedHashMap<>();
        for (int round = 1; round <= bracket.getTotalRounds(); round++) {
            matchesByRound.put(round, new ArrayList<>());
        }

        for (BracketMatchView match : matches) {
            matchesByRound.computeIfAbsent(match.roundNumber(), r -> new ArrayList<>())
                    .add(new BracketMatchDto(
                            match.matchId(),
                            match.matchOrder(),
                            match.status(),
                            toTeamDto(match.team1Id(), teamsById),
                            toTeamDto(match.team2Id(), teamsById),
                            match.winnerTeamId(),
                            match.isBye()
                    ));
        }

        List<BracketRoundDto> rounds = matchesByRound.entrySet().stream()
                .map(entry -> new BracketRoundDto(
                        entry.getKey(),
                        resolveRoundName(bracket.getTotalRounds(), entry.getKey()),
                        entry.getValue()
                ))
                .toList();

        return new BracketResponse(tournament.id(), tournament.status(), rounds);
    }

    private BracketTeamDto toTeamDto(UUID teamId, Map<UUID, TeamView> teamsById) {
        if (teamId == null) {
            return null;
        }
        TeamView team = teamsById.get(teamId);
        String name = team != null ? team.name() : "Unknown Team";
        return new BracketTeamDto(teamId, name);
    }

    static String resolveRoundName(int totalRounds, int roundNumber) {
        int remainingRounds = totalRounds - roundNumber;
        return switch (remainingRounds) {
            case 0 -> "Final";
            case 1 -> "Semifinals";
            case 2 -> "Quarterfinals";
            default -> "Round of " + (1 << (remainingRounds + 1));
        };
    }

    static int nextPowerOfTwo(int n) {
        int power = 1;
        while (power < n) {
            power <<= 1;
        }
        return power;
    }

    static List<Integer> buildStandardSeedOrder(int bracketSize) {
        List<Integer> seeds = List.of(1, 2);
        int currentSize = 2;
        while (currentSize < bracketSize) {
            int nextSize = currentSize * 2;
            List<Integer> nextSeeds = new ArrayList<>(nextSize);
            for (int seed : seeds) {
                nextSeeds.add(seed);
                nextSeeds.add(nextSize + 1 - seed);
            }
            seeds = nextSeeds;
            currentSize = nextSize;
        }
        return seeds;
    }

    private static String slotKey(int round, int matchOrder, boolean isTeam1) {
        return round + ":" + matchOrder + ":" + (isTeam1 ? "1" : "2");
    }
}
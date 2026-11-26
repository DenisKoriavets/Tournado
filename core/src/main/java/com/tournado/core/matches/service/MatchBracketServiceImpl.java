package com.tournado.core.matches.service;

import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.repository.MatchRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchBracketServiceImpl implements MatchBracketService {

    private final MatchRepository matchRepository;
    private final Clock clock;

    public MatchBracketServiceImpl(MatchRepository matchRepository, Clock clock) {
        this.matchRepository = matchRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void createBracketMatches(List<BracketMatchSeed> seeds) {
        Instant now = Instant.now(clock);
        List<MatchEntity> entities = seeds.stream()
                .map(seed -> toEntity(seed, now))
                .toList();
        matchRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketMatchView> findMatchesByTournamentId(UUID tournamentId) {
        return matchRepository.findByTournamentIdOrderByRoundNumberAscMatchOrderAsc(tournamentId).stream()
                .map(this::toBracketMatchView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BracketMatchView> findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .map(this::toBracketMatchView);
    }

    @Override
    @Transactional
    public void advanceTeamToSlot(
            UUID tournamentId,
            int targetRoundNumber,
            int targetMatchOrder,
            boolean isTeam1Slot,
            UUID advancingTeamId,
            Instant readyDeadlineAt) {
        MatchEntity targetMatch = matchRepository
                .findByTournamentIdAndRoundNumberAndMatchOrder(tournamentId, targetRoundNumber, targetMatchOrder)
                .orElseThrow(() -> new IllegalStateException(
                        "Target bracket match not found: tournament=" + tournamentId
                                + ", round=" + targetRoundNumber + ", order=" + targetMatchOrder
                ));

        if (isTeam1Slot) {
            targetMatch.setTeam1Id(advancingTeamId);
        } else {
            targetMatch.setTeam2Id(advancingTeamId);
        }

        if (targetMatch.getTeam1Id() != null
                && targetMatch.getTeam2Id() != null
                && targetMatch.getStatus() == MatchStatus.PENDING) {
            targetMatch.setStatus(MatchStatus.READY_CHECK);
            targetMatch.setReadyDeadlineAt(readyDeadlineAt);
        }

        matchRepository.save(targetMatch);
    }

    private BracketMatchView toBracketMatchView(MatchEntity entity) {
        return new BracketMatchView(
                entity.getId(),
                entity.getRoundNumber(),
                entity.getMatchOrder(),
                entity.getTeam1Id(),
                entity.getTeam2Id(),
                entity.getWinnerTeamId(),
                entity.getStatus(),
                entity.isBye()
        );
    }

    private MatchEntity toEntity(BracketMatchSeed seed, Instant now) {
        MatchEntity entity = new MatchEntity();
        entity.setId(seed.matchId() != null ? seed.matchId() : UUID.randomUUID());
        entity.setTournamentId(seed.tournamentId());
        entity.setBracketId(seed.bracketId());
        entity.setRoundNumber(seed.roundNumber());
        entity.setMatchOrder(seed.matchOrder());
        entity.setTeam1Id(seed.team1Id());
        entity.setTeam2Id(seed.team2Id());
        entity.setWinnerTeamId(seed.winnerTeamId());
        entity.setStatus(seed.status());
        entity.setBye(seed.isBye());
        entity.setReadyDeadlineAt(seed.readyDeadlineAt());
        entity.setTeam1Ready(false);
        entity.setTeam2Ready(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setVersion(0L);
        return entity;
    }
}
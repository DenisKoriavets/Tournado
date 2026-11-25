package com.tournado.core.matches.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.api.dto.SubmitScoreResponse;
import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.domain.ScoreSubmissionEntity;
import com.tournado.core.matches.exception.MatchNotFoundException;
import com.tournado.core.matches.exception.MatchNotInProgressException;
import com.tournado.core.matches.exception.ScoreAlreadySubmittedException;
import com.tournado.core.matches.exception.ScoreEntryNotAvailableException;
import com.tournado.core.matches.exception.ScoreTieNotAllowedException;
import com.tournado.core.matches.repository.MatchRepository;
import com.tournado.core.matches.repository.ScoreSubmissionRepository;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Match results: captain score entry (US-27), automatic confirmation and bracket advancement (US-28),
 * and the dispute hooks used by the disputes module (US-30, US-34).
 */
@Service
public class MatchResultService implements MatchQueryService, MatchOutcomeService {

    private final MatchRepository matchRepository;
    private final ScoreSubmissionRepository scoreSubmissionRepository;
    private final TeamQueryService teamQueryService;
    private final TournamentQueryService tournamentQueryService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MatchResultService(
            MatchRepository matchRepository,
            ScoreSubmissionRepository scoreSubmissionRepository,
            TeamQueryService teamQueryService,
            TournamentQueryService tournamentQueryService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.matchRepository = matchRepository;
        this.scoreSubmissionRepository = scoreSubmissionRepository;
        this.teamQueryService = teamQueryService;
        this.tournamentQueryService = tournamentQueryService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public SubmitScoreResponse submitScore(UUID matchId, UUID userId, int team1Score, int team2Score) {
        MatchEntity match = matchRepository.findByIdForUpdate(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        UUID captainTeamId = resolveCaptainTeamId(match, userId);

        TournamentResponse tournament = tournamentQueryService.findTournamentById(match.getTournamentId())
                .orElseThrow(() -> new IllegalStateException(
                        "Match references tournament '" + match.getTournamentId() + "' which no longer exists."));
        if (tournament.discipline() == Discipline.CHESS) {
            throw new ScoreEntryNotAvailableException();
        }
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new MatchNotInProgressException(
                    "Scores can only be submitted while the match is in progress (current status: " + match.getStatus() + ").");
        }
        if (team1Score == team2Score) {
            throw new ScoreTieNotAllowedException();
        }

        List<ScoreSubmissionEntity> existing = scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(matchId);
        if (existing.stream().anyMatch(submission -> submission.getTeamId().equals(captainTeamId))) {
            throw new ScoreAlreadySubmittedException();
        }

        ScoreSubmissionEntity submission = new ScoreSubmissionEntity();
        submission.setId(UUID.randomUUID());
        submission.setMatchId(matchId);
        submission.setTeamId(captainTeamId);
        submission.setSubmittedByUserId(userId);
        submission.setTeam1Score(team1Score);
        submission.setTeam2Score(team2Score);
        submission.setSubmittedAt(Instant.now(clock));
        scoreSubmissionRepository.save(submission);

        Optional<ScoreSubmissionEntity> opponentSubmission = existing.stream()
                .filter(other -> !other.getTeamId().equals(captainTeamId))
                .findFirst();
        if (opponentSubmission.isEmpty()) {
            return new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, false, false);
        }
        if (!opponentSubmission.get().hasSameScoreAs(submission)) {
            return new SubmitScoreResponse(MatchStatus.IN_PROGRESS, null, true, false);
        }

        UUID winnerTeamId = team1Score > team2Score ? match.getTeam1Id() : match.getTeam2Id();
        closeAndAdvance(match, winnerTeamId);
        return new SubmitScoreResponse(MatchStatus.COMPLETED, winnerTeamId, true, true);
    }

    @Override
    @Transactional
    public void markDisputed(UUID matchId) {
        MatchEntity match = matchRepository.findByIdForUpdate(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new MatchNotInProgressException(
                    "A dispute can only be opened while the match is in progress (current status: " + match.getStatus() + ").");
        }
        match.setStatus(MatchStatus.DISPUTED);
        matchRepository.save(match);
    }

    @Override
    @Transactional
    public MatchView completeMatch(UUID matchId, UUID winnerTeamId) {
        MatchEntity match = matchRepository.findByIdForUpdate(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        if (match.getStatus() != MatchStatus.IN_PROGRESS && match.getStatus() != MatchStatus.DISPUTED) {
            throw new MatchNotInProgressException(
                    "Only an in-progress or disputed match can be completed (current status: " + match.getStatus() + ").");
        }
        if (!winnerTeamId.equals(match.getTeam1Id()) && !winnerTeamId.equals(match.getTeam2Id())) {
            throw new IllegalArgumentException("Winner '" + winnerTeamId + "' does not play in match '" + matchId + "'.");
        }
        closeAndAdvance(match, winnerTeamId);
        return toView(match, scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(matchId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchView> findMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .map(match -> toView(match, scoreSubmissionRepository.findByMatchIdOrderBySubmittedAtAsc(matchId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchView> findMatchesByTournamentId(UUID tournamentId) {
        List<MatchEntity> matches = matchRepository.findByTournamentIdOrderByRoundNumberAscMatchOrderAsc(tournamentId);
        if (matches.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ScoreSubmissionEntity>> submissionsByMatch = scoreSubmissionRepository
                .findByMatchIdIn(matches.stream().map(MatchEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(ScoreSubmissionEntity::getMatchId));
        return matches.stream()
                .map(match -> toView(match, submissionsByMatch.getOrDefault(match.getId(), List.of())))
                .toList();
    }

    /**
     * Single Elimination advancement: the winner of (round r, order k) takes slot team1 (odd k) or team2 (even k)
     * of match (round r + 1, order ceil(k / 2)). No next match means this was the final.
     */
    private void closeAndAdvance(MatchEntity match, UUID winnerTeamId) {
        match.setStatus(MatchStatus.COMPLETED);
        match.setWinnerTeamId(winnerTeamId);
        matchRepository.save(match);

        Optional<MatchEntity> nextMatch = matchRepository.findBracketSlotForUpdate(
                match.getTournamentId(), match.getRoundNumber() + 1, (match.getMatchOrder() + 1) / 2);
        nextMatch.ifPresent(next -> {
            if (match.getMatchOrder() % 2 == 1) {
                next.setTeam1Id(winnerTeamId);
            } else {
                next.setTeam2Id(winnerTeamId);
            }
            matchRepository.save(next);
        });

        eventPublisher.publishEvent(new MatchCompletedEvent(
                match.getId(), match.getTournamentId(), winnerTeamId, nextMatch.isEmpty()));
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

    private MatchView toView(MatchEntity match, List<ScoreSubmissionEntity> submissions) {
        List<ScoreSubmissionView> submissionViews = submissions.stream()
                .sorted(Comparator.comparing(ScoreSubmissionEntity::getSubmittedAt))
                .map(submission -> new ScoreSubmissionView(
                        submission.getTeamId(),
                        submission.getTeam1Score(),
                        submission.getTeam2Score(),
                        submission.getSubmittedAt()))
                .toList();
        return new MatchView(
                match.getId(),
                match.getTournamentId(),
                match.getRoundNumber(),
                match.getMatchOrder(),
                match.getTeam1Id(),
                match.getTeam2Id(),
                match.getWinnerTeamId(),
                match.getStatus(),
                submissionViews
        );
    }
}

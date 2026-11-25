package com.tournado.core.disputes.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.disputes.api.dto.AiAnalysisResponse;
import com.tournado.core.disputes.api.dto.DisputeCardResponse;
import com.tournado.core.disputes.api.dto.DisputeResolutionResponse;
import com.tournado.core.disputes.api.dto.DisputeSummaryResponse;
import com.tournado.core.disputes.api.dto.DisputeTeamResponse;
import com.tournado.core.disputes.api.dto.NicknameCheckResponse;
import com.tournado.core.disputes.api.dto.OpenDisputeResponse;
import com.tournado.core.disputes.api.dto.ResolveDisputeRequest;
import com.tournado.core.disputes.api.dto.ResolveDisputeResponse;
import com.tournado.core.disputes.api.dto.SubmittedScoreResponse;
import com.tournado.core.disputes.api.dto.TournamentDisputesResponse;
import com.tournado.core.disputes.domain.AiAnalysisResultEntity;
import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeEntity;
import com.tournado.core.disputes.domain.DisputeResolutionSource;
import com.tournado.core.disputes.domain.DisputeStatus;
import com.tournado.core.disputes.domain.StoredNicknameCheck;
import com.tournado.core.disputes.exception.AiSuggestionUnavailableException;
import com.tournado.core.disputes.exception.DisputeAccessDeniedException;
import com.tournado.core.disputes.exception.DisputeAlreadyOpenException;
import com.tournado.core.disputes.exception.DisputeAlreadyResolvedException;
import com.tournado.core.disputes.exception.DisputeNotAllowedException;
import com.tournado.core.disputes.exception.DisputeNotFoundException;
import com.tournado.core.disputes.exception.ResolutionScoreRequiredException;
import com.tournado.core.disputes.infra.ScreenshotStorage;
import com.tournado.core.disputes.repository.AiAnalysisResultRepository;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.MatchNotFoundException;
import com.tournado.core.matches.exception.ScoreTieNotAllowedException;
import com.tournado.core.matches.service.MatchOutcomeService;
import com.tournado.core.matches.service.MatchQueryService;
import com.tournado.core.matches.service.MatchView;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.exception.OnlyOrganizerAllowedException;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.service.TournamentParticipantsQueryService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dispute lifecycle (US-30..US-34). The AI verdict is advisory only: nothing here changes a match or the
 * bracket except an explicit organizer decision in {@link #resolveDispute}.
 */
@Service
public class DisputeService {

    private static final String TEAM1_SLOT = "TEAM1";
    private static final String TEAM2_SLOT = "TEAM2";

    private final DisputeRepository disputeRepository;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;
    private final MatchQueryService matchQueryService;
    private final MatchOutcomeService matchOutcomeService;
    private final TeamQueryService teamQueryService;
    private final TournamentQueryService tournamentQueryService;
    private final TournamentParticipantsQueryService tournamentParticipantsQueryService;
    private final ScreenshotValidator screenshotValidator;
    private final ScreenshotStorage screenshotStorage;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DisputeService(
            DisputeRepository disputeRepository,
            AiAnalysisResultRepository aiAnalysisResultRepository,
            MatchQueryService matchQueryService,
            MatchOutcomeService matchOutcomeService,
            TeamQueryService teamQueryService,
            TournamentQueryService tournamentQueryService,
            TournamentParticipantsQueryService tournamentParticipantsQueryService,
            ScreenshotValidator screenshotValidator,
            ScreenshotStorage screenshotStorage,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.disputeRepository = disputeRepository;
        this.aiAnalysisResultRepository = aiAnalysisResultRepository;
        this.matchQueryService = matchQueryService;
        this.matchOutcomeService = matchOutcomeService;
        this.teamQueryService = teamQueryService;
        this.tournamentQueryService = tournamentQueryService;
        this.tournamentParticipantsQueryService = tournamentParticipantsQueryService;
        this.screenshotValidator = screenshotValidator;
        this.screenshotStorage = screenshotStorage;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * US-30 / BR-45, BR-46.
     */
    @Transactional
    public OpenDisputeResponse openDispute(UUID matchId, UUID userId, MultipartFile screenshot) {
        MatchView match = matchQueryService.findMatchById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.team1Id(), match.team2Id()));
        UUID openerTeamId = resolveCaptainTeamId(match, teamsById, userId);

        TournamentResponse tournament = findTournament(match.tournamentId());
        if (tournament.discipline() == Discipline.CHESS) {
            throw new DisputeNotAllowedException("Disputes are only available for CS2 and Brawl Stars matches.");
        }
        if (match.status() == MatchStatus.DISPUTED || disputeRepository.existsByMatchIdAndStatus(matchId, DisputeStatus.OPEN)) {
            throw new DisputeAlreadyOpenException();
        }
        if (match.status() != MatchStatus.IN_PROGRESS) {
            throw new DisputeNotAllowedException(
                    "A dispute can only be opened while the match is in progress (current status: " + match.status() + ").");
        }
        if (match.scoreSubmissions().isEmpty()) {
            throw new DisputeNotAllowedException(
                    "A dispute can be opened only after a score was reported and the captains' scores do not match "
                            + "or the opponent has not confirmed it.");
        }

        ValidatedScreenshot validated = screenshotValidator.validate(screenshot);
        UUID disputeId = UUID.randomUUID();
        String relativePath = "disputes/" + disputeId + "." + validated.format().getExtension();

        DisputeEntity dispute = new DisputeEntity();
        dispute.setId(disputeId);
        dispute.setMatchId(matchId);
        dispute.setOpenedByTeamId(openerTeamId);
        dispute.setOpenedByUserId(userId);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setScreenshotFilePath(relativePath);
        dispute.setScreenshotMimeType(validated.format().getMimeType());
        dispute.setScreenshotFileSizeBytes(validated.content().length);
        dispute.setCreatedAt(Instant.now(clock));
        try {
            disputeRepository.saveAndFlush(dispute);
        } catch (DataIntegrityViolationException ex) {
            throw new DisputeAlreadyOpenException();
        }

        matchOutcomeService.markDisputed(matchId);
        screenshotStorage.store(relativePath, validated.content());
        eventPublisher.publishEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_OPENED, disputeId, match.tournamentId(), matchId));

        return new OpenDisputeResponse(disputeId, matchId, DisputeStatus.OPEN, screenshotUrl(disputeId), AiAnalysisStatus.PENDING);
    }

    /**
     * US-31: organizer dashboard list (open disputes first in the counter, newest first in the list).
     */
    @Transactional(readOnly = true)
    public TournamentDisputesResponse listTournamentDisputes(UUID tournamentId, UUID userId) {
        requireOrganizer(tournamentId, userId);

        Map<UUID, MatchView> matchesById = matchQueryService.findMatchesByTournamentId(tournamentId).stream()
                .collect(Collectors.toMap(MatchView::id, Function.identity()));
        if (matchesById.isEmpty()) {
            return new TournamentDisputesResponse(tournamentId, 0, List.of());
        }
        List<DisputeEntity> disputes = disputeRepository.findByMatchIdInOrderByCreatedAtDesc(matchesById.keySet());
        Map<UUID, AiAnalysisResultEntity> analysesByDispute = aiAnalysisResultRepository
                .findByDisputeIdIn(disputes.stream().map(DisputeEntity::getId).toList()).stream()
                .collect(Collectors.toMap(AiAnalysisResultEntity::getDisputeId, Function.identity()));
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(teamIdsOf(
                disputes.stream().map(dispute -> matchesById.get(dispute.getMatchId())).toList()));

        List<DisputeSummaryResponse> summaries = disputes.stream()
                .map(dispute -> toSummary(dispute, matchesById.get(dispute.getMatchId()),
                        analysesByDispute.get(dispute.getId()), teamsById))
                .toList();
        long openCount = disputes.stream().filter(dispute -> dispute.getStatus() == DisputeStatus.OPEN).count();
        return new TournamentDisputesResponse(tournamentId, openCount, summaries);
    }

    /**
     * US-32 / US-33: dispute card with the AI hint and the roster comparison.
     */
    @Transactional(readOnly = true)
    public DisputeCardResponse getDisputeCard(UUID disputeId, UUID userId) {
        DisputeEntity dispute = findDispute(disputeId);
        MatchView match = findMatch(dispute.getMatchId());
        requireOrganizer(match.tournamentId(), userId);

        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.team1Id(), match.team2Id()));
        AiAnalysisResultEntity analysis = aiAnalysisResultRepository.findByDisputeId(disputeId).orElse(null);
        return toCard(dispute, match, analysis, teamsById);
    }

    @Transactional(readOnly = true)
    public ScreenshotContent getScreenshot(UUID disputeId, UUID userId) {
        DisputeEntity dispute = findDispute(disputeId);
        MatchView match = findMatch(dispute.getMatchId());
        boolean isOrganizer = tournamentParticipantsQueryService.findOrganizerId(match.tournamentId())
                .map(userId::equals)
                .orElse(false);
        if (!isOrganizer && !isParticipant(match, userId)) {
            throw new DisputeAccessDeniedException();
        }
        return new ScreenshotContent(screenshotStorage.load(dispute.getScreenshotFilePath()), dispute.getScreenshotMimeType());
    }

    /**
     * US-34 / BR-50: final organizer ruling - approve the AI hint in one click or enter the score manually.
     */
    @Transactional
    public ResolveDisputeResponse resolveDispute(UUID disputeId, UUID userId, ResolveDisputeRequest request) {
        DisputeEntity dispute = disputeRepository.findByIdForUpdate(disputeId)
                .orElseThrow(() -> new DisputeNotFoundException(disputeId));
        MatchView match = findMatch(dispute.getMatchId());
        requireOrganizer(match.tournamentId(), userId);
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new DisputeAlreadyResolvedException();
        }

        int team1Score;
        int team2Score;
        if (request.decision() == DisputeResolutionSource.AI_SUGGESTION) {
            AiAnalysisResultEntity analysis = aiAnalysisResultRepository.findByDisputeId(disputeId)
                    .filter(AiAnalysisResultEntity::hasUsableSuggestion)
                    .orElseThrow(AiSuggestionUnavailableException::new);
            team1Score = analysis.getRecognizedTeam1Score();
            team2Score = analysis.getRecognizedTeam2Score();
        } else {
            if (request.team1Score() == null || request.team2Score() == null) {
                throw new ResolutionScoreRequiredException();
            }
            team1Score = request.team1Score();
            team2Score = request.team2Score();
        }
        if (team1Score == team2Score) {
            throw new ScoreTieNotAllowedException();
        }

        UUID winnerTeamId = team1Score > team2Score ? match.team1Id() : match.team2Id();
        Instant now = Instant.now(clock);
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedByUserId(userId);
        dispute.setResolvedAt(now);
        dispute.setResolutionWinnerTeamId(winnerTeamId);
        dispute.setResolutionTeam1Score(team1Score);
        dispute.setResolutionTeam2Score(team2Score);
        dispute.setResolutionSource(request.decision());
        dispute.setResolutionNotes(request.resolutionNotes() == null ? null : request.resolutionNotes().strip());
        disputeRepository.save(dispute);

        MatchView completed = matchOutcomeService.completeMatch(match.id(), winnerTeamId);
        eventPublisher.publishEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_RESOLVED, disputeId, match.tournamentId(), match.id()));

        return new ResolveDisputeResponse(
                disputeId,
                DisputeStatus.RESOLVED,
                match.id(),
                completed.status(),
                winnerTeamId,
                team1Score,
                team2Score,
                request.decision(),
                now
        );
    }

    private DisputeCardResponse toCard(
            DisputeEntity dispute,
            MatchView match,
            AiAnalysisResultEntity analysis,
            Map<UUID, TeamView> teamsById) {
        List<SubmittedScoreResponse> submittedScores = match.scoreSubmissions().stream()
                .map(submission -> new SubmittedScoreResponse(
                        submission.teamId(), submission.team1Score(), submission.team2Score(), submission.submittedAt()))
                .toList();
        boolean canApprove = dispute.getStatus() == DisputeStatus.OPEN
                && analysis != null
                && analysis.hasUsableSuggestion()
                && !analysis.getRecognizedTeam1Score().equals(analysis.getRecognizedTeam2Score());
        DisputeResolutionResponse resolution = dispute.getStatus() == DisputeStatus.RESOLVED
                ? new DisputeResolutionResponse(
                        dispute.getResolutionSource(),
                        dispute.getResolutionWinnerTeamId(),
                        dispute.getResolutionTeam1Score(),
                        dispute.getResolutionTeam2Score(),
                        dispute.getResolutionNotes(),
                        dispute.getResolvedByUserId(),
                        dispute.getResolvedAt())
                : null;

        return new DisputeCardResponse(
                dispute.getId(),
                match.id(),
                match.tournamentId(),
                dispute.getStatus(),
                screenshotUrl(dispute.getId()),
                match.roundNumber(),
                match.matchOrder(),
                toTeam(match.team1Id(), teamsById),
                toTeam(match.team2Id(), teamsById),
                dispute.getOpenedByTeamId(),
                submittedScores,
                analysis == null ? AiAnalysisStatus.PENDING : analysis.getStatus(),
                analysis == null ? null : toAnalysis(analysis, match),
                canApprove,
                resolution,
                dispute.getCreatedAt()
        );
    }

    private AiAnalysisResponse toAnalysis(AiAnalysisResultEntity analysis, MatchView match) {
        List<StoredNicknameCheck> checks = analysis.getNicknameChecks() == null ? List.of() : analysis.getNicknameChecks();
        return new AiAnalysisResponse(
                analysis.getStatus(),
                analysis.getRecognizedTeam1Score(),
                analysis.getRecognizedTeam2Score(),
                analysis.getConfidence(),
                analysis.isNicknamesMatchedAll(),
                checks.stream().map(StoredNicknameCheck::nickname).toList(),
                checks.stream()
                        .map(check -> new NicknameCheckResponse(
                                check.nickname(),
                                slotToTeamId(check.side(), match),
                                check.inRoster(),
                                slotToTeamId(check.matchedTeam(), match),
                                check.matchedRosterName()))
                        .toList(),
                analysis.getAnalyzedAt()
        );
    }

    private DisputeSummaryResponse toSummary(
            DisputeEntity dispute,
            MatchView match,
            AiAnalysisResultEntity analysis,
            Map<UUID, TeamView> teamsById) {
        return new DisputeSummaryResponse(
                dispute.getId(),
                match.id(),
                match.roundNumber(),
                match.matchOrder(),
                dispute.getStatus(),
                toTeam(match.team1Id(), teamsById),
                toTeam(match.team2Id(), teamsById),
                dispute.getOpenedByTeamId(),
                analysis == null ? AiAnalysisStatus.PENDING : analysis.getStatus(),
                analysis == null ? null : analysis.getRecognizedTeam1Score(),
                analysis == null ? null : analysis.getRecognizedTeam2Score(),
                analysis == null ? null : analysis.getConfidence(),
                analysis == null ? null : analysis.isNicknamesMatchedAll(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt()
        );
    }

    private DisputeTeamResponse toTeam(UUID teamId, Map<UUID, TeamView> teamsById) {
        TeamView team = teamId == null ? null : teamsById.get(teamId);
        return new DisputeTeamResponse(teamId, team != null ? team.name() : null);
    }

    private UUID slotToTeamId(String slot, MatchView match) {
        if (TEAM1_SLOT.equals(slot)) {
            return match.team1Id();
        }
        if (TEAM2_SLOT.equals(slot)) {
            return match.team2Id();
        }
        return null;
    }

    private Set<UUID> teamIdsOf(Collection<MatchView> matches) {
        Set<UUID> ids = new HashSet<>();
        matches.stream().filter(Objects::nonNull).forEach(match -> {
            ids.add(match.team1Id());
            ids.add(match.team2Id());
        });
        ids.remove(null);
        return ids;
    }

    private UUID resolveCaptainTeamId(MatchView match, Map<UUID, TeamView> teamsById, UUID userId) {
        TeamView team1 = teamsById.get(match.team1Id());
        TeamView team2 = teamsById.get(match.team2Id());
        if (team1 != null && userId.equals(team1.captainId())) {
            return match.team1Id();
        }
        if (team2 != null && userId.equals(team2.captainId())) {
            return match.team2Id();
        }
        throw new OnlyCaptainAllowedException();
    }

    private boolean isParticipant(MatchView match, UUID userId) {
        return teamQueryService.findTeamsByIds(Set.of(match.team1Id(), match.team2Id())).values().stream()
                .flatMap(team -> team.members().stream())
                .anyMatch(member -> member.userId().equals(userId));
    }

    private void requireOrganizer(UUID tournamentId, UUID userId) {
        UUID organizerId = tournamentParticipantsQueryService.findOrganizerId(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        if (!organizerId.equals(userId)) {
            throw new OnlyOrganizerAllowedException();
        }
    }

    private DisputeEntity findDispute(UUID disputeId) {
        return disputeRepository.findById(disputeId).orElseThrow(() -> new DisputeNotFoundException(disputeId));
    }

    private MatchView findMatch(UUID matchId) {
        return matchQueryService.findMatchById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
    }

    private TournamentResponse findTournament(UUID tournamentId) {
        return tournamentQueryService.findTournamentById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
    }

    private String screenshotUrl(UUID disputeId) {
        return "/api/v1/disputes/" + disputeId + "/screenshot";
    }
}

package com.tournado.core.disputes.service;

import com.tournado.core.disputes.domain.AiAnalysisResultEntity;
import com.tournado.core.disputes.domain.AiAnalysisStatus;
import com.tournado.core.disputes.domain.DisputeEntity;
import com.tournado.core.disputes.domain.StoredNicknameCheck;
import com.tournado.core.disputes.infra.AiAnalysisClient;
import com.tournado.core.disputes.infra.AiAnalysisReply;
import com.tournado.core.disputes.infra.AiAnalysisReport;
import com.tournado.core.disputes.infra.AiAnalysisRequest;
import com.tournado.core.disputes.infra.AiRosterEntry;
import com.tournado.core.disputes.infra.ScreenshotStorage;
import com.tournado.core.disputes.repository.AiAnalysisResultRepository;
import com.tournado.core.disputes.repository.DisputeRepository;
import com.tournado.core.matches.service.MatchQueryService;
import com.tournado.core.matches.service.MatchView;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.service.RosterEntryView;
import com.tournado.core.tournaments.service.TournamentParticipantsQueryService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Sends the dispute evidence and the locked rosters to the AI service and stores the advisory verdict
 * (US-32, US-33). Deliberately not transactional: the HTTP call may take up to the client timeout and must not
 * hold a DB connection. Any failure is stored as {@code FAILED} so the card shows "not recognised" (NFR-19).
 */
@Service
public class DisputeAiAnalysisService {

    private static final Logger LOG = LoggerFactory.getLogger(DisputeAiAnalysisService.class);
    private static final int MAX_STORED_NICKNAMES = 20;
    private static final int MAX_NICKNAME_LENGTH = 64;
    private static final String TEAM1_SLOT = "TEAM1";
    private static final String TEAM2_SLOT = "TEAM2";

    private final DisputeRepository disputeRepository;
    private final AiAnalysisResultRepository aiAnalysisResultRepository;
    private final MatchQueryService matchQueryService;
    private final TeamQueryService teamQueryService;
    private final TournamentQueryService tournamentQueryService;
    private final TournamentParticipantsQueryService tournamentParticipantsQueryService;
    private final ScreenshotStorage screenshotStorage;
    private final AiAnalysisClient aiAnalysisClient;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DisputeAiAnalysisService(
            DisputeRepository disputeRepository,
            AiAnalysisResultRepository aiAnalysisResultRepository,
            MatchQueryService matchQueryService,
            TeamQueryService teamQueryService,
            TournamentQueryService tournamentQueryService,
            TournamentParticipantsQueryService tournamentParticipantsQueryService,
            ScreenshotStorage screenshotStorage,
            AiAnalysisClient aiAnalysisClient,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.disputeRepository = disputeRepository;
        this.aiAnalysisResultRepository = aiAnalysisResultRepository;
        this.matchQueryService = matchQueryService;
        this.teamQueryService = teamQueryService;
        this.tournamentQueryService = tournamentQueryService;
        this.tournamentParticipantsQueryService = tournamentParticipantsQueryService;
        this.screenshotStorage = screenshotStorage;
        this.aiAnalysisClient = aiAnalysisClient;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void analyze(UUID disputeId) {
        if (aiAnalysisResultRepository.existsByDisputeId(disputeId)) {
            return;
        }
        DisputeEntity dispute = disputeRepository.findById(disputeId).orElse(null);
        if (dispute == null) {
            LOG.warn("Skipping AI analysis: dispute {} not found", disputeId);
            return;
        }
        MatchView match = matchQueryService.findMatchById(dispute.getMatchId()).orElse(null);
        if (match == null) {
            LOG.warn("Skipping AI analysis: match {} of dispute {} not found", dispute.getMatchId(), disputeId);
            return;
        }

        AiAnalysisResultEntity result;
        try {
            AiAnalysisReply reply = aiAnalysisClient.analyze(buildRequest(dispute, match));
            result = fromReply(disputeId, reply);
        } catch (RuntimeException ex) {
            LOG.warn("AI analysis failed for dispute {}: {}", disputeId, ex.getMessage());
            result = failedResult(disputeId);
        }

        try {
            aiAnalysisResultRepository.save(result);
        } catch (DataIntegrityViolationException ex) {
            LOG.info("AI analysis for dispute {} was already stored by a concurrent run", disputeId);
            return;
        }
        eventPublisher.publishEvent(new DisputeLifecycleEvent(
                DisputeLifecycleEvent.Type.DISPUTE_AI_ANALYZED, disputeId, match.tournamentId(), match.id()));
    }

    private AiAnalysisRequest buildRequest(DisputeEntity dispute, MatchView match) {
        TournamentResponse tournament = tournamentQueryService.findTournamentById(match.tournamentId())
                .orElseThrow(() -> new IllegalStateException("Tournament '" + match.tournamentId() + "' not found"));
        Map<UUID, TeamView> teamsById = teamQueryService.findTeamsByIds(Set.of(match.team1Id(), match.team2Id()));
        TeamView team1 = teamsById.get(match.team1Id());
        TeamView team2 = teamsById.get(match.team2Id());

        return new AiAnalysisRequest(
                tournament.discipline(),
                team1 != null ? team1.name() : "Team 1",
                team2 != null ? team2.name() : "Team 2",
                rosterFor(match.tournamentId(), match.team1Id(), team1),
                rosterFor(match.tournamentId(), match.team2Id(), team2),
                screenshotStorage.load(dispute.getScreenshotFilePath()),
                Path.of(dispute.getScreenshotFilePath()).getFileName().toString(),
                dispute.getScreenshotMimeType()
        );
    }

    /**
     * BR-49: compare against the roster frozen at Roster Lock; fall back to the current team members only when
     * no snapshot exists (e.g. data created before roster lock was introduced).
     */
    private List<AiRosterEntry> rosterFor(UUID tournamentId, UUID teamId, TeamView team) {
        List<RosterEntryView> locked = tournamentParticipantsQueryService.findLockedRoster(tournamentId, teamId);
        if (!locked.isEmpty()) {
            return locked.stream()
                    .map(entry -> new AiRosterEntry(entry.displayName(), entry.gameIdentifier()))
                    .toList();
        }
        if (team == null) {
            return List.of();
        }
        return team.members().stream()
                .map(member -> new AiRosterEntry(member.displayName(), firstGameIdentifier(member)))
                .toList();
    }

    private String firstGameIdentifier(TeamMemberView member) {
        return Stream.of(member.steamId(), member.supercellPlayerTag(), member.chessUsername())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * NFR-22: the AI answer is untrusted data. Only a well-formed SUCCESS with two non-negative scores becomes a
     * usable suggestion; everything else is stored as UNRECOGNIZED.
     */
    private AiAnalysisResultEntity fromReply(UUID disputeId, AiAnalysisReply reply) {
        AiAnalysisReport report = reply.report();
        AiAnalysisReport.RecognizedScore score = report.recognizedScore();
        boolean usable = "SUCCESS".equals(report.status())
                && score != null
                && score.team1() != null && score.team2() != null
                && score.team1() >= 0 && score.team2() >= 0;

        List<StoredNicknameCheck> checks = report.nicknameChecks() == null ? List.of() : report.nicknameChecks().stream()
                .filter(check -> check.nickname() != null && !check.nickname().isBlank())
                .limit(MAX_STORED_NICKNAMES)
                .map(check -> new StoredNicknameCheck(
                        truncate(check.nickname()),
                        normalizeSlot(check.side()),
                        check.inRoster(),
                        normalizeSlot(check.matchedTeam()),
                        truncate(check.matchedRosterName())))
                .toList();

        AiAnalysisResultEntity entity = new AiAnalysisResultEntity();
        entity.setId(UUID.randomUUID());
        entity.setDisputeId(disputeId);
        entity.setStatus(usable ? AiAnalysisStatus.SUCCESS : AiAnalysisStatus.UNRECOGNIZED);
        entity.setRecognizedTeam1Score(usable ? score.team1() : null);
        entity.setRecognizedTeam2Score(usable ? score.team2() : null);
        entity.setConfidence(toConfidence(report.confidence()));
        entity.setNicknameChecks(checks);
        entity.setNicknamesMatchedAll(report.allRosterNicknamesFound() && !checks.isEmpty()
                && checks.stream().allMatch(StoredNicknameCheck::inRoster));
        entity.setRawResponsePayload(reply.rawPayload());
        entity.setAnalyzedAt(Instant.now(clock));
        return entity;
    }

    private AiAnalysisResultEntity failedResult(UUID disputeId) {
        AiAnalysisResultEntity entity = new AiAnalysisResultEntity();
        entity.setId(UUID.randomUUID());
        entity.setDisputeId(disputeId);
        entity.setStatus(AiAnalysisStatus.FAILED);
        entity.setNicknameChecks(List.of());
        entity.setNicknamesMatchedAll(false);
        entity.setAnalyzedAt(Instant.now(clock));
        return entity;
    }

    private BigDecimal toConfidence(Double confidence) {
        if (confidence == null || confidence.isNaN()) {
            return null;
        }
        double clamped = Math.max(0.0, Math.min(1.0, confidence));
        return BigDecimal.valueOf(clamped).setScale(3, RoundingMode.HALF_UP);
    }

    private String normalizeSlot(String slot) {
        return TEAM1_SLOT.equals(slot) || TEAM2_SLOT.equals(slot) ? slot : null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.length() > MAX_NICKNAME_LENGTH ? trimmed.substring(0, MAX_NICKNAME_LENGTH) : trimmed;
    }
}

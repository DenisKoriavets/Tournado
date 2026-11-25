package com.tournado.core.disputes;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthenticatedUser;
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
import com.tournado.core.disputes.infra.AiAnalysisReply;
import com.tournado.core.disputes.infra.AiAnalysisReport;
import com.tournado.core.disputes.infra.AiAnalysisRequest;
import com.tournado.core.disputes.infra.AiRosterEntry;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.service.MatchView;
import com.tournado.core.matches.service.ScoreSubmissionView;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamView;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import com.tournado.core.tournaments.service.RosterEntryView;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockMultipartFile;

public final class DisputeTestResources {

    private DisputeTestResources() {
    }

    public static final UUID DISPUTE_ID = UUID.fromString("d01e2345-6789-abcd-ef01-23456789abcd");
    public static final UUID MATCH_ID = UUID.fromString("c01d2345-6789-abcd-ef01-23456789abcd");
    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID TEAM1_ID = UUID.fromString("7a30368b-59b1-4f18-bb59-8395f13d8d64");
    public static final UUID TEAM2_ID = UUID.fromString("9c40479c-59b1-4f18-bb59-8395f13d8d65");
    public static final UUID CAPTAIN1_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID CAPTAIN2_ID = UUID.fromString("a1b2c3d4-e5f6-4789-9012-112233445566");
    public static final UUID PLAYER2_ID = UUID.fromString("b2c3d4e5-f6a7-4890-9123-223344556677");
    public static final UUID ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID OUTSIDER_ID = UUID.fromString("00000000-1111-4222-8333-444455556666");
    public static final String TEAM1_NAME = "QA Terror Squad";
    public static final String TEAM2_NAME = "QA Vegayta Five";
    public static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");
    public static final Instant DISPUTE_CREATED_AT = NOW.minusSeconds(600L);
    public static final String SCREENSHOT_PATH = "disputes/" + DISPUTE_ID + ".png";
    public static final String SCREENSHOT_URL = "/api/v1/disputes/" + DISPUTE_ID + "/screenshot";

    public static final byte[] PNG_BYTES = withPadding(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
    public static final byte[] JPEG_BYTES = withPadding(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
    public static final byte[] WEBP_BYTES = withPadding(new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'});
    public static final byte[] PDF_BYTES = withPadding(new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '7'});

    private static byte[] withPadding(byte[] signature) {
        byte[] content = Arrays.copyOf(signature, 64);
        Arrays.fill(content, signature.length, content.length, (byte) 7);
        return content;
    }

    public static MockMultipartFile buildPngScreenshotFile() {
        return new MockMultipartFile("screenshot", "scoreboard.png", "image/png", PNG_BYTES);
    }

    public static MockMultipartFile buildPdfScreenshotFile() {
        return new MockMultipartFile("screenshot", "scoreboard.png", "image/png", PDF_BYTES);
    }

    public static AuthenticatedUser buildCaptain1AuthUser() {
        return new AuthenticatedUser(CAPTAIN1_ID, "BOT Mayer", "t.captain@test.tournado.ua", null);
    }

    public static AuthenticatedUser buildOrganizerAuthUser() {
        return new AuthenticatedUser(ORGANIZER_ID, "Dispute QA Organizer", "dispute.organizer@test.tournado.ua", null);
    }

    public static TournamentResponse buildCs2Tournament() {
        return buildTournament(Discipline.CS2);
    }

    public static TournamentResponse buildChessTournament() {
        return buildTournament(Discipline.CHESS);
    }

    private static TournamentResponse buildTournament(Discipline discipline) {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "Dispute QA Arena",
                "QA",
                discipline,
                discipline.getTeamSize(),
                TournamentFormat.SINGLE_ELIMINATION,
                8,
                TournamentStatus.IN_PROGRESS,
                NOW.minusSeconds(10800L),
                NOW.minusSeconds(7200L),
                NOW.minusSeconds(3600L),
                null,
                discipline.getDefaultMapPool(),
                4,
                NOW.minusSeconds(86400L)
        );
    }

    public static TeamView buildTeam1View() {
        return new TeamView(TEAM1_ID, TEAM1_NAME, Discipline.CS2, CAPTAIN1_ID, 1, 5, false,
                List.of(new TeamMemberView(CAPTAIN1_ID, "BOT Mayer", true, "76561190000000001", null, null)));
    }

    public static TeamView buildTeam2View() {
        return new TeamView(TEAM2_ID, TEAM2_NAME, Discipline.CS2, CAPTAIN2_ID, 2, 5, false, List.of(
                new TeamMemberView(CAPTAIN2_ID, "Vegayta", true, "76561190000000011", null, null),
                new TeamMemberView(PLAYER2_ID, "BOT Ulysses", false, "76561190000000012", null, null)));
    }

    public static Map<UUID, TeamView> buildTeamsById() {
        return Map.of(TEAM1_ID, buildTeam1View(), TEAM2_ID, buildTeam2View());
    }

    public static List<ScoreSubmissionView> buildMismatchedSubmissions() {
        return List.of(
                new ScoreSubmissionView(TEAM1_ID, 13, 9, NOW.minusSeconds(900L)),
                new ScoreSubmissionView(TEAM2_ID, 9, 13, NOW.minusSeconds(800L)));
    }

    public static MatchView buildMatchView(MatchStatus status, List<ScoreSubmissionView> submissions) {
        return new MatchView(MATCH_ID, TOURNAMENT_ID, 1, 1, TEAM1_ID, TEAM2_ID, null, status, submissions);
    }

    public static MatchView buildInProgressMatchWithMismatch() {
        return buildMatchView(MatchStatus.IN_PROGRESS, buildMismatchedSubmissions());
    }

    public static MatchView buildDisputedMatch() {
        return buildMatchView(MatchStatus.DISPUTED, buildMismatchedSubmissions());
    }

    public static MatchView buildCompletedMatch(UUID winnerTeamId) {
        return new MatchView(MATCH_ID, TOURNAMENT_ID, 1, 1, TEAM1_ID, TEAM2_ID, winnerTeamId, MatchStatus.COMPLETED,
                buildMismatchedSubmissions());
    }

    public static DisputeEntity buildOpenDisputeEntity() {
        DisputeEntity dispute = new DisputeEntity();
        dispute.setId(DISPUTE_ID);
        dispute.setMatchId(MATCH_ID);
        dispute.setOpenedByTeamId(TEAM1_ID);
        dispute.setOpenedByUserId(CAPTAIN1_ID);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setScreenshotFilePath(SCREENSHOT_PATH);
        dispute.setScreenshotMimeType("image/png");
        dispute.setScreenshotFileSizeBytes(PNG_BYTES.length);
        dispute.setCreatedAt(DISPUTE_CREATED_AT);
        return dispute;
    }

    public static DisputeEntity buildExpectedNewDisputeEntity() {
        DisputeEntity dispute = buildOpenDisputeEntity();
        dispute.setCreatedAt(NOW);
        return dispute;
    }

    public static DisputeEntity buildExpectedResolvedDisputeEntity(
            DisputeResolutionSource source, int team1Score, int team2Score, UUID winnerTeamId, String notes) {
        DisputeEntity dispute = buildOpenDisputeEntity();
        dispute.setStatus(DisputeStatus.RESOLVED);
        dispute.setResolvedByUserId(ORGANIZER_ID);
        dispute.setResolvedAt(NOW);
        dispute.setResolutionWinnerTeamId(winnerTeamId);
        dispute.setResolutionTeam1Score(team1Score);
        dispute.setResolutionTeam2Score(team2Score);
        dispute.setResolutionSource(source);
        dispute.setResolutionNotes(notes);
        return dispute;
    }

    public static List<StoredNicknameCheck> buildStoredNicknameChecks() {
        return List.of(
                new StoredNicknameCheck("BOT Mayer", "TEAM1", true, "TEAM1", "BOT Mayer"),
                new StoredNicknameCheck("Vegayta", "TEAM2", true, "TEAM2", "Vegayta"),
                new StoredNicknameCheck("xX_Ringer_Xx", "TEAM2", false, null, null));
    }

    public static Map<String, Object> buildRawSuccessPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "SUCCESS");
        payload.put("recognizedScore", Map.of("team1", 0, "team2", 1));
        payload.put("confidence", 0.96);
        payload.put("allRosterNicknamesFound", false);
        return payload;
    }

    public static AiAnalysisResultEntity buildSuccessAnalysisEntity() {
        AiAnalysisResultEntity analysis = new AiAnalysisResultEntity();
        analysis.setId(UUID.fromString("a1a1a1a1-0000-4000-8000-000000000001"));
        analysis.setDisputeId(DISPUTE_ID);
        analysis.setStatus(AiAnalysisStatus.SUCCESS);
        analysis.setRecognizedTeam1Score(0);
        analysis.setRecognizedTeam2Score(1);
        analysis.setConfidence(new BigDecimal("0.960"));
        analysis.setNicknameChecks(buildStoredNicknameChecks());
        analysis.setNicknamesMatchedAll(false);
        analysis.setRawResponsePayload(buildRawSuccessPayload());
        analysis.setAnalyzedAt(NOW);
        return analysis;
    }

    public static AiAnalysisResultEntity buildUnrecognizedAnalysisEntity() {
        AiAnalysisResultEntity analysis = new AiAnalysisResultEntity();
        analysis.setId(UUID.fromString("a1a1a1a1-0000-4000-8000-000000000002"));
        analysis.setDisputeId(DISPUTE_ID);
        analysis.setStatus(AiAnalysisStatus.UNRECOGNIZED);
        analysis.setNicknameChecks(List.of());
        analysis.setNicknamesMatchedAll(false);
        analysis.setAnalyzedAt(NOW);
        return analysis;
    }

    public static AiAnalysisResultEntity buildExpectedFailedAnalysisEntity() {
        AiAnalysisResultEntity analysis = new AiAnalysisResultEntity();
        analysis.setDisputeId(DISPUTE_ID);
        analysis.setStatus(AiAnalysisStatus.FAILED);
        analysis.setNicknameChecks(List.of());
        analysis.setNicknamesMatchedAll(false);
        analysis.setAnalyzedAt(NOW);
        return analysis;
    }

    public static AiAnalysisReport buildSuccessAiReport() {
        return new AiAnalysisReport(
                "SUCCESS",
                new AiAnalysisReport.RecognizedScore(0, 1),
                0.96,
                List.of("BOT Mayer", "Vegayta", "xX_Ringer_Xx"),
                List.of(
                        new AiAnalysisReport.NicknameCheck("BOT Mayer", "TEAM1", true, "TEAM1", "BOT Mayer"),
                        new AiAnalysisReport.NicknameCheck("Vegayta", "TEAM2", true, "TEAM2", "Vegayta"),
                        new AiAnalysisReport.NicknameCheck("xX_Ringer_Xx", "TEAM2", false, null, null)),
                false,
                null,
                "groq",
                1420L
        );
    }

    public static AiAnalysisReply buildSuccessAiReply() {
        return new AiAnalysisReply(buildSuccessAiReport(), buildRawSuccessPayload());
    }

    public static AiAnalysisReply buildMalformedSuccessAiReply() {
        AiAnalysisReport report = new AiAnalysisReport(
                "SUCCESS", new AiAnalysisReport.RecognizedScore(-3, null), 7.5, List.of(), null, true, null, "groq", 10L);
        return new AiAnalysisReply(report, Map.of("status", "SUCCESS"));
    }

    public static AiAnalysisResultEntity buildExpectedMalformedReplyEntity() {
        AiAnalysisResultEntity analysis = new AiAnalysisResultEntity();
        analysis.setDisputeId(DISPUTE_ID);
        analysis.setStatus(AiAnalysisStatus.UNRECOGNIZED);
        analysis.setConfidence(new BigDecimal("1.000"));
        analysis.setNicknameChecks(List.of());
        analysis.setNicknamesMatchedAll(false);
        analysis.setRawResponsePayload(Map.of("status", "SUCCESS"));
        analysis.setAnalyzedAt(NOW);
        return analysis;
    }

    public static List<RosterEntryView> buildLockedRoster1() {
        return List.of(new RosterEntryView(CAPTAIN1_ID, "BOT Mayer", "76561190000000001", true));
    }

    public static List<RosterEntryView> buildLockedRoster2() {
        return List.of(
                new RosterEntryView(CAPTAIN2_ID, "Vegayta", "76561190000000011", true),
                new RosterEntryView(PLAYER2_ID, "BOT Ulysses", "76561190000000012", false));
    }

    public static AiAnalysisRequest buildExpectedAiRequest(List<AiRosterEntry> team1Roster) {
        return new AiAnalysisRequest(
                Discipline.CS2,
                TEAM1_NAME,
                TEAM2_NAME,
                team1Roster,
                List.of(new AiRosterEntry("Vegayta", "76561190000000011"), new AiRosterEntry("BOT Ulysses", "76561190000000012")),
                PNG_BYTES,
                DISPUTE_ID + ".png",
                "image/png"
        );
    }

    public static List<AiRosterEntry> buildExpectedLockedTeam1AiRoster() {
        return List.of(new AiRosterEntry("BOT Mayer", "76561190000000001"));
    }

    public static OpenDisputeResponse buildExpectedOpenDisputeResponse() {
        return new OpenDisputeResponse(DISPUTE_ID, MATCH_ID, DisputeStatus.OPEN, SCREENSHOT_URL, AiAnalysisStatus.PENDING);
    }

    public static List<SubmittedScoreResponse> buildExpectedSubmittedScores() {
        return List.of(
                new SubmittedScoreResponse(TEAM1_ID, 13, 9, NOW.minusSeconds(900L)),
                new SubmittedScoreResponse(TEAM2_ID, 9, 13, NOW.minusSeconds(800L)));
    }

    public static AiAnalysisResponse buildExpectedSuccessAnalysisResponse() {
        return new AiAnalysisResponse(
                AiAnalysisStatus.SUCCESS,
                0,
                1,
                new BigDecimal("0.960"),
                false,
                List.of("BOT Mayer", "Vegayta", "xX_Ringer_Xx"),
                List.of(
                        new NicknameCheckResponse("BOT Mayer", TEAM1_ID, true, TEAM1_ID, "BOT Mayer"),
                        new NicknameCheckResponse("Vegayta", TEAM2_ID, true, TEAM2_ID, "Vegayta"),
                        new NicknameCheckResponse("xX_Ringer_Xx", TEAM2_ID, false, null, null)),
                NOW
        );
    }

    public static DisputeCardResponse buildExpectedCard(
            AiAnalysisStatus aiStatus, AiAnalysisResponse analysis, boolean canApprove) {
        return new DisputeCardResponse(
                DISPUTE_ID,
                MATCH_ID,
                TOURNAMENT_ID,
                DisputeStatus.OPEN,
                SCREENSHOT_URL,
                1,
                1,
                new DisputeTeamResponse(TEAM1_ID, TEAM1_NAME),
                new DisputeTeamResponse(TEAM2_ID, TEAM2_NAME),
                TEAM1_ID,
                buildExpectedSubmittedScores(),
                aiStatus,
                analysis,
                canApprove,
                null,
                DISPUTE_CREATED_AT
        );
    }

    public static DisputeCardResponse buildExpectedResolvedCard() {
        return new DisputeCardResponse(
                DISPUTE_ID,
                MATCH_ID,
                TOURNAMENT_ID,
                DisputeStatus.RESOLVED,
                SCREENSHOT_URL,
                1,
                1,
                new DisputeTeamResponse(TEAM1_ID, TEAM1_NAME),
                new DisputeTeamResponse(TEAM2_ID, TEAM2_NAME),
                TEAM1_ID,
                buildExpectedSubmittedScores(),
                AiAnalysisStatus.SUCCESS,
                buildExpectedSuccessAnalysisResponse(),
                false,
                new DisputeResolutionResponse(DisputeResolutionSource.MANUAL_SCORE, TEAM1_ID, 13, 9, "Replay checked",
                        ORGANIZER_ID, NOW),
                DISPUTE_CREATED_AT
        );
    }

    public static TournamentDisputesResponse buildExpectedTournamentDisputes() {
        return new TournamentDisputesResponse(TOURNAMENT_ID, 1, List.of(new DisputeSummaryResponse(
                DISPUTE_ID,
                MATCH_ID,
                1,
                1,
                DisputeStatus.OPEN,
                new DisputeTeamResponse(TEAM1_ID, TEAM1_NAME),
                new DisputeTeamResponse(TEAM2_ID, TEAM2_NAME),
                TEAM1_ID,
                AiAnalysisStatus.SUCCESS,
                0,
                1,
                new BigDecimal("0.960"),
                false,
                DISPUTE_CREATED_AT,
                null
        )));
    }

    public static ResolveDisputeRequest buildAcceptAiRequest() {
        return new ResolveDisputeRequest(DisputeResolutionSource.AI_SUGGESTION, null, null, "AI hint confirmed");
    }

    public static ResolveDisputeRequest buildManualRequest(Integer team1Score, Integer team2Score) {
        return new ResolveDisputeRequest(DisputeResolutionSource.MANUAL_SCORE, team1Score, team2Score, "Replay checked");
    }

    public static ResolveDisputeResponse buildExpectedResolveResponse(
            DisputeResolutionSource source, int team1Score, int team2Score, UUID winnerTeamId) {
        return new ResolveDisputeResponse(DISPUTE_ID, DisputeStatus.RESOLVED, MATCH_ID, MatchStatus.COMPLETED,
                winnerTeamId, team1Score, team2Score, source, NOW);
    }

    public static ProblemDetail buildProblemDetail(HttpStatus status, String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://tournado.ua/errors/" + code));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", code);
        return problem;
    }
}

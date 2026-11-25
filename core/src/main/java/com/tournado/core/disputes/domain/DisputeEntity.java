package com.tournado.core.disputes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class DisputeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, updatable = false)
    private UUID matchId;

    @Column(name = "opened_by_team_id", nullable = false, updatable = false)
    private UUID openedByTeamId;

    @Column(name = "opened_by_user_id", nullable = false, updatable = false)
    private UUID openedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private DisputeStatus status;

    @Column(name = "screenshot_file_path", columnDefinition = "TEXT", nullable = false, updatable = false)
    private String screenshotFilePath;

    @Column(name = "screenshot_mime_type", length = 64, nullable = false, updatable = false)
    private String screenshotMimeType;

    @Column(name = "screenshot_file_size_bytes", nullable = false, updatable = false)
    private long screenshotFileSizeBytes;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_winner_team_id")
    private UUID resolutionWinnerTeamId;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolution_team1_score")
    private Integer resolutionTeam1Score;

    @Column(name = "resolution_team2_score")
    private Integer resolutionTeam2Score;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_source", length = 32)
    private DisputeResolutionSource resolutionSource;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DisputeEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (status == null) {
            status = DisputeStatus.OPEN;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMatchId() {
        return matchId;
    }

    public void setMatchId(UUID matchId) {
        this.matchId = matchId;
    }

    public UUID getOpenedByTeamId() {
        return openedByTeamId;
    }

    public void setOpenedByTeamId(UUID openedByTeamId) {
        this.openedByTeamId = openedByTeamId;
    }

    public UUID getOpenedByUserId() {
        return openedByUserId;
    }

    public void setOpenedByUserId(UUID openedByUserId) {
        this.openedByUserId = openedByUserId;
    }

    public DisputeStatus getStatus() {
        return status;
    }

    public void setStatus(DisputeStatus status) {
        this.status = status;
    }

    public String getScreenshotFilePath() {
        return screenshotFilePath;
    }

    public void setScreenshotFilePath(String screenshotFilePath) {
        this.screenshotFilePath = screenshotFilePath;
    }

    public String getScreenshotMimeType() {
        return screenshotMimeType;
    }

    public void setScreenshotMimeType(String screenshotMimeType) {
        this.screenshotMimeType = screenshotMimeType;
    }

    public long getScreenshotFileSizeBytes() {
        return screenshotFileSizeBytes;
    }

    public void setScreenshotFileSizeBytes(long screenshotFileSizeBytes) {
        this.screenshotFileSizeBytes = screenshotFileSizeBytes;
    }

    public UUID getResolvedByUserId() {
        return resolvedByUserId;
    }

    public void setResolvedByUserId(UUID resolvedByUserId) {
        this.resolvedByUserId = resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public UUID getResolutionWinnerTeamId() {
        return resolutionWinnerTeamId;
    }

    public void setResolutionWinnerTeamId(UUID resolutionWinnerTeamId) {
        this.resolutionWinnerTeamId = resolutionWinnerTeamId;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Integer getResolutionTeam1Score() {
        return resolutionTeam1Score;
    }

    public void setResolutionTeam1Score(Integer resolutionTeam1Score) {
        this.resolutionTeam1Score = resolutionTeam1Score;
    }

    public Integer getResolutionTeam2Score() {
        return resolutionTeam2Score;
    }

    public void setResolutionTeam2Score(Integer resolutionTeam2Score) {
        this.resolutionTeam2Score = resolutionTeam2Score;
    }

    public DisputeResolutionSource getResolutionSource() {
        return resolutionSource;
    }

    public void setResolutionSource(DisputeResolutionSource resolutionSource) {
        this.resolutionSource = resolutionSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

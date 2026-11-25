package com.tournado.core.matches.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "score_submissions")
public class ScoreSubmissionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, updatable = false)
    private UUID matchId;

    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    @Column(name = "submitted_by_user_id", nullable = false, updatable = false)
    private UUID submittedByUserId;

    @Column(name = "team1_score", nullable = false, updatable = false)
    private int team1Score;

    @Column(name = "team2_score", nullable = false, updatable = false)
    private int team2Score;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    public ScoreSubmissionEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (submittedAt == null) {
            submittedAt = Instant.now();
        }
    }

    public boolean hasSameScoreAs(ScoreSubmissionEntity other) {
        return team1Score == other.team1Score && team2Score == other.team2Score;
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

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getSubmittedByUserId() {
        return submittedByUserId;
    }

    public void setSubmittedByUserId(UUID submittedByUserId) {
        this.submittedByUserId = submittedByUserId;
    }

    public int getTeam1Score() {
        return team1Score;
    }

    public void setTeam1Score(int team1Score) {
        this.team1Score = team1Score;
    }

    public int getTeam2Score() {
        return team2Score;
    }

    public void setTeam2Score(int team2Score) {
        this.team2Score = team2Score;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }
}

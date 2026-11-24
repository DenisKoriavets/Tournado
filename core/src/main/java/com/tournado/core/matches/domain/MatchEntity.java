package com.tournado.core.matches.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "matches")
public class MatchEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tournament_id", nullable = false, updatable = false)
    private UUID tournamentId;

    @Column(name = "bracket_id", nullable = false, updatable = false)
    private UUID bracketId;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @Column(name = "match_order", nullable = false)
    private int matchOrder;

    @Column(name = "team1_id")
    private UUID team1Id;

    @Column(name = "team2_id")
    private UUID team2Id;

    @Column(name = "winner_team_id")
    private UUID winnerTeamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private MatchStatus status;

    @Column(name = "is_bye", nullable = false)
    private boolean isBye;

    @Column(name = "ready_deadline_at")
    private Instant readyDeadlineAt;

    @Column(name = "ban_deadline_at")
    private Instant banDeadlineAt;

    @Column(name = "team1_ready", nullable = false)
    private boolean team1Ready;

    @Column(name = "team2_ready", nullable = false)
    private boolean team2Ready;

    @Column(name = "current_ban_turn_team_id")
    private UUID currentBanTurnTeamId;

    @Column(name = "decided_map", length = 64)
    private String decidedMap;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public MatchEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = MatchStatus.PENDING;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(UUID tournamentId) {
        this.tournamentId = tournamentId;
    }

    public UUID getBracketId() {
        return bracketId;
    }

    public void setBracketId(UUID bracketId) {
        this.bracketId = bracketId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public int getMatchOrder() {
        return matchOrder;
    }

    public void setMatchOrder(int matchOrder) {
        this.matchOrder = matchOrder;
    }

    public UUID getTeam1Id() {
        return team1Id;
    }

    public void setTeam1Id(UUID team1Id) {
        this.team1Id = team1Id;
    }

    public UUID getTeam2Id() {
        return team2Id;
    }

    public void setTeam2Id(UUID team2Id) {
        this.team2Id = team2Id;
    }

    public UUID getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(UUID winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public boolean isBye() {
        return isBye;
    }

    public void setBye(boolean bye) {
        isBye = bye;
    }

    public Instant getReadyDeadlineAt() {
        return readyDeadlineAt;
    }

    public void setReadyDeadlineAt(Instant readyDeadlineAt) {
        this.readyDeadlineAt = readyDeadlineAt;
    }

    public Instant getBanDeadlineAt() {
        return banDeadlineAt;
    }

    public void setBanDeadlineAt(Instant banDeadlineAt) {
        this.banDeadlineAt = banDeadlineAt;
    }

    public boolean isTeam1Ready() {
        return team1Ready;
    }

    public void setTeam1Ready(boolean team1Ready) {
        this.team1Ready = team1Ready;
    }

    public boolean isTeam2Ready() {
        return team2Ready;
    }

    public void setTeam2Ready(boolean team2Ready) {
        this.team2Ready = team2Ready;
    }

    public UUID getCurrentBanTurnTeamId() {
        return currentBanTurnTeamId;
    }

    public void setCurrentBanTurnTeamId(UUID currentBanTurnTeamId) {
        this.currentBanTurnTeamId = currentBanTurnTeamId;
    }

    public String getDecidedMap() {
        return decidedMap;
    }

    public void setDecidedMap(String decidedMap) {
        this.decidedMap = decidedMap;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

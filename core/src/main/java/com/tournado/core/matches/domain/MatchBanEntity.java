package com.tournado.core.matches.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_bans")
public class MatchBanEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, updatable = false)
    private UUID matchId;

    @Column(name = "team_id", nullable = false, updatable = false)
    private UUID teamId;

    @Column(name = "map_name", length = 64, nullable = false, updatable = false)
    private String mapName;

    @Column(name = "ban_order", nullable = false, updatable = false)
    private int banOrder;

    @Column(name = "banned_at", nullable = false, updatable = false)
    private Instant bannedAt;

    public MatchBanEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (bannedAt == null) {
            bannedAt = Instant.now();
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

    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public int getBanOrder() {
        return banOrder;
    }

    public void setBanOrder(int banOrder) {
        this.banOrder = banOrder;
    }

    public Instant getBannedAt() {
        return bannedAt;
    }

    public void setBannedAt(Instant bannedAt) {
        this.bannedAt = bannedAt;
    }
}

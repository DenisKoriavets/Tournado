package com.tournado.core.matches.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_credentials")
public class MatchCredentialsEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "match_id", nullable = false, updatable = false, unique = true)
    private UUID matchId;

    @Column(name = "cs2_connect_string", columnDefinition = "TEXT")
    private String cs2ConnectString;

    @Column(name = "brawl_stars_code", length = 32)
    private String brawlStarsCode;

    @Column(name = "chess_game_url", columnDefinition = "TEXT")
    private String chessGameUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public MatchCredentialsEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
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

    public String getCs2ConnectString() {
        return cs2ConnectString;
    }

    public void setCs2ConnectString(String cs2ConnectString) {
        this.cs2ConnectString = cs2ConnectString;
    }

    public String getBrawlStarsCode() {
        return brawlStarsCode;
    }

    public void setBrawlStarsCode(String brawlStarsCode) {
        this.brawlStarsCode = brawlStarsCode;
    }

    public String getChessGameUrl() {
        return chessGameUrl;
    }

    public void setChessGameUrl(String chessGameUrl) {
        this.chessGameUrl = chessGameUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

package com.tournado.core.bracket.domain;

import com.tournado.core.tournaments.domain.TournamentFormat;
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
@Table(name = "brackets")
public class BracketEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tournament_id", nullable = false, unique = true, updatable = false)
    private UUID tournamentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private TournamentFormat type;

    @Column(name = "total_rounds", nullable = false)
    private int totalRounds;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    public BracketEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (type == null) {
            type = TournamentFormat.SINGLE_ELIMINATION;
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
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

    public TournamentFormat getType() {
        return type;
    }

    public void setType(TournamentFormat type) {
        this.type = type;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Instant generatedAt) {
        this.generatedAt = generatedAt;
    }
}
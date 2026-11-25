package com.tournado.core.tournaments.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_roster_snapshots")
public class RosterSnapshotEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "registration_id", nullable = false)
    private UUID registrationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Column(name = "game_identifier", length = 100, nullable = false)
    private String gameIdentifier;

    @Column(name = "is_captain", nullable = false)
    private boolean isCaptain;

    @Column(name = "snapshotted_at", nullable = false, updatable = false)
    private Instant snapshottedAt;

    public RosterSnapshotEntity() {
    }

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (snapshottedAt == null) {
            snapshottedAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(UUID registrationId) {
        this.registrationId = registrationId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getGameIdentifier() {
        return gameIdentifier;
    }

    public void setGameIdentifier(String gameIdentifier) {
        this.gameIdentifier = gameIdentifier;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean captain) {
        isCaptain = captain;
    }

    public Instant getSnapshottedAt() {
        return snapshottedAt;
    }

    public void setSnapshottedAt(Instant snapshottedAt) {
        this.snapshottedAt = snapshottedAt;
    }
}

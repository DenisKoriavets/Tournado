package com.tournado.core.tournaments.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournaments")
public class TournamentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discipline", length = 32, nullable = false)
    private Discipline discipline;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 32, nullable = false)
    private TournamentFormat format;

    @Column(name = "slot_limit", nullable = false)
    private int slotLimit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private TournamentStatus status;

    @Column(name = "registration_deadline_at", nullable = false)
    private Instant registrationDeadlineAt;

    @Column(name = "check_in_opens_at", nullable = false)
    private Instant checkInOpensAt;

    @Column(name = "start_time_at", nullable = false)
    private Instant startTimeAt;

    @Column(name = "cs2_server_connect", columnDefinition = "TEXT")
    private String cs2ServerConnect;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sortOrder ASC")
    private List<TournamentMapEntity> mapPool = new ArrayList<>();

    public TournamentEntity() {
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
        if (format == null) {
            format = TournamentFormat.SINGLE_ELIMINATION;
        }
        if (status == null) {
            status = TournamentStatus.REGISTRATION_OPEN;
        }
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addMap(String mapName, int sortOrder) {
        TournamentMapEntity mapEntity = new TournamentMapEntity(this, mapName, sortOrder);
        this.mapPool.add(mapEntity);
    }

    public List<String> getMapNames() {
        return mapPool.stream()
                .map(TournamentMapEntity::getMapName)
                .toList();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Discipline getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Discipline discipline) {
        this.discipline = discipline;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
    }

    public int getSlotLimit() {
        return slotLimit;
    }

    public void setSlotLimit(int slotLimit) {
        this.slotLimit = slotLimit;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public Instant getRegistrationDeadlineAt() {
        return registrationDeadlineAt;
    }

    public void setRegistrationDeadlineAt(Instant registrationDeadlineAt) {
        this.registrationDeadlineAt = registrationDeadlineAt;
    }

    public Instant getCheckInOpensAt() {
        return checkInOpensAt;
    }

    public void setCheckInOpensAt(Instant checkInOpensAt) {
        this.checkInOpensAt = checkInOpensAt;
    }

    public Instant getStartTimeAt() {
        return startTimeAt;
    }

    public void setStartTimeAt(Instant startTimeAt) {
        this.startTimeAt = startTimeAt;
    }

    public String getCs2ServerConnect() {
        return cs2ServerConnect;
    }

    public void setCs2ServerConnect(String cs2ServerConnect) {
        this.cs2ServerConnect = cs2ServerConnect;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
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

    public List<TournamentMapEntity> getMapPool() {
        return mapPool;
    }

    public void setMapPool(List<TournamentMapEntity> mapPool) {
        this.mapPool = mapPool;
    }
}

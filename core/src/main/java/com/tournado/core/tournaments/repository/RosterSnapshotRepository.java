package com.tournado.core.tournaments.repository;

import com.tournado.core.tournaments.domain.RosterSnapshotEntity;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RosterSnapshotRepository extends JpaRepository<RosterSnapshotEntity, UUID> {

    List<RosterSnapshotEntity> findByRegistrationIdIn(Collection<UUID> registrationIds);

    boolean existsByRegistrationId(UUID registrationId);

    boolean existsByRegistrationIdIn(Collection<UUID> registrationIds);

    @Query("SELECT DISTINCT s.registrationId FROM RosterSnapshotEntity s WHERE s.registrationId IN :registrationIds")
    Set<UUID> findSnapshottedRegistrationIds(@Param("registrationIds") Collection<UUID> registrationIds);
}

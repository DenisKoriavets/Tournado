package com.tournado.core.disputes.repository;

import com.tournado.core.disputes.domain.DisputeEntity;
import com.tournado.core.disputes.domain.DisputeStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeRepository extends JpaRepository<DisputeEntity, UUID> {

    boolean existsByMatchIdAndStatus(UUID matchId, DisputeStatus status);

    List<DisputeEntity> findByMatchIdInOrderByCreatedAtDesc(Collection<UUID> matchIds);

    long countByMatchIdInAndStatus(Collection<UUID> matchIds, DisputeStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM DisputeEntity d WHERE d.id = :id")
    Optional<DisputeEntity> findByIdForUpdate(@Param("id") UUID id);
}

package com.tournado.core.disputes.repository;

import com.tournado.core.disputes.domain.AiAnalysisResultEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResultEntity, UUID> {

    Optional<AiAnalysisResultEntity> findByDisputeId(UUID disputeId);

    List<AiAnalysisResultEntity> findByDisputeIdIn(Collection<UUID> disputeIds);

    boolean existsByDisputeId(UUID disputeId);
}

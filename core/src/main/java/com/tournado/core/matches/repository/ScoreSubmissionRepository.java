package com.tournado.core.matches.repository;

import com.tournado.core.matches.domain.ScoreSubmissionEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreSubmissionRepository extends JpaRepository<ScoreSubmissionEntity, UUID> {

    List<ScoreSubmissionEntity> findByMatchIdOrderBySubmittedAtAsc(UUID matchId);

    List<ScoreSubmissionEntity> findByMatchIdIn(Collection<UUID> matchIds);
}

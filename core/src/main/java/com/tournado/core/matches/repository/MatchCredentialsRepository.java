package com.tournado.core.matches.repository;

import com.tournado.core.matches.domain.MatchCredentialsEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchCredentialsRepository extends JpaRepository<MatchCredentialsEntity, UUID> {

    Optional<MatchCredentialsEntity> findByMatchId(UUID matchId);
}

package com.tournado.core.matches.repository;

import com.tournado.core.matches.domain.MatchBanEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchBanRepository extends JpaRepository<MatchBanEntity, UUID> {

    List<MatchBanEntity> findByMatchIdOrderByBanOrderAsc(UUID matchId);
}

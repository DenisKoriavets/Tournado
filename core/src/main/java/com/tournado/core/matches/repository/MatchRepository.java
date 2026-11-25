package com.tournado.core.matches.repository;

import com.tournado.core.matches.domain.MatchEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
}

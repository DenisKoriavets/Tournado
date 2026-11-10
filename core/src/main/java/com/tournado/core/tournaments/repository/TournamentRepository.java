package com.tournado.core.tournaments.repository;

import com.tournado.core.tournaments.domain.TournamentEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TournamentRepository extends JpaRepository<TournamentEntity, UUID> {
}

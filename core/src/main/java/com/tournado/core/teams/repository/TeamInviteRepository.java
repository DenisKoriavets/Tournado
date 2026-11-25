package com.tournado.core.teams.repository;

import com.tournado.core.teams.domain.TeamInviteEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamInviteRepository extends JpaRepository<TeamInviteEntity, UUID> {

    Optional<TeamInviteEntity> findByToken(String token);
}

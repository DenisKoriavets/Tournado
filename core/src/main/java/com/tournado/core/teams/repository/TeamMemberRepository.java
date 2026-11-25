package com.tournado.core.teams.repository;

import com.tournado.core.teams.domain.TeamMemberEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMemberEntity, UUID> {

    List<TeamMemberEntity> findByTeamIdOrderByJoinedAtAsc(UUID teamId);

    List<TeamMemberEntity> findByTeamIdIn(Collection<UUID> teamIds);

    List<TeamMemberEntity> findByUserId(UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    int countByTeamId(UUID teamId);
}

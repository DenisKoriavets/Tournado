package com.tournado.core.teams.repository;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.teams.domain.TeamEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<TeamEntity, UUID> {

    boolean existsByNameIgnoreCaseAndDiscipline(String name, Discipline discipline);

    List<TeamEntity> findByIdIn(Collection<UUID> ids);
}

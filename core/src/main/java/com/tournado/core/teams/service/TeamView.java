package com.tournado.core.teams.service;

import com.tournado.core.common.domain.Discipline;
import java.util.List;
import java.util.UUID;

public record TeamView(
        UUID id,
        String name,
        Discipline discipline,
        UUID captainId,
        int memberCount,
        int maxMembers,
        boolean isFull,
        List<TeamMemberView> members
) {
}

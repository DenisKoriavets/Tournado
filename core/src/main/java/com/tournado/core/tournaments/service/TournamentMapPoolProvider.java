package com.tournado.core.tournaments.service;

import com.tournado.core.tournaments.domain.Discipline;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TournamentMapPoolProvider {

    public List<String> getMapPoolForDiscipline(Discipline discipline) {
        if (discipline == null) {
            return List.of();
        }
        return discipline.getDefaultMapPool();
    }
}

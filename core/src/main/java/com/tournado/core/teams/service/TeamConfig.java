package com.tournado.core.teams.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TeamConfig {

    @Bean
    @ConditionalOnMissingBean(TournamentRosterLockChecker.class)
    public TournamentRosterLockChecker defaultTournamentRosterLockChecker() {
        return teamId -> false;
    }
}

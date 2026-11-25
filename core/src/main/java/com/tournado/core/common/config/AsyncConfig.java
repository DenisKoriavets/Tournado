package com.tournado.core.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Background executor for slow, non-critical work such as the AI dispute analysis call (ADR-0006):
 * opening a dispute never waits for the AI.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AI_ANALYSIS_EXECUTOR = "aiAnalysisExecutor";

    @Bean(name = AI_ANALYSIS_EXECUTOR)
    public Executor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-analysis-");
        executor.initialize();
        return executor;
    }
}

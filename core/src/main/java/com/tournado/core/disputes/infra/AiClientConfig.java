package com.tournado.core.disputes.infra;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiClientConfig {

    public static final String AI_SERVICE_REST_CLIENT = "aiServiceRestClient";

    @Bean(name = AI_SERVICE_REST_CLIENT)
    public RestClient aiServiceRestClient(
            RestClient.Builder builder,
            @Value("${tournado.ai-service-url}") String baseUrl,
            @Value("${tournado.ai.api-token:}") String apiToken,
            @Value("${tournado.ai.timeout-seconds:30}") int timeoutSeconds) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        RestClient.Builder configured = builder.baseUrl(baseUrl).requestFactory(requestFactory);
        if (apiToken != null && !apiToken.isBlank()) {
            configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken.trim());
        }
        return configured.build();
    }
}

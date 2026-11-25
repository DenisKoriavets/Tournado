package com.tournado.core.disputes.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.disputes.exception.AiServiceUnavailableException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAiAnalysisClient implements AiAnalysisClient {

    static final String ANALYZE_PATH = "/api/v1/ai/analyze-dispute";
    private static final MediaType TEXT_UTF8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
    private static final TypeReference<Map<String, Object>> RAW_PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpAiAnalysisClient(
            @Qualifier(AiClientConfig.AI_SERVICE_REST_CLIENT) RestClient restClient,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiAnalysisReply analyze(AiAnalysisRequest request) {
        try {
            String body = restClient.post()
                    .uri(ANALYZE_PATH)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(buildMultipartBody(request))
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new AiServiceUnavailableException("AI service returned an empty body", null);
            }
            Map<String, Object> rawPayload = objectMapper.readValue(body, RAW_PAYLOAD_TYPE);
            AiAnalysisReport report = objectMapper.convertValue(rawPayload, AiAnalysisReport.class);
            return new AiAnalysisReply(report, rawPayload);
        } catch (RestClientException | JsonProcessingException | IllegalArgumentException ex) {
            throw new AiServiceUnavailableException("AI service call failed: " + ex.getMessage(), ex);
        }
    }

    private MultiValueMap<String, Object> buildMultipartBody(AiAnalysisRequest request) throws JsonProcessingException {
        String filename = request.screenshotFilename();
        ByteArrayResource screenshot = new ByteArrayResource(request.screenshot()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(request.screenshotMimeType()));

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("screenshot", new HttpEntity<>(screenshot, fileHeaders));
        parts.add("discipline", textPart(request.discipline().name()));
        parts.add("team1_name", textPart(request.team1Name()));
        parts.add("team2_name", textPart(request.team2Name()));
        parts.add("team1_roster", textPart(objectMapper.writeValueAsString(request.team1Roster())));
        parts.add("team2_roster", textPart(objectMapper.writeValueAsString(request.team2Roster())));
        return parts;
    }

    /**
     * Explicit UTF-8 so Cyrillic team names survive the multipart encoding.
     */
    private HttpEntity<String> textPart(String value) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(TEXT_UTF8);
        return new HttpEntity<>(value, headers);
    }
}

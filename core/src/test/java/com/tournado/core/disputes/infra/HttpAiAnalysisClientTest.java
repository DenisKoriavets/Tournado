package com.tournado.core.disputes.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.disputes.exception.AiServiceUnavailableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.tournado.core.disputes.DisputeTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpAiAnalysisClientTest {

    private static final String AI_RESPONSE = """
            {
              "status": "SUCCESS",
              "recognizedScore": {"team1": 0, "team2": 1},
              "confidence": 0.96,
              "nicknamesDetected": ["BOT Mayer", "Vegayta", "xX_Ringer_Xx"],
              "nicknameChecks": [
                {"nickname": "BOT Mayer", "side": "TEAM1", "inRoster": true, "matchedTeam": "TEAM1", "matchedRosterName": "BOT Mayer"},
                {"nickname": "Vegayta", "side": "TEAM2", "inRoster": true, "matchedTeam": "TEAM2", "matchedRosterName": "Vegayta"},
                {"nickname": "xX_Ringer_Xx", "side": "TEAM2", "inRoster": false, "matchedTeam": null, "matchedRosterName": null}
              ],
              "allRosterNicknamesFound": false,
              "reason": null,
              "provider": "groq",
              "processingTimeMs": 1420,
              "futureField": "ignored"
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer server;
    private HttpAiAnalysisClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-service:8000")
                .defaultHeader("Authorization", "Bearer internal-token");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpAiAnalysisClient(builder.build(), objectMapper);
    }

    @Test
    void givenAiServiceAnswer_analyze_shouldPostMultipartContractAndParseReport() {
        server.expect(requestTo("http://ai-service:8000/api/v1/ai/analyze-dispute"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer internal-token"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.MULTIPART_FORM_DATA))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
                        .contains("name=\"screenshot\"; filename=\"" + DISPUTE_ID + ".png\"")
                        .contains("name=\"discipline\"", "CS2")
                        .contains("name=\"team1_name\"", TEAM1_NAME)
                        .contains("name=\"team1_roster\"", "[{\"displayName\":\"BOT Mayer\",\"gameIdentifier\":\"76561190000000001\"}]"))
                .andRespond(withSuccess(AI_RESPONSE, MediaType.APPLICATION_JSON));

        AiAnalysisReply reply = client.analyze(buildExpectedAiRequest(buildExpectedLockedTeam1AiRoster()));

        server.verify();
        assertThat(reply.report()).usingRecursiveComparison().isEqualTo(buildSuccessAiReport());
        assertThat(reply.rawPayload()).containsEntry("provider", "groq").containsEntry("futureField", "ignored");
    }

    @Test
    void givenAiServiceError_analyze_shouldThrowAiServiceUnavailableException() {
        server.expect(requestTo("http://ai-service:8000/api/v1/ai/analyze-dispute"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.analyze(buildExpectedAiRequest(List.of())))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void givenNonJsonAnswer_analyze_shouldThrowAiServiceUnavailableException() {
        server.expect(requestTo("http://ai-service:8000/api/v1/ai/analyze-dispute"))
                .andRespond(withSuccess("<html>gateway</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.analyze(buildExpectedAiRequest(List.of())))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void givenUnrecognizedAnswer_analyze_shouldParseNullScore() {
        server.expect(requestTo("http://ai-service:8000/api/v1/ai/analyze-dispute"))
                .andRespond(withSuccess("""
                        {"status": "UNRECOGNIZED", "recognizedScore": null, "confidence": null,
                         "nicknamesDetected": [], "nicknameChecks": [], "allRosterNicknamesFound": false,
                         "reason": "LOW_CONFIDENCE", "provider": "groq", "processingTimeMs": 900}
                        """, MediaType.APPLICATION_JSON));

        AiAnalysisReply reply = client.analyze(buildExpectedAiRequest(List.of()));

        assertThat(reply.report()).usingRecursiveComparison().isEqualTo(new AiAnalysisReport(
                "UNRECOGNIZED", null, null, List.of(), List.of(), false, "LOW_CONFIDENCE", "groq", 900L));
    }
}

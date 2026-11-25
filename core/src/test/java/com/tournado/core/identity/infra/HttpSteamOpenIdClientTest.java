package com.tournado.core.identity.infra;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.tournado.core.identity.IdentityTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class HttpSteamOpenIdClientTest {

    private MockRestServiceServer server;
    private HttpSteamOpenIdClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HttpSteamOpenIdClient(buildAuthProperties(), builder.build());
    }

    @Test
    void givenReturnToAndRealm_buildLoginRedirectUrl_shouldBuildOpenIdSetupUrl() {
        String returnTo = PUBLIC_BASE_URL + "/api/v1/auth/steam/callback";

        String url = client.buildLoginRedirectUrl(returnTo, PUBLIC_BASE_URL);

        assertThat(url)
                .startsWith(STEAM_OPENID_ENDPOINT + "?")
                .contains("openid.mode=checkid_setup")
                .contains("openid.ns=http://specs.openid.net/auth/2.0")
                .contains("openid.return_to=" + returnTo)
                .contains("openid.realm=" + PUBLIC_BASE_URL);
    }

    @Test
    void givenValidIdResAndSteamConfirmation_verifyAndExtractSteamId_shouldReturnSteamId() {
        Map<String, String> params = buildValidSteamCallbackParams();
        String expectedReturnTo = PUBLIC_BASE_URL + "/api/v1/auth/steam/callback";
        server.expect(requestTo(STEAM_OPENID_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ns:http://specs.openid.net/auth/2.0\nis_valid:true\n", MediaType.TEXT_PLAIN));

        Optional<String> result = client.verifyAndExtractSteamId(params, expectedReturnTo);

        assertThat(result).contains(STEAM_ID);
    }

    @Test
    void givenInvalidReturnTo_verifyAndExtractSteamId_shouldReturnEmptyWithoutCallingSteam() {
        Map<String, String> params = Map.of(
                "openid.mode", "id_res",
                "openid.return_to", "https://evil.example/callback",
                "openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID
        );

        Optional<String> result = client.verifyAndExtractSteamId(params, PUBLIC_BASE_URL + "/api/v1/auth/steam/callback");

        assertThat(result).isEmpty();
    }

    @Test
    void givenSteamReturnsIsValidFalse_verifyAndExtractSteamId_shouldReturnEmpty() {
        Map<String, String> params = buildValidSteamCallbackParams();
        String expectedReturnTo = PUBLIC_BASE_URL + "/api/v1/auth/steam/callback";
        server.expect(requestTo(STEAM_OPENID_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("ns:http://specs.openid.net/auth/2.0\nis_valid:false\n", MediaType.TEXT_PLAIN));

        Optional<String> result = client.verifyAndExtractSteamId(params, expectedReturnTo);

        assertThat(result).isEmpty();
    }

    @Test
    void givenSteamEndpointReturnsServerError_verifyAndExtractSteamId_shouldReturnEmpty() {
        Map<String, String> params = buildValidSteamCallbackParams();
        String expectedReturnTo = PUBLIC_BASE_URL + "/api/v1/auth/steam/callback";
        server.expect(requestTo(STEAM_OPENID_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        Optional<String> result = client.verifyAndExtractSteamId(params, expectedReturnTo);

        assertThat(result).isEmpty();
    }
}

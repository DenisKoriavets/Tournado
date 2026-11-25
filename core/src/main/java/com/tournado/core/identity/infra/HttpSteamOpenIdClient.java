package com.tournado.core.identity.infra;

import com.tournado.core.common.security.AuthProperties;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class HttpSteamOpenIdClient implements SteamOpenIdClient {

    private static final String OPENID_NS = "http://specs.openid.net/auth/2.0";
    private static final String IDENTIFIER_SELECT = "http://specs.openid.net/auth/2.0/identifier_select";
    private static final Pattern STEAM_CLAIMED_ID_PATTERN =
            Pattern.compile("^https?://steamcommunity\\.com/openid/id/(7656119[0-9]{10})$");

    private final AuthProperties authProperties;
    private final RestClient restClient;

    @Autowired
    public HttpSteamOpenIdClient(AuthProperties authProperties) {
        this(authProperties, RestClient.create());
    }

    HttpSteamOpenIdClient(AuthProperties authProperties, RestClient restClient) {
        this.authProperties = authProperties;
        this.restClient = restClient;
    }

    @Override
    public String buildLoginRedirectUrl(String returnToUrl, String realmUrl) {
        return UriComponentsBuilder.fromHttpUrl(authProperties.steamOpenidEndpoint())
                .queryParam("openid.ns", OPENID_NS)
                .queryParam("openid.mode", "checkid_setup")
                .queryParam("openid.return_to", returnToUrl)
                .queryParam("openid.realm", realmUrl)
                .queryParam("openid.identity", IDENTIFIER_SELECT)
                .queryParam("openid.claimed_id", IDENTIFIER_SELECT)
                .encode()
                .toUriString();
    }

    @Override
    public Optional<String> verifyAndExtractSteamId(Map<String, String> params, String expectedReturnToUrl) {
        if (params == null || !"id_res".equals(params.get("openid.mode"))) {
            return Optional.empty();
        }
        if (!isValidReturnTo(params.get("openid.return_to"), expectedReturnToUrl)) {
            return Optional.empty();
        }

        Optional<String> steamId = extractSteamId(params.get("openid.claimed_id"));
        if (steamId.isEmpty()) {
            return Optional.empty();
        }

        MultiValueMap<String, String> verificationPayload = new LinkedMultiValueMap<>();
        params.forEach((key, value) -> {
            if (key != null && key.startsWith("openid.") && value != null) {
                verificationPayload.add(key, value);
            }
        });
        verificationPayload.set("openid.mode", "check_authentication");

        try {
            String responseBody = restClient.post()
                    .uri(authProperties.steamOpenidEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(verificationPayload)
                    .retrieve()
                    .body(String.class);

            if (!isCheckAuthenticationValid(responseBody)) {
                return Optional.empty();
            }
            return steamId;
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private boolean isValidReturnTo(String returnTo, String expectedReturnToUrl) {
        if (returnTo == null || expectedReturnToUrl == null) {
            return false;
        }
        return returnTo.equals(expectedReturnToUrl) || returnTo.startsWith(expectedReturnToUrl + "?");
    }

    private Optional<String> extractSteamId(String claimedId) {
        if (claimedId == null) {
            return Optional.empty();
        }
        Matcher matcher = STEAM_CLAIMED_ID_PATTERN.matcher(claimedId.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1));
    }

    private boolean isCheckAuthenticationValid(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        return Arrays.stream(responseBody.split("\\R"))
                .map(String::trim)
                .anyMatch("is_valid:true"::equals);
    }
}

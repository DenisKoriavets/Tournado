package com.tournado.core.identity.service;

import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.SteamAuthFailedException;
import com.tournado.core.identity.infra.SteamOpenIdClient;
import com.tournado.core.identity.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SteamAuthService {

    private static final String STEAM_CALLBACK_PATH = "/api/v1/auth/steam/callback";
    private static final int STEAM_SUFFIX_LENGTH = 6;

    private final SteamOpenIdClient steamOpenIdClient;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final Clock clock;

    public SteamAuthService(
            SteamOpenIdClient steamOpenIdClient,
            UserRepository userRepository,
            AuthProperties authProperties,
            Clock clock) {
        this.steamOpenIdClient = steamOpenIdClient;
        this.userRepository = userRepository;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    public String buildSteamLoginRedirectUrl() {
        String realmUrl = trimTrailingSlash(authProperties.publicBaseUrl());
        String returnToUrl = realmUrl + STEAM_CALLBACK_PATH;
        return steamOpenIdClient.buildLoginRedirectUrl(returnToUrl, realmUrl);
    }

    @Transactional
    public AuthenticatedUser handleSteamCallback(Map<String, String> queryParams) {
        String expectedReturnToUrl = trimTrailingSlash(authProperties.publicBaseUrl()) + STEAM_CALLBACK_PATH;
        String steamId = steamOpenIdClient.verifyAndExtractSteamId(queryParams, expectedReturnToUrl)
                .orElseThrow(SteamAuthFailedException::new);

        UserEntity user = userRepository.findBySteamId(steamId)
                .orElseGet(() -> createSteamUser(steamId));

        return new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail(), user.getSteamId());
    }

    public boolean isCancelled(Map<String, String> queryParams) {
        return queryParams != null && "cancel".equals(queryParams.get("openid.mode"));
    }

    public String getCancelledRedirectUrl() {
        return trimTrailingSlash(authProperties.publicBaseUrl()) + "/?authError=steam_cancelled";
    }

    public String getSuccessRedirectUrl() {
        return trimTrailingSlash(authProperties.publicBaseUrl()) + "/";
    }

    private UserEntity createSteamUser(String steamId) {
        Instant now = Instant.now(clock);
        UserEntity user = new UserEntity();
        user.setSteamId(steamId);
        user.setDisplayName("Steam_" + steamId.substring(steamId.length() - STEAM_SUFFIX_LENGTH));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

package com.tournado.core.identity.infra;

import java.util.Map;
import java.util.Optional;

public interface SteamOpenIdClient {

    String buildLoginRedirectUrl(String returnToUrl, String realmUrl);

    Optional<String> verifyAndExtractSteamId(Map<String, String> params, String expectedReturnToUrl);
}

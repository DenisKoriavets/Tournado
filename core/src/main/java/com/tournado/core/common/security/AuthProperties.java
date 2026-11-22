package com.tournado.core.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tournado.auth")
public record AuthProperties(
        long emailMagicLinkTtlMinutes,
        String publicBaseUrl,
        String steamOpenidEndpoint
) {
}

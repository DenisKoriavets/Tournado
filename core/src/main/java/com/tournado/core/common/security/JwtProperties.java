package com.tournado.core.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tournado.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds,
        String cookieName
) {
}

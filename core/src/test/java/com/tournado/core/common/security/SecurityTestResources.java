package com.tournado.core.common.security;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseCookie;

public final class SecurityTestResources {

    private SecurityTestResources() {
    }

    public static final UUID USER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final String DISPLAY_NAME = "CyberHawk";
    public static final String EMAIL = "student@ukma.edu.ua";
    public static final String STEAM_ID = "76561198012345678";
    public static final Instant FIXED_NOW = Instant.parse("2026-11-22T12:00:00Z");
    public static final String JWT_SECRET = "tournado-test-secret-key-at-least-32-chars-long-2026";
    public static final long EXPIRATION_SECONDS = 604800L;
    public static final String COOKIE_NAME = "X-Auth-Token";

    public static JwtProperties buildJwtProperties() {
        return new JwtProperties(JWT_SECRET, EXPIRATION_SECONDS, COOKIE_NAME);
    }

    public static AuthenticatedUser buildAuthenticatedUser() {
        return new AuthenticatedUser(USER_ID, DISPLAY_NAME, EMAIL, STEAM_ID);
    }

    public static AuthenticatedUser buildAuthenticatedUserWithEmailOnly() {
        return new AuthenticatedUser(USER_ID, DISPLAY_NAME, EMAIL, null);
    }

    public static ResponseCookie buildExpectedSessionCookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(EXPIRATION_SECONDS)
                .build();
    }

    public static ResponseCookie buildExpectedClearedCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0L)
                .build();
    }

    public static ProblemDetail buildUnauthorizedProblemDetail(String instanceUri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication via Steam or Email magic link is required to perform this action."
        );
        problem.setType(URI.create("https://tournado.ua/errors/AUTH_UNAUTHORIZED"));
        problem.setTitle("Unauthorized");
        problem.setInstance(URI.create(instanceUri));
        problem.setProperty("code", "AUTH_UNAUTHORIZED");
        return problem;
    }

    public static ProblemDetail buildSessionExpiredProblemDetail(String instanceUri) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "JWT session token in HttpOnly cookie has expired."
        );
        problem.setType(URI.create("https://tournado.ua/errors/AUTH_SESSION_EXPIRED"));
        problem.setTitle("Unauthorized");
        problem.setInstance(URI.create(instanceUri));
        problem.setProperty("code", "AUTH_SESSION_EXPIRED");
        return problem;
    }
}

package com.tournado.core.common.security;

import com.tournado.core.common.exception.SessionExpiredException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static com.tournado.core.common.security.SecurityTestResources.*;
import static org.assertj.core.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(buildJwtProperties(), Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
    }

    @Test
    void givenValidUser_generateAndParseToken_shouldReturnAuthenticatedUser() {
        AuthenticatedUser expected = buildAuthenticatedUser();
        String token = jwtTokenService.generateToken(expected);

        Optional<AuthenticatedUser> actual = jwtTokenService.parseToken(token);

        assertThat(actual).isPresent();
        assertThat(actual.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenUserWithoutSteamId_generateAndParseToken_shouldPreserveNullFields() {
        AuthenticatedUser expected = buildAuthenticatedUserWithEmailOnly();
        String token = jwtTokenService.generateToken(expected);

        Optional<AuthenticatedUser> actual = jwtTokenService.parseToken(token);

        assertThat(actual).isPresent();
        assertThat(actual.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExpiredToken_parseToken_shouldThrowSessionExpiredException() {
        String token = jwtTokenService.generateToken(buildAuthenticatedUser());
        Clock expiredClock = Clock.fixed(FIXED_NOW.plusSeconds(EXPIRATION_SECONDS + 1L), ZoneOffset.UTC);
        JwtTokenService expiredClockService = new JwtTokenService(buildJwtProperties(), expiredClock);

        assertThatThrownBy(() -> expiredClockService.parseToken(token))
                .isInstanceOf(SessionExpiredException.class)
                .hasMessage("JWT session token in HttpOnly cookie has expired.");
    }

    @Test
    void givenTamperedToken_parseToken_shouldReturnEmptyOptional() {
        String token = jwtTokenService.generateToken(buildAuthenticatedUser());
        String tamperedToken = token.substring(0, token.length() - 4) + "abcd";

        Optional<AuthenticatedUser> actual = jwtTokenService.parseToken(tamperedToken);

        assertThat(actual).isEmpty();
    }

    @Test
    void givenBlankOrNullToken_parseToken_shouldReturnEmptyOptional() {
        Optional<AuthenticatedUser> nullResult = jwtTokenService.parseToken(null);
        Optional<AuthenticatedUser> blankResult = jwtTokenService.parseToken("   ");

        assertThat(nullResult).isEmpty();
        assertThat(blankResult).isEmpty();
    }

    @Test
    void givenToken_createSessionCookie_shouldReturnHttpOnlyLaxCookie() {
        String token = "sample-jwt-token";
        ResponseCookie expected = buildExpectedSessionCookie(token);

        ResponseCookie actual = jwtTokenService.createSessionCookie(token);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenClearRequest_clearSessionCookie_shouldReturnZeroMaxAgeCookie() {
        ResponseCookie expected = buildExpectedClearedCookie();

        ResponseCookie actual = jwtTokenService.clearSessionCookie();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}

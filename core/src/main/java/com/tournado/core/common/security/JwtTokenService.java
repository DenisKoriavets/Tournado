package com.tournado.core.common.security;

import com.tournado.core.common.exception.SessionExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_STEAM_ID = "steamId";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now(clock);
        Instant expiry = now.plusSeconds(jwtProperties.expirationSeconds());

        return Jwts.builder()
                .subject(user.userId().toString())
                .claim(CLAIM_DISPLAY_NAME, user.displayName())
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_STEAM_ID, user.steamId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AuthenticatedUser> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());
            String displayName = claims.get(CLAIM_DISPLAY_NAME, String.class);
            String email = claims.get(CLAIM_EMAIL, String.class);
            String steamId = claims.get(CLAIM_STEAM_ID, String.class);
            return Optional.of(new AuthenticatedUser(userId, displayName, email, steamId));
        } catch (ExpiredJwtException ex) {
            throw new SessionExpiredException();
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public ResponseCookie createSessionCookie(String token) {
        return ResponseCookie.from(jwtProperties.cookieName(), token)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(jwtProperties.expirationSeconds())
                .build();
    }

    public ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(jwtProperties.cookieName(), "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0L)
                .build();
    }
}

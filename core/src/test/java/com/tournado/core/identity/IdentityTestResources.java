package com.tournado.core.identity;

import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.api.dto.EmailLoginRequest;
import com.tournado.core.identity.api.dto.MessageResponse;
import com.tournado.core.identity.api.dto.UpdateGameIdentifiersRequest;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.domain.EmailLoginTokenEntity;
import com.tournado.core.identity.domain.GameIdentifierValidator.NormalizedGameIdentifiers;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.service.UserProfileSummary;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class IdentityTestResources {

    private IdentityTestResources() {
    }

    public static final UUID USER_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID SECOND_USER_ID = UUID.fromString("a1b2c3d4-e5f6-4789-9012-112233445566");
    public static final UUID TOKEN_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    public static final String USER_EMAIL = "student@ukma.edu.ua";
    public static final String USER_DISPLAY_NAME = "student";
    public static final String STEAM_ID = "76561198012345678";
    public static final String STEAM_DISPLAY_NAME = "Steam_345678";
    public static final String SUPERCELL_TAG = "#2PP98L9";
    public static final String CHESS_USERNAME = "cyber_master";
    public static final String AVATAR_URL = "https://avatars.steamstatic.com/sample.jpg";
    public static final String PUBLIC_BASE_URL = "http://localhost:3000";
    public static final String STEAM_OPENID_ENDPOINT = "https://steamcommunity.com/openid/login";
    public static final long MAGIC_LINK_TTL_MINUTES = 15L;
    public static final Instant FIXED_NOW = Instant.parse("2026-11-22T12:00:00Z");
    public static final String RAW_MAGIC_TOKEN = "valid-raw-magic-token-value";
    public static final String RAW_MAGIC_TOKEN_SHA256 =
            "f123cd09703759d79dff7c2abf3bbf98c24b03d030ff5cd25ea5ea86e54169d8";

    public static AuthProperties buildAuthProperties() {
        return new AuthProperties(MAGIC_LINK_TTL_MINUTES, PUBLIC_BASE_URL, STEAM_OPENID_ENDPOINT);
    }

    public static UserEntity buildEmailUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setDisplayName(USER_DISPLAY_NAME);
        user.setSteamId(STEAM_ID);
        user.setSupercellPlayerTag(SUPERCELL_TAG);
        user.setChessUsername(CHESS_USERNAME);
        user.setAvatarUrl(AVATAR_URL);
        user.setCreatedAt(FIXED_NOW);
        user.setUpdatedAt(FIXED_NOW);
        return user;
    }

    public static UserEntity buildEmailOnlyUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail(USER_EMAIL);
        user.setDisplayName(USER_DISPLAY_NAME);
        user.setCreatedAt(FIXED_NOW);
        user.setUpdatedAt(FIXED_NOW);
        return user;
    }

    public static UserEntity buildSteamOnlyUserEntity() {
        UserEntity user = new UserEntity();
        user.setId(SECOND_USER_ID);
        user.setSteamId(STEAM_ID);
        user.setDisplayName(STEAM_DISPLAY_NAME);
        user.setCreatedAt(FIXED_NOW);
        user.setUpdatedAt(FIXED_NOW);
        return user;
    }

    public static EmailLoginTokenEntity buildValidEmailTokenEntity(String tokenHash) {
        EmailLoginTokenEntity entity = new EmailLoginTokenEntity();
        entity.setId(TOKEN_ID);
        entity.setEmail(USER_EMAIL);
        entity.setTokenHash(tokenHash);
        entity.setCreatedAt(FIXED_NOW.minus(5, ChronoUnit.MINUTES));
        entity.setExpiresAt(FIXED_NOW.plus(10, ChronoUnit.MINUTES));
        return entity;
    }

    public static EmailLoginTokenEntity buildExpiredEmailTokenEntity(String tokenHash) {
        EmailLoginTokenEntity entity = buildValidEmailTokenEntity(tokenHash);
        entity.setExpiresAt(FIXED_NOW.minusSeconds(1L));
        return entity;
    }

    public static EmailLoginTokenEntity buildUsedEmailTokenEntity(String tokenHash) {
        EmailLoginTokenEntity entity = buildValidEmailTokenEntity(tokenHash);
        entity.setUsedAt(FIXED_NOW.minusSeconds(30L));
        return entity;
    }

    public static AuthenticatedUser buildAuthenticatedEmailUser() {
        return new AuthenticatedUser(USER_ID, USER_DISPLAY_NAME, USER_EMAIL, STEAM_ID);
    }

    public static AuthenticatedUser buildAuthenticatedSteamUser() {
        return new AuthenticatedUser(SECOND_USER_ID, STEAM_DISPLAY_NAME, null, STEAM_ID);
    }

    public static EmailLoginRequest buildValidEmailLoginRequest() {
        return new EmailLoginRequest(USER_EMAIL);
    }

    public static MessageResponse buildExpectedEmailLinkMessageResponse() {
        return new MessageResponse("If the provided email is valid, a login link has been dispatched.");
    }

    public static UpdateGameIdentifiersRequest buildValidUpdateGameIdentifiersRequest() {
        return new UpdateGameIdentifiersRequest("  " + STEAM_ID + " ", " #2pp98l9 ", " " + CHESS_USERNAME + " ");
    }

    public static UpdateGameIdentifiersRequest buildClearGameIdentifiersRequest() {
        return new UpdateGameIdentifiersRequest("", "   ", null);
    }

    public static UpdateGameIdentifiersRequest buildInvalidGameIdentifiersRequest() {
        return new UpdateGameIdentifiersRequest("12345", "invalid", "a");
    }

    public static NormalizedGameIdentifiers buildExpectedNormalizedIdentifiers() {
        return new NormalizedGameIdentifiers(STEAM_ID, SUPERCELL_TAG, CHESS_USERNAME);
    }

    public static UserProfileResponse buildExpectedUserProfileResponse() {
        return new UserProfileResponse(
                USER_ID,
                USER_EMAIL,
                STEAM_ID,
                USER_DISPLAY_NAME,
                SUPERCELL_TAG,
                CHESS_USERNAME,
                AVATAR_URL
        );
    }

    public static UserProfileResponse buildClearedUserProfileResponse() {
        return new UserProfileResponse(
                USER_ID,
                USER_EMAIL,
                null,
                USER_DISPLAY_NAME,
                null,
                null,
                AVATAR_URL
        );
    }

    public static UserProfileSummary buildExpectedUserProfileSummary() {
        return new UserProfileSummary(
                USER_ID,
                USER_EMAIL,
                STEAM_ID,
                USER_DISPLAY_NAME,
                SUPERCELL_TAG,
                CHESS_USERNAME,
                AVATAR_URL
        );
    }

    public static List<InvalidParam> buildExpectedAllInvalidGameIdentifierParams() {
        return List.of(
                new InvalidParam("steamId", "Steam ID must be a 17-digit SteamID64 starting with 7656119."),
                new InvalidParam(
                        "supercellPlayerTag",
                        "Supercell player tag must start with '#' followed by 3 to 14 alphanumeric characters."
                ),
                new InvalidParam(
                        "chessUsername",
                        "Chess.com username must be 3-25 characters using letters, digits, underscores, or hyphens."
                )
        );
    }

    public static Map<String, String> buildValidSteamCallbackParams() {
        return Map.of(
                "openid.ns", "http://specs.openid.net/auth/2.0",
                "openid.mode", "id_res",
                "openid.return_to", PUBLIC_BASE_URL + "/api/v1/auth/steam/callback",
                "openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID,
                "openid.identity", "https://steamcommunity.com/openid/id/" + STEAM_ID,
                "openid.sig", "signed-payload"
        );
    }

    public static ProblemDetail buildSteamAuthFailedProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Verification of Steam OpenID response signature failed."
        );
        problem.setType(URI.create("https://tournado.ua/errors/STEAM_AUTH_FAILED"));
        problem.setTitle("Unauthorized");
        problem.setInstance(URI.create("/api/v1/auth/steam/callback"));
        problem.setProperty("code", "STEAM_AUTH_FAILED");
        return problem;
    }

    public static ProblemDetail buildEmailMagicLinkExpiredProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The requested one-time login link has expired or has already been used."
        );
        problem.setType(URI.create("https://tournado.ua/errors/EMAIL_MAGIC_LINK_EXPIRED"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create("/api/v1/auth/email/verify"));
        problem.setProperty("code", "EMAIL_MAGIC_LINK_EXPIRED");
        return problem;
    }

    public static ProblemDetail buildInvalidGameIdentifierProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "One or more game identifiers have an invalid format."
        );
        problem.setType(URI.create("https://tournado.ua/errors/INVALID_GAME_IDENTIFIER"));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create("/api/v1/users/me/game-identifiers"));
        problem.setProperty("code", "INVALID_GAME_IDENTIFIER");
        problem.setProperty("invalidParams", List.of(
                Map.of("name", "steamId", "reason", "Steam ID must be a 17-digit SteamID64 starting with 7656119."),
                Map.of(
                        "name", "supercellPlayerTag",
                        "reason", "Supercell player tag must start with '#' followed by 3 to 14 alphanumeric characters."
                ),
                Map.of(
                        "name", "chessUsername",
                        "reason", "Chess.com username must be 3-25 characters using letters, digits, underscores, or hyphens."
                )
        ));
        return problem;
    }

    public static ProblemDetail buildInvalidEmailProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body contains 1 validation constraint violations"
        );
        problem.setType(URI.create("https://tournado.ua/errors/VALIDATION_FAILED"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create("/api/v1/auth/email/request-link"));
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("invalidParams", List.of(
                Map.of("name", "email", "reason", "Email must be a valid email address")
        ));
        return problem;
    }
}

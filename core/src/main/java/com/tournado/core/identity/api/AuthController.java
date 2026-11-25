package com.tournado.core.identity.api;

import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.identity.api.dto.EmailLoginRequest;
import com.tournado.core.identity.api.dto.MessageResponse;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.service.EmailMagicLinkService;
import com.tournado.core.identity.service.SteamAuthService;
import com.tournado.core.identity.service.UserProfileService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String EMAIL_REQUEST_ACCEPTED_MESSAGE =
            "If the provided email is valid, a login link has been dispatched.";

    private final SteamAuthService steamAuthService;
    private final EmailMagicLinkService emailMagicLinkService;
    private final UserProfileService userProfileService;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final AuthProperties authProperties;

    public AuthController(
            SteamAuthService steamAuthService,
            EmailMagicLinkService emailMagicLinkService,
            UserProfileService userProfileService,
            JwtTokenService jwtTokenService,
            CurrentUserProvider currentUserProvider,
            AuthProperties authProperties) {
        this.steamAuthService = steamAuthService;
        this.emailMagicLinkService = emailMagicLinkService;
        this.userProfileService = userProfileService;
        this.jwtTokenService = jwtTokenService;
        this.currentUserProvider = currentUserProvider;
        this.authProperties = authProperties;
    }

    @GetMapping("/steam/login")
    public ResponseEntity<Void> steamLogin() {
        URI redirectUri = URI.create(steamAuthService.buildSteamLoginRedirectUrl());
        return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
    }

    @GetMapping("/steam/callback")
    public ResponseEntity<Void> steamCallback(@RequestParam Map<String, String> queryParams) {
        if (steamAuthService.isCancelled(queryParams)) {
            URI cancelledUri = URI.create(steamAuthService.getCancelledRedirectUrl());
            return ResponseEntity.status(HttpStatus.FOUND).location(cancelledUri).build();
        }
        AuthenticatedUser authenticatedUser = steamAuthService.handleSteamCallback(queryParams);
        return issueSessionRedirect(authenticatedUser);
    }

    @PostMapping("/email/request-link")
    public ResponseEntity<MessageResponse> requestEmailLink(@Valid @RequestBody EmailLoginRequest request) {
        emailMagicLinkService.requestLoginLink(request.email());
        return ResponseEntity.ok(new MessageResponse(EMAIL_REQUEST_ACCEPTED_MESSAGE));
    }

    @GetMapping("/email/verify")
    public ResponseEntity<Void> verifyEmailLink(@RequestParam(name = "token", required = false) String token) {
        AuthenticatedUser authenticatedUser = emailMagicLinkService.verifyLoginToken(token);
        return issueSessionRedirect(authenticatedUser);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(userProfileService.getCurrentUserProfile(currentUser.userId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie clearedCookie = jwtTokenService.clearSessionCookie();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedCookie.toString())
                .build();
    }

    private ResponseEntity<Void> issueSessionRedirect(AuthenticatedUser authenticatedUser) {
        String jwt = jwtTokenService.generateToken(authenticatedUser);
        ResponseCookie sessionCookie = jwtTokenService.createSessionCookie(jwt);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .location(URI.create(buildSuccessRedirectUrl()))
                .build();
    }

    private String buildSuccessRedirectUrl() {
        String baseUrl = authProperties.publicBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            return baseUrl;
        }
        return baseUrl + "/";
    }
}

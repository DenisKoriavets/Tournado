package com.tournado.core.identity.service;

import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.domain.EmailLoginTokenEntity;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.EmailMagicLinkExpiredException;
import com.tournado.core.identity.infra.MagicLinkMailSender;
import com.tournado.core.identity.repository.EmailLoginTokenRepository;
import com.tournado.core.identity.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailMagicLinkService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailLoginTokenRepository emailLoginTokenRepository;
    private final UserRepository userRepository;
    private final MagicLinkMailSender magicLinkMailSender;
    private final AuthProperties authProperties;
    private final Clock clock;

    public EmailMagicLinkService(
            EmailLoginTokenRepository emailLoginTokenRepository,
            UserRepository userRepository,
            MagicLinkMailSender magicLinkMailSender,
            AuthProperties authProperties,
            Clock clock) {
        this.emailLoginTokenRepository = emailLoginTokenRepository;
        this.userRepository = userRepository;
        this.magicLinkMailSender = magicLinkMailSender;
        this.authProperties = authProperties;
        this.clock = clock;
    }

    @Transactional
    public void requestLoginLink(String rawEmail) {
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        String rawToken = generateRawToken();
        String tokenHash = sha256Hex(rawToken);
        Instant now = Instant.now(clock);

        EmailLoginTokenEntity tokenEntity = new EmailLoginTokenEntity();
        tokenEntity.setEmail(normalizedEmail);
        tokenEntity.setTokenHash(tokenHash);
        tokenEntity.setCreatedAt(now);
        tokenEntity.setExpiresAt(now.plus(authProperties.emailMagicLinkTtlMinutes(), ChronoUnit.MINUTES));
        emailLoginTokenRepository.save(tokenEntity);

        String magicLinkUrl = trimTrailingSlash(authProperties.publicBaseUrl())
                + "/api/v1/auth/email/verify?token="
                + rawToken;
        magicLinkMailSender.sendMagicLink(normalizedEmail, magicLinkUrl);
    }

    @Transactional
    public AuthenticatedUser verifyLoginToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new EmailMagicLinkExpiredException();
        }
        String tokenHash = sha256Hex(rawToken.trim());
        EmailLoginTokenEntity tokenEntity = emailLoginTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(EmailMagicLinkExpiredException::new);

        Instant now = Instant.now(clock);
        if (tokenEntity.getUsedAt() != null || tokenEntity.getExpiresAt().isBefore(now)) {
            throw new EmailMagicLinkExpiredException();
        }

        tokenEntity.setUsedAt(now);
        emailLoginTokenRepository.save(tokenEntity);

        UserEntity user = userRepository.findByEmailIgnoreCase(tokenEntity.getEmail())
                .orElseGet(() -> createEmailUser(tokenEntity.getEmail(), now));

        return new AuthenticatedUser(user.getId(), user.getDisplayName(), user.getEmail(), user.getSteamId());
    }

    private UserEntity createEmailUser(String email, Instant now) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setDisplayName(deriveDisplayNameFromEmail(email));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return userRepository.save(user);
    }

    private String deriveDisplayNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        String localPart = atIndex > 0 ? email.substring(0, atIndex) : "Player";
        if (localPart.length() > MAX_DISPLAY_NAME_LENGTH) {
            return localPart.substring(0, MAX_DISPLAY_NAME_LENGTH);
        }
        return localPart;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    private String trimTrailingSlash(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}

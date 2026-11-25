package com.tournado.core.identity.service;

import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.domain.EmailLoginTokenEntity;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.EmailMagicLinkExpiredException;
import com.tournado.core.identity.infra.MagicLinkMailSender;
import com.tournado.core.identity.repository.EmailLoginTokenRepository;
import com.tournado.core.identity.repository.UserRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.identity.IdentityTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailMagicLinkServiceTest {

    @Mock
    private EmailLoginTokenRepository emailLoginTokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MagicLinkMailSender magicLinkMailSender;

    private EmailMagicLinkService emailMagicLinkService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        emailMagicLinkService = new EmailMagicLinkService(
                emailLoginTokenRepository,
                userRepository,
                magicLinkMailSender,
                buildAuthProperties(),
                fixedClock
        );
    }

    @Test
    void givenValidEmail_requestLoginLink_shouldHashTokenPersistAndSendEmail() {
        when(emailLoginTokenRepository.save(any(EmailLoginTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailMagicLinkService.requestLoginLink("  Student@UKMA.EDU.UA ");

        ArgumentCaptor<EmailLoginTokenEntity> tokenCaptor = ArgumentCaptor.forClass(EmailLoginTokenEntity.class);
        verify(emailLoginTokenRepository).save(tokenCaptor.capture());
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(magicLinkMailSender).sendMagicLink(eq(USER_EMAIL), linkCaptor.capture());
        String sentLink = linkCaptor.getValue();
        assertThat(sentLink).startsWith(PUBLIC_BASE_URL + "/api/v1/auth/email/verify?token=");
        String rawToken = sentLink.substring((PUBLIC_BASE_URL + "/api/v1/auth/email/verify?token=").length());
        EmailLoginTokenEntity savedToken = tokenCaptor.getValue();
        assertThat(savedToken)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(buildExpectedRequestedTokenEntity(EmailMagicLinkService.sha256Hex(rawToken)));
    }

    @Test
    void givenValidTokenAndNewUser_verifyLoginToken_shouldMarkUsedCreateUserAndReturnAuthenticatedUser() {
        EmailLoginTokenEntity tokenEntity = buildValidEmailTokenEntity(RAW_MAGIC_TOKEN_SHA256);
        UserEntity createdUser = buildEmailOnlyUserEntity();
        AuthenticatedUser expectedUser = new AuthenticatedUser(USER_ID, USER_DISPLAY_NAME, USER_EMAIL, null);
        when(emailLoginTokenRepository.findByTokenHash(RAW_MAGIC_TOKEN_SHA256)).thenReturn(Optional.of(tokenEntity));
        when(emailLoginTokenRepository.save(any(EmailLoginTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(createdUser);

        AuthenticatedUser actual = emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN);

        verify(emailLoginTokenRepository).save(tokenEntity);
        assertThat(tokenEntity.getUsedAt()).isEqualTo(FIXED_NOW);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedUser);
    }

    @Test
    void givenValidTokenAndExistingUser_verifyLoginToken_shouldReuseExistingUser() {
        EmailLoginTokenEntity tokenEntity = buildValidEmailTokenEntity(RAW_MAGIC_TOKEN_SHA256);
        UserEntity existingUser = buildEmailUserEntity();
        AuthenticatedUser expectedUser = buildAuthenticatedEmailUser();
        when(emailLoginTokenRepository.findByTokenHash(RAW_MAGIC_TOKEN_SHA256)).thenReturn(Optional.of(tokenEntity));
        when(emailLoginTokenRepository.save(any(EmailLoginTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByEmailIgnoreCase(USER_EMAIL)).thenReturn(Optional.of(existingUser));

        AuthenticatedUser actual = emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN);

        verify(userRepository, never()).save(any());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedUser);
    }

    @Test
    void givenExpiredToken_verifyLoginToken_shouldThrowEmailMagicLinkExpiredException() {
        EmailLoginTokenEntity expiredToken = buildExpiredEmailTokenEntity(RAW_MAGIC_TOKEN_SHA256);
        when(emailLoginTokenRepository.findByTokenHash(RAW_MAGIC_TOKEN_SHA256)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN))
                .isInstanceOf(EmailMagicLinkExpiredException.class)
                .hasMessageContaining("expired or has already been used");
    }

    @Test
    void givenAlreadyUsedToken_verifyLoginToken_shouldThrowEmailMagicLinkExpiredException() {
        EmailLoginTokenEntity usedToken = buildUsedEmailTokenEntity(RAW_MAGIC_TOKEN_SHA256);
        when(emailLoginTokenRepository.findByTokenHash(RAW_MAGIC_TOKEN_SHA256)).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN))
                .isInstanceOf(EmailMagicLinkExpiredException.class);
    }

    @Test
    void givenBlankToken_verifyLoginToken_shouldThrowEmailMagicLinkExpiredException() {
        assertThatThrownBy(() -> emailMagicLinkService.verifyLoginToken("   "))
                .isInstanceOf(EmailMagicLinkExpiredException.class);
        verifyNoInteractions(emailLoginTokenRepository, userRepository);
    }

    private EmailLoginTokenEntity buildExpectedRequestedTokenEntity(String expectedHash) {
        EmailLoginTokenEntity entity = new EmailLoginTokenEntity();
        entity.setEmail(USER_EMAIL);
        entity.setTokenHash(expectedHash);
        entity.setCreatedAt(FIXED_NOW);
        entity.setExpiresAt(FIXED_NOW.plus(MAGIC_LINK_TTL_MINUTES, ChronoUnit.MINUTES));
        return entity;
    }
}

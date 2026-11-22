package com.tournado.core.identity.service;

import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.SteamAuthFailedException;
import com.tournado.core.identity.infra.SteamOpenIdClient;
import com.tournado.core.identity.repository.UserRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
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
class SteamAuthServiceTest {

    @Mock
    private SteamOpenIdClient steamOpenIdClient;

    @Mock
    private UserRepository userRepository;

    private SteamAuthService steamAuthService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        steamAuthService = new SteamAuthService(steamOpenIdClient, userRepository, buildAuthProperties(), fixedClock);
    }

    @Test
    void givenConfiguredPublicUrl_buildSteamLoginRedirectUrl_shouldDelegateWithCallbackAndRealm() {
        String expectedCallback = PUBLIC_BASE_URL + "/api/v1/auth/steam/callback";
        String expectedRedirect = STEAM_OPENID_ENDPOINT + "?openid.mode=checkid_setup";
        when(steamOpenIdClient.buildLoginRedirectUrl(expectedCallback, PUBLIC_BASE_URL)).thenReturn(expectedRedirect);

        String actual = steamAuthService.buildSteamLoginRedirectUrl();

        assertThat(actual).isEqualTo(expectedRedirect);
    }

    @Test
    void givenValidSteamCallbackForNewUser_handleSteamCallback_shouldCreateUserAndReturnAuthenticatedUser() {
        Map<String, String> params = buildValidSteamCallbackParams();
        UserEntity savedUser = buildSteamOnlyUserEntity();
        AuthenticatedUser expected = buildAuthenticatedSteamUser();
        when(steamOpenIdClient.verifyAndExtractSteamId(params, PUBLIC_BASE_URL + "/api/v1/auth/steam/callback"))
                .thenReturn(Optional.of(STEAM_ID));
        when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        AuthenticatedUser actual = steamAuthService.handleSteamCallback(params);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(savedUser);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenValidSteamCallbackForExistingUser_handleSteamCallback_shouldReuseExistingUser() {
        Map<String, String> params = buildValidSteamCallbackParams();
        UserEntity existingUser = buildSteamOnlyUserEntity();
        AuthenticatedUser expected = buildAuthenticatedSteamUser();
        when(steamOpenIdClient.verifyAndExtractSteamId(params, PUBLIC_BASE_URL + "/api/v1/auth/steam/callback"))
                .thenReturn(Optional.of(STEAM_ID));
        when(userRepository.findBySteamId(STEAM_ID)).thenReturn(Optional.of(existingUser));

        AuthenticatedUser actual = steamAuthService.handleSteamCallback(params);

        verify(userRepository, never()).save(any());
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidSteamSignature_handleSteamCallback_shouldThrowSteamAuthFailedException() {
        Map<String, String> params = buildValidSteamCallbackParams();
        when(steamOpenIdClient.verifyAndExtractSteamId(params, PUBLIC_BASE_URL + "/api/v1/auth/steam/callback"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> steamAuthService.handleSteamCallback(params))
                .isInstanceOf(SteamAuthFailedException.class)
                .hasMessageContaining("Steam OpenID");
    }

    @Test
    void givenCancelOpenIdMode_isCancelled_shouldReturnTrue() {
        Map<String, String> params = Map.of("openid.mode", "cancel");

        boolean cancelled = steamAuthService.isCancelled(params);

        assertThat(cancelled).isTrue();
    }
}

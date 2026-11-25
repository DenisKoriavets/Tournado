package com.tournado.core.common.security;

import com.tournado.core.common.exception.UnauthorizedException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.tournado.core.common.security.SecurityTestResources.*;
import static org.assertj.core.api.Assertions.*;

class SecurityContextCurrentUserProviderTest {

    private SecurityContextCurrentUserProvider currentUserProvider;

    @BeforeEach
    void setUp() {
        currentUserProvider = new SecurityContextCurrentUserProvider();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAuthenticatedUserInContext_findCurrentUser_shouldReturnUser() {
        AuthenticatedUser expected = buildAuthenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(expected, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        Optional<AuthenticatedUser> actual = currentUserProvider.findCurrentUser();

        assertThat(actual).isPresent();
        assertThat(actual.get()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenAuthenticatedUserInContext_requireCurrentUser_shouldReturnUser() {
        AuthenticatedUser expected = buildAuthenticatedUser();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(expected, null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );

        AuthenticatedUser actual = currentUserProvider.requireCurrentUser();

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenEmptySecurityContext_findCurrentUser_shouldReturnEmptyOptional() {
        Optional<AuthenticatedUser> actual = currentUserProvider.findCurrentUser();

        assertThat(actual).isEmpty();
    }

    @Test
    void givenEmptySecurityContext_requireCurrentUser_shouldThrowUnauthorizedException() {
        assertThatThrownBy(() -> currentUserProvider.requireCurrentUser())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication via Steam or Email magic link is required to perform this action.");
    }

    @Test
    void givenUnauthenticatedToken_findCurrentUser_shouldReturnEmptyOptional() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(buildAuthenticatedUser(), null)
        );

        Optional<AuthenticatedUser> actual = currentUserProvider.findCurrentUser();

        assertThat(actual).isEmpty();
    }

    @Test
    void givenAnonymousPrincipal_requireCurrentUser_shouldThrowUnauthorizedException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
        );

        assertThatThrownBy(() -> currentUserProvider.requireCurrentUser())
                .isInstanceOf(UnauthorizedException.class);
    }
}

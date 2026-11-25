package com.tournado.core.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.identity.api.dto.EmailLoginRequest;
import com.tournado.core.identity.api.dto.MessageResponse;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.exception.EmailMagicLinkExpiredException;
import com.tournado.core.identity.exception.SteamAuthFailedException;
import com.tournado.core.identity.service.EmailMagicLinkService;
import com.tournado.core.identity.service.SteamAuthService;
import com.tournado.core.identity.service.UserProfileService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.tournado.core.common.security.SecurityTestResources.buildUnauthorizedProblemDetail;
import static com.tournado.core.identity.IdentityTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private SteamAuthService steamAuthService;

    @MockBean
    private EmailMagicLinkService emailMagicLinkService;

    @MockBean
    private UserProfileService userProfileService;

    @Test
    void givenAnonymousUser_getCurrentUser_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/auth/me");

        MvcResult result = mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(userProfileService);
    }

    @Test
    void givenAuthenticatedUser_getCurrentUser_shouldReturn200WithProfile() throws Exception {
        AuthenticatedUser authUser = buildAuthenticatedEmailUser();
        String token = jwtTokenService.generateToken(authUser);
        UserProfileResponse expected = buildExpectedUserProfileResponse();
        when(userProfileService.getCurrentUserProfile(USER_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/auth/me")
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        UserProfileResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenValidEmailRequest_requestEmailLink_shouldReturn200WithMessage() throws Exception {
        EmailLoginRequest request = buildValidEmailLoginRequest();
        MessageResponse expected = buildExpectedEmailLinkMessageResponse();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/email/request-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        verify(emailMagicLinkService).requestLoginLink(USER_EMAIL);
        MessageResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), MessageResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidEmailRequest_requestEmailLink_shouldReturn400ValidationProblemDetail() throws Exception {
        EmailLoginRequest request = new EmailLoginRequest("not-an-email");
        ProblemDetail expected = buildInvalidEmailProblemDetail();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/email/request-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(emailMagicLinkService);
    }

    @Test
    void givenValidMagicToken_verifyEmailLink_shouldSetSessionCookieAndRedirect() throws Exception {
        AuthenticatedUser authUser = buildAuthenticatedEmailUser();
        when(emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN)).thenReturn(authUser);

        MvcResult result = mockMvc.perform(get("/api/v1/auth/email/verify")
                .param("token", RAW_MAGIC_TOKEN))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, PUBLIC_BASE_URL + "/"))
                .andExpect(cookie().httpOnly(jwtProperties.cookieName(), true))
                .andExpect(cookie().path(jwtProperties.cookieName(), "/"))
                .andReturn();

        String cookieValue = result.getResponse().getCookie(jwtProperties.cookieName()).getValue();
        assertThat(jwtTokenService.parseToken(cookieValue)).contains(authUser);
    }

    @Test
    void givenExpiredMagicToken_verifyEmailLink_shouldReturn400ProblemDetail() throws Exception {
        ProblemDetail expected = buildEmailMagicLinkExpiredProblemDetail();
        when(emailMagicLinkService.verifyLoginToken(RAW_MAGIC_TOKEN)).thenThrow(new EmailMagicLinkExpiredException());

        MvcResult result = mockMvc.perform(get("/api/v1/auth/email/verify")
                .param("token", RAW_MAGIC_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenSteamLoginRequest_steamLogin_shouldReturn302LocationToSteam() throws Exception {
        String steamRedirectUrl = STEAM_OPENID_ENDPOINT + "?openid.mode=checkid_setup";
        when(steamAuthService.buildSteamLoginRedirectUrl()).thenReturn(steamRedirectUrl);

        mockMvc.perform(get("/api/v1/auth/steam/login"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, steamRedirectUrl));
    }

    @Test
    void givenValidSteamCallback_steamCallback_shouldSetSessionCookieAndRedirect() throws Exception {
        AuthenticatedUser authUser = buildAuthenticatedSteamUser();
        when(steamAuthService.isCancelled(anyMap())).thenReturn(false);
        when(steamAuthService.handleSteamCallback(anyMap())).thenReturn(authUser);

        MvcResult result = mockMvc.perform(get("/api/v1/auth/steam/callback")
                .param("openid.mode", "id_res")
                .param("openid.claimed_id", "https://steamcommunity.com/openid/id/" + STEAM_ID))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, PUBLIC_BASE_URL + "/"))
                .andExpect(cookie().httpOnly(jwtProperties.cookieName(), true))
                .andReturn();

        String cookieValue = result.getResponse().getCookie(jwtProperties.cookieName()).getValue();
        assertThat(jwtTokenService.parseToken(cookieValue)).contains(authUser);
    }

    @Test
    void givenCancelledSteamCallback_steamCallback_shouldRedirectWithAuthErrorAndNoCookie() throws Exception {
        String cancelledUrl = PUBLIC_BASE_URL + "/?authError=steam_cancelled";
        when(steamAuthService.isCancelled(anyMap())).thenReturn(true);
        when(steamAuthService.getCancelledRedirectUrl()).thenReturn(cancelledUrl);

        mockMvc.perform(get("/api/v1/auth/steam/callback")
                .param("openid.mode", "cancel"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, cancelledUrl))
                .andExpect(cookie().doesNotExist(jwtProperties.cookieName()));
    }

    @Test
    void givenFailedSteamCallback_steamCallback_shouldReturn401SteamAuthFailedProblemDetail() throws Exception {
        ProblemDetail expected = buildSteamAuthFailedProblemDetail();
        when(steamAuthService.isCancelled(anyMap())).thenReturn(false);
        when(steamAuthService.handleSteamCallback(anyMap())).thenThrow(new SteamAuthFailedException());

        MvcResult result = mockMvc.perform(get("/api/v1/auth/steam/callback")
                .param("openid.mode", "id_res"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenLogoutRequest_logout_shouldClearSessionCookieAndReturn204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(jwtProperties.cookieName(), 0))
                .andExpect(cookie().httpOnly(jwtProperties.cookieName(), true));
    }

    private void assertProblemDetailMatches(MvcResult result, ProblemDetail expected) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}

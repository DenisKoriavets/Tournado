package com.tournado.core.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.identity.api.dto.UpdateGameIdentifiersRequest;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.exception.InvalidGameIdentifierException;
import com.tournado.core.identity.service.UserProfileService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(controllers = UserProfileController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private UserProfileService userProfileService;

    @Test
    void givenAnonymousUser_updateGameIdentifiers_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        UpdateGameIdentifiersRequest request = buildValidUpdateGameIdentifiersRequest();
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/users/me/game-identifiers");

        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/game-identifiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(userProfileService);
    }

    @Test
    void givenAuthenticatedUserAndValidPayload_updateGameIdentifiers_shouldReturn200WithUpdatedProfile() throws Exception {
        UpdateGameIdentifiersRequest request = buildValidUpdateGameIdentifiersRequest();
        UserProfileResponse expected = buildExpectedUserProfileResponse();
        String token = jwtTokenService.generateToken(buildAuthenticatedEmailUser());
        when(userProfileService.updateGameIdentifiers(USER_ID, request)).thenReturn(expected);

        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/game-identifiers")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        UserProfileResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenInvalidGameIdentifiers_updateGameIdentifiers_shouldReturn422ProblemDetailWithInvalidParams() throws Exception {
        UpdateGameIdentifiersRequest request = buildInvalidGameIdentifiersRequest();
        ProblemDetail expected = buildInvalidGameIdentifierProblemDetail();
        String token = jwtTokenService.generateToken(buildAuthenticatedEmailUser());
        when(userProfileService.updateGameIdentifiers(USER_ID, request))
                .thenThrow(new InvalidGameIdentifierException(buildExpectedAllInvalidGameIdentifierParams()));

        MvcResult result = mockMvc.perform(patch("/api/v1/users/me/game-identifiers")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    private void assertProblemDetailMatches(MvcResult result, ProblemDetail expected) throws Exception {
        ProblemDetail actual = objectMapper.readValue(result.getResponse().getContentAsString(), ProblemDetail.class);
        assertThat(actual.getProperties()).containsKey("timestamp");
        actual.getProperties().remove("timestamp");
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}

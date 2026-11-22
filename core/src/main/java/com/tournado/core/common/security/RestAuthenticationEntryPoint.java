package com.tournado.core.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String AUTH_ERROR_CODE_ATTR = "tournado.auth.errorCode";
    public static final String AUTH_ERROR_DETAIL_ATTR = "tournado.auth.errorDetail";

    private static final String ERROR_BASE_URL = "https://tournado.ua/errors/";
    private static final String DEFAULT_ERROR_CODE = "AUTH_UNAUTHORIZED";
    private static final String DEFAULT_DETAIL =
            "Authentication via Steam or Email magic link is required to perform this action.";

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String code = resolveAttribute(request, AUTH_ERROR_CODE_ATTR, DEFAULT_ERROR_CODE);
        String detail = resolveAttribute(request, AUTH_ERROR_DETAIL_ATTR, DEFAULT_DETAIL);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
        problem.setType(URI.create(ERROR_BASE_URL + code));
        problem.setTitle(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now(clock));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private String resolveAttribute(HttpServletRequest request, String attributeName, String fallback) {
        Object value = request.getAttribute(attributeName);
        if (value instanceof String str && !str.isBlank()) {
            return str;
        }
        return fallback;
    }
}

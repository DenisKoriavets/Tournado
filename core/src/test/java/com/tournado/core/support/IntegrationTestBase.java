package com.tournado.core.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.common.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.UUID;

/**
 * Base class for API integration tests (docs/testing-strategy.md, level 2).
 *
 * <p>Every test runs the full Spring context (security filter chain, controllers, services, JPA) against a real
 * PostgreSQL 16 Docker container (Testcontainers) with all Flyway migrations applied. The container is shared by all
 * integration test classes (static field + Spring context caching), and every table except {@code flyway_schema_history} is
 * truncated before each test, so tests never depend on each other or on the seed migrations (V2..V6).
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTestBase {

    /**
     * Singleton container: started once per test JVM and reused by every integration test class (Ryuk removes it when
     * the JVM exits). A per-class lifecycle would break Spring's cached context, which keeps the first port.
     * {@code @ServiceConnection} points spring.datasource.* at the container.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected JwtTokenService jwtTokenService;

    protected DbFixtures db;

    @BeforeEach
    void resetDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
                String.class
        );
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " CASCADE");
        db = new DbFixtures(jdbcTemplate);
    }

    /** Authorization header value for a user that exists in the database (same JWT the auth flow issues). */
    protected String bearer(UUID userId) {
        String token = jwtTokenService.generateToken(new AuthenticatedUser(userId, "Test User " + userId, null, null));
        return "Bearer " + token;
    }

    protected String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected ApiError readError(MvcResult result) throws Exception {
        return read(result, ApiError.class);
    }

    protected <T> T read(MvcResult result, Class<T> type) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), type);
    }
}

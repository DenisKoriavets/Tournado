package com.tournado.core.common.api;

import java.time.Instant;
import java.util.Map;
import javax.sql.DataSource;
import java.sql.Connection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping({"/api/health", "/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        String dbStatus = "DOWN";
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(2)) {
                dbStatus = "UP";
            }
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "tournado-core",
                "database", dbStatus,
                "timestamp", Instant.now().toString()
        ));
    }
}
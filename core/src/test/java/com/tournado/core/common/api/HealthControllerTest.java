package com.tournado.core.common.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthController healthController;

    @Test
    void givenHealthyDatabase_health_shouldReturn200AndDatabaseUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("tournado-core");
        assertThat(response.getBody().get("database")).isEqualTo("UP");
        assertThat(response.getBody()).containsKey("timestamp");
    }

    @Test
    void givenFailingDatabaseConnection_health_shouldReturn200AndDatabaseDown() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection refused"));

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
        assertThat(response.getBody().get("service")).isEqualTo("tournado-core");
        assertThat(response.getBody().get("database")).isEqualTo("DOWN");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}

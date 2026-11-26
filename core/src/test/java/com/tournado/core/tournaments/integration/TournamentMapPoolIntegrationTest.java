package com.tournado.core.tournaments.integration;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.support.IntegrationTestBase;
import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** US-08 "Автоматична підстановка пулу карт" — docs/requirements.md §5, Epic 3. */
@DisplayName("US-08: automatic map pool")
class TournamentMapPoolIntegrationTest extends IntegrationTestBase {

    private static final String POOL_SIZE_SQL = "SELECT COUNT(*) FROM tournament_map_pool WHERE tournament_id = ?";

    @Test
    @DisplayName("US-08 sc.1: a CS2 tournament is created with the standard CS2 map pool stored in the DB")
    void givenCs2Tournament_create_shouldStoreStandardMapPool() throws Exception {
        UUID organizer = db.player("Organizer");

        TournamentResponse created = create(organizer, Discipline.CS2);

        assertThat(created.mapPool()).containsExactlyElementsOf(Discipline.CS2.getDefaultMapPool());
        assertThat(db.count(POOL_SIZE_SQL, created.id())).isEqualTo(Discipline.CS2.getDefaultMapPool().size());
    }

    @Test
    @DisplayName("US-08 sc.2: a Chess.com tournament is created without any map pool")
    void givenChessTournament_create_shouldHaveNoMapPool() throws Exception {
        UUID organizer = db.player("Organizer");

        TournamentResponse created = create(organizer, Discipline.CHESS);

        assertThat(created.mapPool()).isEmpty();
        assertThat(db.count(POOL_SIZE_SQL, created.id())).isZero();
    }

    private TournamentResponse create(UUID organizer, Discipline discipline) throws Exception {
        Instant deadline = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        CreateTournamentRequest request = new CreateTournamentRequest("IT " + discipline + " Cup", "Integration test",
                discipline, 8, deadline, deadline.plus(1, ChronoUnit.HOURS), deadline.plus(2, ChronoUnit.HOURS), null);
        MvcResult result = mockMvc.perform(post("/api/v1/tournaments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(organizer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, TournamentResponse.class);
    }
}

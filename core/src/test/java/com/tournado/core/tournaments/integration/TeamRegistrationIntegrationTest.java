package com.tournado.core.tournaments.integration;

import com.tournado.core.support.ApiError;
import com.tournado.core.support.IntegrationTestBase;
import com.tournado.core.tournaments.api.dto.RegisterTeamRequest;
import com.tournado.core.tournaments.api.dto.RegisterTeamResponse;
import com.tournado.core.tournaments.api.dto.RegisteredTeamItem;
import com.tournado.core.tournaments.api.dto.TournamentRegistrationsResponse;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.tournado.core.common.domain.Discipline.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * US-12 "Подача заявки команди на турнір" — docs/requirements.md §5, Epic 4.
 * One test per Gherkin scenario (sc.1..sc.6) plus the negative rules from the story checklist.
 */
@DisplayName("US-12: team registration for a tournament")
class TeamRegistrationIntegrationTest extends IntegrationTestBase {

    private static final String REGISTRATIONS_COUNT_SQL =
            "SELECT COUNT(*) FROM registrations WHERE tournament_id = ? AND status <> 'EXCLUDED'";

    @Test
    @DisplayName("US-12 sc.1: full CS2 roster before the deadline is registered and takes one slot")
    void givenFullCs2TeamAndFreeSlots_register_shouldReturn201AndListTeam() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.fullTeam("Navi Junior KMA", CS2, captain);
        UUID tournamentId = db.openTournament(organizer, CS2, 8);
        RegisterTeamResponse expected = new RegisterTeamResponse(null, tournamentId, teamId,
                RegistrationStatus.REGISTERED, null);
        TournamentRegistrationsResponse expectedList = new TournamentRegistrationsResponse(tournamentId, organizer,
                8, 1, false, List.of(new RegisteredTeamItem(null, teamId, "Navi Junior KMA", CS2,
                        RegistrationStatus.REGISTERED, 5, null, false)));

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(read(result, RegisterTeamResponse.class))
                .usingRecursiveComparison()
                .ignoringFields("registrationId", "registeredAt")
                .isEqualTo(expected);
        MvcResult listResult = mockMvc.perform(get("/api/v1/tournaments/{id}/registrations", tournamentId))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(read(listResult, TournamentRegistrationsResponse.class))
                .usingRecursiveComparison()
                .ignoringFields("teams.registrationId", "teams.registeredAt")
                .isEqualTo(expectedList);
    }

    @Test
    @DisplayName("US-12 sc.2: all 8 slots are taken -> 409 REGISTRATION_CLOSED, nothing is added")
    void givenAllSlotsTaken_register_shouldReturn409AndNotPersist() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID tournamentId = db.openTournament(organizer, CS2, 8);
        for (int i = 1; i <= 8; i++) {
            db.registration(tournamentId, db.fullTeam("Registered " + i, CS2, db.player("Captain " + i)), "REGISTERED");
        }
        UUID captain = db.player("Late captain");
        UUID teamId = db.fullTeam("Late Team", CS2, captain);

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "REGISTRATION_CLOSED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isEqualTo(8);
    }

    @Test
    @DisplayName("US-12 sc.3: CS2 team with 4 of 5 players -> 422 ROSTER_NOT_FULL")
    void givenCs2TeamWithFourPlayers_register_shouldReturn422RosterNotFull() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.team("Four Stack", CS2, captain, db.player("P2"), db.player("P3"), db.player("P4"));
        UUID tournamentId = db.openTournament(organizer, CS2, 8);

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "ROSTER_NOT_FULL"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    @Test
    @DisplayName("US-12 sc.4: registration deadline has passed -> 409 REGISTRATION_CLOSED")
    void givenDeadlinePassed_register_shouldReturn409RegistrationClosed() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.fullTeam("Too Late", CS2, captain);
        UUID tournamentId = db.tournament(organizer, CS2, 8, "REGISTRATION_OPEN",
                Instant.now().minus(1, ChronoUnit.MINUTES));

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "REGISTRATION_CLOSED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    @Test
    @DisplayName("US-12 sc.5: the same team registers twice -> 409 ALREADY_REGISTERED, no duplicate row")
    void givenTeamAlreadyRegistered_register_shouldReturn409AndKeepSingleRegistration() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.fullTeam("Double Trouble", CS2, captain);
        UUID tournamentId = db.openTournament(organizer, CS2, 8);
        register(tournamentId, teamId, captain).andExpect(status().isCreated());

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "ALREADY_REGISTERED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-12 sc.6: 15 of 16 slots taken -> the last slot is accepted, the next team is rejected")
    void givenOneFreeSlotLeft_register_shouldAcceptLastTeamAndRejectNext() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID tournamentId = db.openTournament(organizer, BRAWL_STARS, 16);
        for (int i = 1; i <= 15; i++) {
            db.registration(tournamentId, db.fullTeam("BS " + i, BRAWL_STARS, db.player("Captain " + i)),
                    "REGISTERED");
        }
        UUID lastCaptain = db.player("Last captain");
        UUID lastTeam = db.fullTeam("Last Slot", BRAWL_STARS, lastCaptain);
        UUID extraCaptain = db.player("Extra captain");
        UUID extraTeam = db.fullTeam("No Slot", BRAWL_STARS, extraCaptain);

        register(tournamentId, lastTeam, lastCaptain).andExpect(status().isCreated());
        MvcResult rejected = register(tournamentId, extraTeam, extraCaptain)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isEqualTo(16);
        assertThat(readError(rejected)).isEqualTo(new ApiError(409, "REGISTRATION_CLOSED"));
    }

    @Test
    @DisplayName("US-12 checklist: a player without a Steam ID blocks CS2 registration -> 422 MISSING_GAME_IDENTIFIER")
    void givenRosterMemberWithoutSteamId_register_shouldReturn422MissingGameIdentifier() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.team("No Steam", CS2, captain, db.player("P2"), db.player("P3"), db.player("P4"),
                db.playerWithoutGameIds("Email only"));
        UUID tournamentId = db.openTournament(organizer, CS2, 8);

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(422, "MISSING_GAME_IDENTIFIER"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    @Test
    @DisplayName("US-12 checklist: a Brawl Stars team cannot register for a CS2 tournament -> 400 VALIDATION_FAILED")
    void givenTeamOfOtherDiscipline_register_shouldReturn400ValidationFailed() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.fullTeam("Wrong Game", BRAWL_STARS, captain);
        UUID tournamentId = db.openTournament(organizer, CS2, 8);

        MvcResult result = register(tournamentId, teamId, captain)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(400, "VALIDATION_FAILED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    @Test
    @DisplayName("US-12 checklist: only the captain can register the team -> 403 ONLY_CAPTAIN_ALLOWED")
    void givenNonCaptainTeamMember_register_shouldReturn403() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID member = db.player("Member");
        UUID teamId = db.team("Strict Captain", CS2, captain, member, db.player("P3"), db.player("P4"),
                db.player("P5"));
        UUID tournamentId = db.openTournament(organizer, CS2, 8);

        MvcResult result = register(tournamentId, teamId, member)
                .andExpect(status().isForbidden())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(403, "ONLY_CAPTAIN_ALLOWED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    @Test
    @DisplayName("US-01 sc.5 / US-12: anonymous visitor cannot register -> 401 AUTH_UNAUTHORIZED")
    void givenAnonymousVisitor_register_shouldReturn401() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID teamId = db.fullTeam("Anonymous", CS2, db.player("Captain"));
        UUID tournamentId = db.openTournament(organizer, CS2, 8);

        MvcResult result = mockMvc.perform(post("/api/v1/tournaments/{id}/register", tournamentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new RegisterTeamRequest(teamId))))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(401, "AUTH_UNAUTHORIZED"));
        assertThat(db.count(REGISTRATIONS_COUNT_SQL, tournamentId)).isZero();
    }

    private ResultActions register(UUID tournamentId, UUID teamId, UUID actor)
            throws Exception {
        return mockMvc.perform(post("/api/v1/tournaments/{id}/register", tournamentId)
                .header(HttpHeaders.AUTHORIZATION, bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new RegisterTeamRequest(teamId))));
    }
}

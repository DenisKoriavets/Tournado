package com.tournado.core.teams.integration;

import com.tournado.core.support.ApiError;
import com.tournado.core.support.IntegrationTestBase;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.api.dto.TeamMemberResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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
 * US-05 "Приєднання гравця до команди за посиланням" (+ US-14 sc.2/sc.3 for the roster lock) — docs/requirements.md §5.
 */
@DisplayName("US-05: joining a team by invite link")
class TeamJoinByInviteIntegrationTest extends IntegrationTestBase {

    private static final String MEMBERS_COUNT_SQL = "SELECT COUNT(*) FROM team_members WHERE team_id = ?";

    @Test
    @DisplayName("US-05 sc.1: player follows a fresh invite of a team with a free slot -> joins, counter grows by 1")
    void givenTeamWithFreeSlot_joinByInvite_shouldAddPlayerAndIncreaseCounter() throws Exception {
        UUID captain = db.player("Captain");
        UUID teammate = db.player("Teammate");
        UUID newcomer = db.player("Newcomer");
        UUID teamId = db.team("Brawl Owls", BRAWL_STARS, captain, teammate);
        String token = createInviteViaApi(teamId, captain);
        TeamDetailsResponse expected = new TeamDetailsResponse(teamId, "Brawl Owls", BRAWL_STARS, captain, 3, 3, true,
                List.of(new TeamMemberResponse(captain, "Captain", true, null),
                        new TeamMemberResponse(teammate, "Teammate", false, null),
                        new TeamMemberResponse(newcomer, "Newcomer", false, null)));

        MvcResult result = join(token, newcomer)
                .andExpect(status().isOk())
                .andReturn();

        assertThat(read(result, TeamDetailsResponse.class))
                .usingRecursiveComparison()
                .ignoringFields("members.gameIdentifier")
                .ignoringCollectionOrder()
                .isEqualTo(expected);
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(3);
    }

    @Test
    @DisplayName("US-05 sc.2: roster is already full -> 409 ROSTER_FULL, nobody is added")
    void givenFullRoster_joinByInvite_shouldReturn409RosterFull() throws Exception {
        UUID captain = db.player("Captain");
        UUID teamId = db.fullTeam("Full House", BRAWL_STARS, captain);
        db.invite(teamId, captain, "inv_full_house", true, Instant.now().plus(7, ChronoUnit.DAYS));

        MvcResult result = join("inv_full_house", db.player("Fourth wheel"))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "ROSTER_FULL"));
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(3);
    }

    @Test
    @DisplayName("US-05 sc.3: player is already in the roster -> 409 PLAYER_ALREADY_IN_TEAM, no duplicate")
    void givenPlayerAlreadyInTeam_joinByInvite_shouldReturn409AndNotDuplicate() throws Exception {
        UUID captain = db.player("Captain");
        UUID teammate = db.player("Teammate");
        UUID teamId = db.team("Loyal Five", CS2, captain, teammate);
        String token = createInviteViaApi(teamId, captain);

        MvcResult result = join(token, teammate)
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "PLAYER_ALREADY_IN_TEAM"));
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(2);
    }

    @Test
    @DisplayName("US-05 sc.4: token does not exist -> 400 INVITE_LINK_EXPIRED")
    void givenUnknownToken_joinByInvite_shouldReturn400InviteLinkExpired() throws Exception {
        MvcResult result = join("inv_does_not_exist", db.player("Lost player"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(400, "INVITE_LINK_EXPIRED"));
    }

    @Test
    @DisplayName("US-05 sc.4: invite was deactivated or has expired -> 400 INVITE_LINK_EXPIRED, nobody is added")
    void givenDeactivatedOrExpiredInvite_joinByInvite_shouldReturn400() throws Exception {
        UUID captain = db.player("Captain");
        UUID teamId = db.team("Old Links", CS2, captain);
        db.invite(teamId, captain, "inv_deactivated", false, Instant.now().plus(7, ChronoUnit.DAYS));
        db.invite(teamId, captain, "inv_expired", true, Instant.now().minus(1, ChronoUnit.MINUTES));
        UUID player = db.player("Late player");

        MvcResult deactivated = join("inv_deactivated", player).andExpect(status().isBadRequest()).andReturn();
        MvcResult expired = join("inv_expired", player).andExpect(status().isBadRequest()).andReturn();

        assertThat(List.of(readError(deactivated), readError(expired)))
                .containsOnly(new ApiError(400, "INVITE_LINK_EXPIRED"));
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(1);
    }

    @Test
    @DisplayName("US-05 sc.5 / US-14 sc.2: team is registered and the deadline has passed -> 409 ROSTER_LOCKED")
    void givenRegisteredTeamAfterDeadline_joinByInvite_shouldReturn409RosterLocked() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.team("Locked Four", CS2, captain, db.player("P2"), db.player("P3"), db.player("P4"));
        db.invite(teamId, captain, "inv_locked", true, Instant.now().plus(7, ChronoUnit.DAYS));
        UUID tournamentId = db.tournament(organizer, CS2, 8, "REGISTRATION_OPEN",
                Instant.now().minus(1, ChronoUnit.HOURS));
        db.registration(tournamentId, teamId, "REGISTERED");

        MvcResult result = join("inv_locked", db.player("Ringer"))
                .andExpect(status().isConflict())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(409, "ROSTER_LOCKED"));
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(4);
    }

    @Test
    @DisplayName("US-14 sc.3: team is registered but the deadline is still ahead -> joining is allowed")
    void givenRegisteredTeamBeforeDeadline_joinByInvite_shouldAddPlayer() throws Exception {
        UUID organizer = db.player("Organizer");
        UUID captain = db.player("Captain");
        UUID teamId = db.team("Open Four", CS2, captain, db.player("P2"), db.player("P3"), db.player("P4"));
        UUID tournamentId = db.openTournament(organizer, CS2, 8);
        db.registration(tournamentId, teamId, "REGISTERED");
        String token = createInviteViaApi(teamId, captain);

        join(token, db.player("Fifth")).andExpect(status().isOk());

        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(5);
    }

    @Test
    @DisplayName("US-01 sc.5 / US-05: anonymous visitor cannot join -> 401 AUTH_UNAUTHORIZED")
    void givenAnonymousVisitor_joinByInvite_shouldReturn401() throws Exception {
        UUID captain = db.player("Captain");
        UUID teamId = db.team("Guarded", CS2, captain);
        String token = createInviteViaApi(teamId, captain);

        MvcResult result = mockMvc.perform(post("/api/v1/teams/join/{token}", token))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(readError(result)).isEqualTo(new ApiError(401, "AUTH_UNAUTHORIZED"));
        assertThat(db.count(MEMBERS_COUNT_SQL, teamId)).isEqualTo(1);
    }

    private String createInviteViaApi(UUID teamId, UUID captain) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/teams/{id}/invites", teamId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(captain)))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result, TeamInviteResponse.class).token();
    }

    private ResultActions join(String token, UUID userId) throws Exception {
        return mockMvc.perform(post("/api/v1/teams/join/{token}", token)
                .header(HttpHeaders.AUTHORIZATION, bearer(userId)));
    }
}

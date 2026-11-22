package com.tournado.core.teams.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournado.core.common.config.TimeConfig;
import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.exception.GlobalExceptionHandler;
import com.tournado.core.common.security.JwtProperties;
import com.tournado.core.common.security.JwtTokenService;
import com.tournado.core.common.security.RestAuthenticationEntryPoint;
import com.tournado.core.common.security.SecurityConfig;
import com.tournado.core.common.security.SecurityContextCurrentUserProvider;
import com.tournado.core.teams.api.dto.CreateTeamRequest;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInvitePreviewResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.api.dto.TeamSummaryResponse;
import com.tournado.core.teams.exception.InviteLinkExpiredException;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.exception.PlayerAlreadyInTeamException;
import com.tournado.core.teams.exception.RosterFullException;
import com.tournado.core.teams.exception.RosterLockedException;
import com.tournado.core.teams.exception.TeamNameAlreadyExistsException;
import com.tournado.core.teams.exception.TeamNotFoundException;
import com.tournado.core.teams.service.TeamInviteService;
import com.tournado.core.teams.service.TeamService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import static com.tournado.core.teams.TeamTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TeamController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class,
    TimeConfig.class,
    JwtTokenService.class,
    RestAuthenticationEntryPoint.class,
    SecurityContextCurrentUserProvider.class
})
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockBean
    private TeamService teamService;

    @MockBean
    private TeamInviteService teamInviteService;

    @Test
    void givenAnonymousUser_createTeam_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        CreateTeamRequest request = buildCreateCs2TeamRequest();
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/teams");

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(teamService);
    }

    @Test
    void givenAuthenticatedUserAndValidRequest_createTeam_shouldReturn201CreatedWithSummary() throws Exception {
        CreateTeamRequest request = buildCreateCs2TeamRequest();
        TeamSummaryResponse expected = buildExpectedCs2CreatedSummaryResponse();
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamService.createTeam(request, CAPTAIN_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String rawJson = result.getResponse().getContentAsString();
        Map<String, Object> jsonMap = objectMapper.readValue(rawJson, new TypeReference<>() {});
        TeamSummaryResponse actual = objectMapper.readValue(rawJson, TeamSummaryResponse.class);
        assertThat(jsonMap).containsEntry("isFull", false);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenBlankTeamName_createTeam_shouldReturn400ValidationProblemDetail() throws Exception {
        CreateTeamRequest request = buildBlankNameCreateTeamRequest();
        ProblemDetail expected = buildBlankTeamNameProblemDetail();
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(teamService);
    }

    @Test
    void givenDuplicateTeamName_createTeam_shouldReturn409ConflictProblemDetail() throws Exception {
        CreateTeamRequest request = buildCreateCs2TeamRequest();
        ProblemDetail expected = buildTeamNameAlreadyExistsProblemDetail();
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamService.createTeam(request, CAPTAIN_ID))
                .thenThrow(new TeamNameAlreadyExistsException(TEAM_NAME, Discipline.CS2));

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                .cookie(new Cookie(jwtProperties.cookieName(), token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAnonymousUser_getMyTeams_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/teams/my");

        MvcResult result = mockMvc.perform(get("/api/v1/teams/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(teamService);
    }

    @Test
    void givenAuthenticatedUser_getMyTeams_shouldReturn200WithTeamSummaries() throws Exception {
        List<TeamSummaryResponse> expected = buildExpectedMyTeamsResponse();
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamService.getMyTeams(CAPTAIN_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/teams/my")
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        List<TeamSummaryResponse> actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenPublicCaller_getTeamById_shouldReturn200WithTeamDetails() throws Exception {
        TeamDetailsResponse expected = buildExpectedCs2TeamDetailsResponse();
        when(teamService.getTeamById(TEAM_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/teams/{teamId}", TEAM_ID))
                .andExpect(status().isOk())
                .andReturn();

        String rawJson = result.getResponse().getContentAsString();
        Map<String, Object> jsonMap = objectMapper.readValue(rawJson, new TypeReference<>() {});
        TeamDetailsResponse actual = objectMapper.readValue(rawJson, TeamDetailsResponse.class);
        assertThat(jsonMap).containsEntry("isFull", false);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingTeam_getTeamById_shouldReturn404NotFoundProblemDetail() throws Exception {
        UUID missingId = UUID.randomUUID();
        ProblemDetail expected = buildTeamNotFoundProblemDetail(missingId, "/api/v1/teams/" + missingId);
        when(teamService.getTeamById(missingId)).thenThrow(new TeamNotFoundException(missingId));

        MvcResult result = mockMvc.perform(get("/api/v1/teams/{teamId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAnonymousUser_createInvite_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/teams/" + TEAM_ID + "/invites");

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{teamId}/invites", TEAM_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(teamInviteService);
    }

    @Test
    void givenAuthenticatedCaptain_createInvite_shouldReturn201WithInvite() throws Exception {
        TeamInviteResponse expected = buildExpectedInviteResponse(INVITE_TOKEN);
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{teamId}/invites", TEAM_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isCreated())
                .andReturn();

        TeamInviteResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), TeamInviteResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNonCaptainUser_createInvite_shouldReturn403ForbiddenProblemDetail() throws Exception {
        ProblemDetail expected = buildOnlyCaptainAllowedProblemDetail(TEAM_ID);
        String token = jwtTokenService.generateToken(buildSecondPlayerAuthUser());
        when(teamInviteService.createInvite(TEAM_ID, PLAYER_TWO_ID)).thenThrow(new OnlyCaptainAllowedException());

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{teamId}/invites", TEAM_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenLockedRoster_createInvite_shouldReturn409ConflictProblemDetail() throws Exception {
        ProblemDetail expected = buildRosterLockedProblemDetail("/api/v1/teams/" + TEAM_ID + "/invites");
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID)).thenThrow(new RosterLockedException());

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{teamId}/invites", TEAM_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenFullRoster_createInvite_shouldReturn409ConflictProblemDetail() throws Exception {
        ProblemDetail expected = buildRosterFullProblemDetail(TEAM_NAME, Discipline.CS2, "/api/v1/teams/" + TEAM_ID + "/invites");
        String token = jwtTokenService.generateToken(buildCaptainAuthUser());
        when(teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID))
                .thenThrow(new RosterFullException(TEAM_NAME, Discipline.CS2));

        MvcResult result = mockMvc.perform(post("/api/v1/teams/{teamId}/invites", TEAM_ID)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenPublicCaller_previewInvite_shouldReturn200WithPreview() throws Exception {
        TeamInvitePreviewResponse expected = buildExpectedInvitePreviewResponse();
        when(teamInviteService.previewInvite(INVITE_TOKEN)).thenReturn(expected);

        MvcResult result = mockMvc.perform(get("/api/v1/teams/invites/{token}", INVITE_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        TeamInvitePreviewResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TeamInvitePreviewResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExpiredInvite_previewInvite_shouldReturn400ProblemDetail() throws Exception {
        ProblemDetail expected = buildInviteLinkExpiredProblemDetail("/api/v1/teams/invites/" + INVITE_TOKEN);
        when(teamInviteService.previewInvite(INVITE_TOKEN)).thenThrow(new InviteLinkExpiredException());

        MvcResult result = mockMvc.perform(get("/api/v1/teams/invites/{token}", INVITE_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
    }

    @Test
    void givenAnonymousUser_joinTeamByToken_shouldReturn401UnauthorizedProblemDetail() throws Exception {
        ProblemDetail expected = buildUnauthorizedProblemDetail("/api/v1/teams/join/" + INVITE_TOKEN);

        MvcResult result = mockMvc.perform(post("/api/v1/teams/join/{token}", INVITE_TOKEN))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        assertProblemDetailMatches(result, expected);
        verifyNoInteractions(teamInviteService);
    }

    @Test
    void givenAuthenticatedUser_joinTeamByToken_shouldReturn200WithTeamDetails() throws Exception {
        TeamDetailsResponse expected = buildExpectedCs2TeamDetailsWithTwoMembersResponse();
        String token = jwtTokenService.generateToken(buildSecondPlayerAuthUser());
        when(teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID)).thenReturn(expected);

        MvcResult result = mockMvc.perform(post("/api/v1/teams/join/{token}", INVITE_TOKEN)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isOk())
                .andReturn();

        TeamDetailsResponse actual = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TeamDetailsResponse.class
        );
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExistingMember_joinTeamByToken_shouldReturn409ConflictProblemDetail() throws Exception {
        ProblemDetail expected = buildPlayerAlreadyInTeamProblemDetail(INVITE_TOKEN);
        String token = jwtTokenService.generateToken(buildSecondPlayerAuthUser());
        when(teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID))
                .thenThrow(new PlayerAlreadyInTeamException());

        MvcResult result = mockMvc.perform(post("/api/v1/teams/join/{token}", INVITE_TOKEN)
                .cookie(new Cookie(jwtProperties.cookieName(), token)))
                .andExpect(status().isConflict())
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

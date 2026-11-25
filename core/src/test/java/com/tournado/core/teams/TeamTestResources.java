package com.tournado.core.teams;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.common.security.AuthProperties;
import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.identity.service.UserProfileSummary;
import com.tournado.core.teams.api.dto.CreateTeamRequest;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInvitePreviewResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.api.dto.TeamMemberResponse;
import com.tournado.core.teams.api.dto.TeamSummaryResponse;
import com.tournado.core.teams.domain.TeamEntity;
import com.tournado.core.teams.domain.TeamInviteEntity;
import com.tournado.core.teams.domain.TeamMemberEntity;
import com.tournado.core.teams.service.TeamMemberView;
import com.tournado.core.teams.service.TeamView;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

public final class TeamTestResources {

    private TeamTestResources() {
    }

    public static final UUID CAPTAIN_ID = UUID.fromString("e4b2d184-f376-476c-9226-64d852a39a01");
    public static final UUID PLAYER_TWO_ID = UUID.fromString("a1b2c3d4-e5f6-4789-9012-112233445566");
    public static final UUID TEAM_ID = UUID.fromString("7a30368b-59b1-4f18-bb59-8395f13d8d64");
    public static final UUID CHESS_TEAM_ID = UUID.fromString("8b40479c-6ac2-4029-cc6a-94a6f24e9e75");
    public static final UUID BRAWL_TEAM_ID = UUID.fromString("9c5058ad-7bd3-413a-dd7b-a5b7035faf86");
    public static final UUID INVITE_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    public static final String TEAM_NAME = "Navi Junior KMA";
    public static final String CHESS_TEAM_NAME = "Grandmasters KMA";
    public static final String BRAWL_TEAM_NAME = "Brawl Stars KMA";
    public static final String INVITE_TOKEN = "inv_a8d712f5b6e9";
    public static final String PUBLIC_BASE_URL = "http://localhost:3000";
    public static final Instant FIXED_NOW = Instant.parse("2026-11-22T12:00:00Z");
    public static final Instant INVITE_EXPIRES_AT = FIXED_NOW.plus(7L, ChronoUnit.DAYS);
    public static final String CAPTAIN_DISPLAY_NAME = "CyberHawk";
    public static final String CAPTAIN_EMAIL = "captain@ukma.edu.ua";
    public static final String CAPTAIN_STEAM_ID = "76561198012345678";
    public static final String CAPTAIN_SUPERCELL_TAG = "#2PP98L9";
    public static final String CAPTAIN_CHESS_USERNAME = "cyber_master";
    public static final String PLAYER_TWO_DISPLAY_NAME = "s1mple_kma";
    public static final String PLAYER_TWO_EMAIL = "player2@ukma.edu.ua";
    public static final String PLAYER_TWO_STEAM_ID = "76561198099887766";

    public static AuthProperties buildTeamAuthProperties() {
        return new AuthProperties(15L, PUBLIC_BASE_URL, "https://steamcommunity.com/openid/login");
    }

    public static AuthenticatedUser buildCaptainAuthUser() {
        return new AuthenticatedUser(CAPTAIN_ID, CAPTAIN_DISPLAY_NAME, CAPTAIN_EMAIL, CAPTAIN_STEAM_ID);
    }

    public static AuthenticatedUser buildSecondPlayerAuthUser() {
        return new AuthenticatedUser(PLAYER_TWO_ID, PLAYER_TWO_DISPLAY_NAME, PLAYER_TWO_EMAIL, PLAYER_TWO_STEAM_ID);
    }

    public static UserProfileSummary buildCaptainProfileSummary() {
        return new UserProfileSummary(
                CAPTAIN_ID,
                CAPTAIN_EMAIL,
                CAPTAIN_STEAM_ID,
                CAPTAIN_DISPLAY_NAME,
                CAPTAIN_SUPERCELL_TAG,
                CAPTAIN_CHESS_USERNAME,
                null
        );
    }

    public static UserProfileSummary buildSecondPlayerProfileSummary() {
        return new UserProfileSummary(
                PLAYER_TWO_ID,
                PLAYER_TWO_EMAIL,
                PLAYER_TWO_STEAM_ID,
                PLAYER_TWO_DISPLAY_NAME,
                null,
                null,
                null
        );
    }

    public static TeamEntity buildCs2TeamEntity() {
        TeamEntity entity = new TeamEntity();
        entity.setId(TEAM_ID);
        entity.setName(TEAM_NAME);
        entity.setDiscipline(Discipline.CS2);
        entity.setCaptainId(CAPTAIN_ID);
        entity.setCreatedAt(FIXED_NOW);
        entity.setUpdatedAt(FIXED_NOW);
        entity.setVersion(0L);
        return entity;
    }

    public static TeamEntity buildChessTeamEntity() {
        TeamEntity entity = new TeamEntity();
        entity.setId(CHESS_TEAM_ID);
        entity.setName(CHESS_TEAM_NAME);
        entity.setDiscipline(Discipline.CHESS);
        entity.setCaptainId(CAPTAIN_ID);
        entity.setCreatedAt(FIXED_NOW);
        entity.setUpdatedAt(FIXED_NOW);
        entity.setVersion(0L);
        return entity;
    }

    public static TeamEntity buildBrawlTeamEntity() {
        TeamEntity entity = new TeamEntity();
        entity.setId(BRAWL_TEAM_ID);
        entity.setName(BRAWL_TEAM_NAME);
        entity.setDiscipline(Discipline.BRAWL_STARS);
        entity.setCaptainId(CAPTAIN_ID);
        entity.setCreatedAt(FIXED_NOW);
        entity.setUpdatedAt(FIXED_NOW);
        entity.setVersion(0L);
        return entity;
    }

    public static TeamMemberEntity buildCaptainMemberEntity() {
        TeamMemberEntity member = new TeamMemberEntity();
        member.setId(UUID.fromString("33333333-4444-5555-6666-777777777771"));
        member.setTeamId(TEAM_ID);
        member.setUserId(CAPTAIN_ID);
        member.setJoinedAt(FIXED_NOW);
        return member;
    }

    public static TeamMemberEntity buildSecondMemberEntity() {
        TeamMemberEntity member = new TeamMemberEntity();
        member.setId(UUID.fromString("33333333-4444-5555-6666-777777777772"));
        member.setTeamId(TEAM_ID);
        member.setUserId(PLAYER_TWO_ID);
        member.setJoinedAt(FIXED_NOW.plusSeconds(60L));
        return member;
    }

    public static TeamMemberEntity buildChessCaptainMemberEntity() {
        TeamMemberEntity member = new TeamMemberEntity();
        member.setId(UUID.fromString("33333333-4444-5555-6666-777777777773"));
        member.setTeamId(CHESS_TEAM_ID);
        member.setUserId(CAPTAIN_ID);
        member.setJoinedAt(FIXED_NOW);
        return member;
    }

    public static TeamMemberEntity buildBrawlCaptainMemberEntity() {
        TeamMemberEntity member = new TeamMemberEntity();
        member.setId(UUID.fromString("33333333-4444-5555-6666-777777777774"));
        member.setTeamId(BRAWL_TEAM_ID);
        member.setUserId(CAPTAIN_ID);
        member.setJoinedAt(FIXED_NOW);
        return member;
    }

    public static TeamInviteEntity buildActiveInviteEntity() {
        TeamInviteEntity invite = new TeamInviteEntity();
        invite.setId(INVITE_ID);
        invite.setTeamId(TEAM_ID);
        invite.setToken(INVITE_TOKEN);
        invite.setCreatedBy(CAPTAIN_ID);
        invite.setActive(true);
        invite.setExpiresAt(INVITE_EXPIRES_AT);
        invite.setCreatedAt(FIXED_NOW);
        return invite;
    }

    public static TeamInviteEntity buildInactiveInviteEntity() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        invite.setActive(false);
        return invite;
    }

    public static TeamInviteEntity buildExpiredInviteEntity() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        invite.setExpiresAt(FIXED_NOW.minusSeconds(1L));
        return invite;
    }

    public static CreateTeamRequest buildCreateCs2TeamRequest() {
        return new CreateTeamRequest("  " + TEAM_NAME + "  ", Discipline.CS2);
    }

    public static CreateTeamRequest buildCreateChessTeamRequest() {
        return new CreateTeamRequest(CHESS_TEAM_NAME, Discipline.CHESS);
    }

    public static CreateTeamRequest buildBlankNameCreateTeamRequest() {
        return new CreateTeamRequest("   ", Discipline.CS2);
    }

    public static TeamSummaryResponse buildExpectedCs2CreatedSummaryResponse() {
        return new TeamSummaryResponse(TEAM_ID, TEAM_NAME, Discipline.CS2, CAPTAIN_ID, 1, 5, false);
    }

    public static TeamSummaryResponse buildExpectedChessCreatedSummaryResponse() {
        return new TeamSummaryResponse(CHESS_TEAM_ID, CHESS_TEAM_NAME, Discipline.CHESS, CAPTAIN_ID, 1, 1, true);
    }

    public static List<TeamSummaryResponse> buildExpectedMyTeamsResponse() {
        return List.of(
                new TeamSummaryResponse(TEAM_ID, TEAM_NAME, Discipline.CS2, CAPTAIN_ID, 2, 5, false),
                new TeamSummaryResponse(CHESS_TEAM_ID, CHESS_TEAM_NAME, Discipline.CHESS, CAPTAIN_ID, 1, 1, true)
        );
    }

    public static TeamDetailsResponse buildExpectedCs2TeamDetailsResponse() {
        return new TeamDetailsResponse(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_ID,
                1,
                5,
                false,
                List.of(new TeamMemberResponse(CAPTAIN_ID, CAPTAIN_DISPLAY_NAME, true, CAPTAIN_STEAM_ID))
        );
    }

    public static TeamDetailsResponse buildExpectedCs2TeamDetailsWithTwoMembersResponse() {
        return new TeamDetailsResponse(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_ID,
                2,
                5,
                false,
                List.of(
                        new TeamMemberResponse(CAPTAIN_ID, CAPTAIN_DISPLAY_NAME, true, CAPTAIN_STEAM_ID),
                        new TeamMemberResponse(PLAYER_TWO_ID, PLAYER_TWO_DISPLAY_NAME, false, PLAYER_TWO_STEAM_ID)
                )
        );
    }

    public static TeamDetailsResponse buildExpectedBrawlTeamDetailsResponse() {
        return new TeamDetailsResponse(
                BRAWL_TEAM_ID,
                BRAWL_TEAM_NAME,
                Discipline.BRAWL_STARS,
                CAPTAIN_ID,
                1,
                3,
                false,
                List.of(new TeamMemberResponse(CAPTAIN_ID, CAPTAIN_DISPLAY_NAME, true, CAPTAIN_SUPERCELL_TAG))
        );
    }

    public static TeamDetailsResponse buildExpectedChessTeamDetailsResponse() {
        return new TeamDetailsResponse(
                CHESS_TEAM_ID,
                CHESS_TEAM_NAME,
                Discipline.CHESS,
                CAPTAIN_ID,
                1,
                1,
                true,
                List.of(new TeamMemberResponse(CAPTAIN_ID, CAPTAIN_DISPLAY_NAME, true, CAPTAIN_CHESS_USERNAME))
        );
    }

    public static TeamInviteResponse buildExpectedInviteResponse(String token) {
        return new TeamInviteResponse(token, PUBLIC_BASE_URL + "/join/" + token, INVITE_EXPIRES_AT);
    }

    public static TeamInvitePreviewResponse buildExpectedInvitePreviewResponse() {
        return new TeamInvitePreviewResponse(INVITE_TOKEN, TEAM_ID, TEAM_NAME, Discipline.CS2, 2, 5, false);
    }

    public static TeamView buildExpectedCs2TeamView() {
        return new TeamView(
                TEAM_ID,
                TEAM_NAME,
                Discipline.CS2,
                CAPTAIN_ID,
                2,
                5,
                false,
                List.of(
                        new TeamMemberView(
                                CAPTAIN_ID,
                                CAPTAIN_DISPLAY_NAME,
                                true,
                                CAPTAIN_STEAM_ID,
                                CAPTAIN_SUPERCELL_TAG,
                                CAPTAIN_CHESS_USERNAME
                        ),
                        new TeamMemberView(
                                PLAYER_TWO_ID,
                                PLAYER_TWO_DISPLAY_NAME,
                                false,
                                PLAYER_TWO_STEAM_ID,
                                null,
                                null
                        )
                )
        );
    }

    public static ProblemDetail buildTeamNotFoundProblemDetail(UUID teamId, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "Team with id '" + teamId + "' was not found."
        );
        problem.setType(URI.create("https://tournado.ua/errors/TEAM_NOT_FOUND"));
        problem.setTitle("Not Found");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "TEAM_NOT_FOUND");
        return problem;
    }

    public static ProblemDetail buildTeamNameAlreadyExistsProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "A team with the name '" + TEAM_NAME + "' already exists in the CS2 discipline."
        );
        problem.setType(URI.create("https://tournado.ua/errors/TEAM_NAME_ALREADY_EXISTS"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/teams"));
        problem.setProperty("code", "TEAM_NAME_ALREADY_EXISTS");
        return problem;
    }

    public static ProblemDetail buildOnlyCaptainAllowedProblemDetail(UUID teamId) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Action can only be performed by the registered team captain."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ONLY_CAPTAIN_ALLOWED"));
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create("/api/v1/teams/" + teamId + "/invites"));
        problem.setProperty("code", "ONLY_CAPTAIN_ALLOWED");
        return problem;
    }

    public static ProblemDetail buildRosterLockedProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Roster is locked; players cannot be added or removed."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ROSTER_LOCKED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "ROSTER_LOCKED");
        return problem;
    }

    public static ProblemDetail buildRosterFullProblemDetail(String teamName, Discipline discipline, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Team '" + teamName + "' roster has already reached the maximum of "
                        + discipline.getTeamSize() + " players for " + discipline + "."
        );
        problem.setType(URI.create("https://tournado.ua/errors/ROSTER_FULL"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "ROSTER_FULL");
        return problem;
    }

    public static ProblemDetail buildPlayerAlreadyInTeamProblemDetail(String token) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Player is already an active member of this team roster."
        );
        problem.setType(URI.create("https://tournado.ua/errors/PLAYER_ALREADY_IN_TEAM"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/teams/join/" + token));
        problem.setProperty("code", "PLAYER_ALREADY_IN_TEAM");
        return problem;
    }

    public static ProblemDetail buildInviteLinkExpiredProblemDetail(String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The team invite link has been deactivated or expired."
        );
        problem.setType(URI.create("https://tournado.ua/errors/INVITE_LINK_EXPIRED"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create(instance));
        problem.setProperty("code", "INVITE_LINK_EXPIRED");
        return problem;
    }

    public static ProblemDetail buildBlankTeamNameProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body contains 1 validation constraint violations"
        );
        problem.setType(URI.create("https://tournado.ua/errors/VALIDATION_FAILED"));
        problem.setTitle("Bad Request");
        problem.setInstance(URI.create("/api/v1/teams"));
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("invalidParams", List.of(
                Map.of("name", "name", "reason", "Team name cannot be blank")
        ));
        return problem;
    }
}

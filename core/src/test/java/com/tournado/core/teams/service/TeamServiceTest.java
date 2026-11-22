package com.tournado.core.teams.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.identity.service.UserQueryService;
import com.tournado.core.teams.api.dto.CreateTeamRequest;
import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamSummaryResponse;
import com.tournado.core.teams.domain.TeamEntity;
import com.tournado.core.teams.domain.TeamMemberEntity;
import com.tournado.core.teams.exception.TeamNameAlreadyExistsException;
import com.tournado.core.teams.exception.TeamNotFoundException;
import com.tournado.core.teams.repository.TeamMemberRepository;
import com.tournado.core.teams.repository.TeamRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.teams.TeamTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private UserQueryService userQueryService;

    private TeamService teamService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        teamService = new TeamService(teamRepository, teamMemberRepository, userQueryService, fixedClock);
    }

    @Test
    void givenValidCs2TeamRequest_createTeam_shouldPersistTeamAndCaptainMember() {
        CreateTeamRequest request = buildCreateCs2TeamRequest();
        TeamEntity expectedTeam = buildCs2TeamEntity();
        TeamMemberEntity expectedMember = buildCaptainMemberEntity();
        TeamSummaryResponse expectedResponse = buildExpectedCs2CreatedSummaryResponse();
        when(teamRepository.existsByNameIgnoreCaseAndDiscipline(TEAM_NAME, Discipline.CS2)).thenReturn(false);
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.save(any(TeamMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamSummaryResponse actual = teamService.createTeam(request, CAPTAIN_ID);

        ArgumentCaptor<TeamEntity> teamCaptor = ArgumentCaptor.forClass(TeamEntity.class);
        verify(teamRepository).save(teamCaptor.capture());
        assertThat(teamCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedTeam);
        ArgumentCaptor<TeamMemberEntity> memberCaptor = ArgumentCaptor.forClass(TeamMemberEntity.class);
        verify(teamMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id", "teamId")
                .isEqualTo(expectedMember);
        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedResponse);
    }

    @Test
    void givenValidChessTeamRequest_createTeam_shouldReturnFullTeamSummary() {
        CreateTeamRequest request = buildCreateChessTeamRequest();
        TeamSummaryResponse expectedResponse = buildExpectedChessCreatedSummaryResponse();
        when(teamRepository.existsByNameIgnoreCaseAndDiscipline(CHESS_TEAM_NAME, Discipline.CHESS)).thenReturn(false);
        when(teamRepository.save(any(TeamEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamMemberRepository.save(any(TeamMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamSummaryResponse actual = teamService.createTeam(request, CAPTAIN_ID);

        assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedResponse);
    }

    @Test
    void givenDuplicateTeamNameInDiscipline_createTeam_shouldThrowTeamNameAlreadyExistsException() {
        CreateTeamRequest request = buildCreateCs2TeamRequest();
        when(teamRepository.existsByNameIgnoreCaseAndDiscipline(TEAM_NAME, Discipline.CS2)).thenReturn(true);

        assertThatThrownBy(() -> teamService.createTeam(request, CAPTAIN_ID))
                .isInstanceOf(TeamNameAlreadyExistsException.class);
        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void givenExistingCs2TeamWithMembers_getTeamById_shouldEnrichMembersFromUserQueryService() {
        TeamEntity team = buildCs2TeamEntity();
        List<TeamMemberEntity> members = List.of(buildCaptainMemberEntity(), buildSecondMemberEntity());
        TeamDetailsResponse expected = buildExpectedCs2TeamDetailsWithTwoMembersResponse();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(TEAM_ID)).thenReturn(members);
        when(userQueryService.findUsersByIds(List.of(CAPTAIN_ID, PLAYER_TWO_ID))).thenReturn(Map.of(
                CAPTAIN_ID, buildCaptainProfileSummary(),
                PLAYER_TWO_ID, buildSecondPlayerProfileSummary()
        ));

        TeamDetailsResponse actual = teamService.getTeamById(TEAM_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExistingBrawlStarsTeam_getTeamById_shouldSelectSupercellPlayerTagAsGameIdentifier() {
        TeamEntity team = buildBrawlTeamEntity();
        TeamDetailsResponse expected = buildExpectedBrawlTeamDetailsResponse();
        when(teamRepository.findById(BRAWL_TEAM_ID)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(BRAWL_TEAM_ID))
                .thenReturn(List.of(buildBrawlCaptainMemberEntity()));
        when(userQueryService.findUsersByIds(List.of(CAPTAIN_ID)))
                .thenReturn(Map.of(CAPTAIN_ID, buildCaptainProfileSummary()));

        TeamDetailsResponse actual = teamService.getTeamById(BRAWL_TEAM_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExistingChessTeam_getTeamById_shouldSelectChessUsernameAsGameIdentifier() {
        TeamEntity team = buildChessTeamEntity();
        TeamDetailsResponse expected = buildExpectedChessTeamDetailsResponse();
        when(teamRepository.findById(CHESS_TEAM_ID)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(CHESS_TEAM_ID))
                .thenReturn(List.of(buildChessCaptainMemberEntity()));
        when(userQueryService.findUsersByIds(List.of(CAPTAIN_ID)))
                .thenReturn(Map.of(CAPTAIN_ID, buildCaptainProfileSummary()));

        TeamDetailsResponse actual = teamService.getTeamById(CHESS_TEAM_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingTeam_getTeamById_shouldThrowTeamNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(teamRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getTeamById(missingId))
                .isInstanceOf(TeamNotFoundException.class);
    }

    @Test
    void givenMemberUser_getMyTeams_shouldReturnTeamSummariesWithMemberCounts() {
        List<TeamMemberEntity> userMemberships = List.of(buildCaptainMemberEntity(), buildChessCaptainMemberEntity());
        List<UUID> teamIds = List.of(TEAM_ID, CHESS_TEAM_ID);
        List<TeamSummaryResponse> expected = buildExpectedMyTeamsResponse();
        when(teamMemberRepository.findByUserId(CAPTAIN_ID)).thenReturn(userMemberships);
        when(teamRepository.findByIdIn(teamIds)).thenReturn(List.of(buildCs2TeamEntity(), buildChessTeamEntity()));
        when(teamMemberRepository.findByTeamIdIn(teamIds)).thenReturn(List.of(
                buildCaptainMemberEntity(),
                buildSecondMemberEntity(),
                buildChessCaptainMemberEntity()
        ));

        List<TeamSummaryResponse> actual = teamService.getMyTeams(CAPTAIN_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenUserWithNoTeams_getMyTeams_shouldReturnEmptyList() {
        when(teamMemberRepository.findByUserId(PLAYER_TWO_ID)).thenReturn(List.of());

        List<TeamSummaryResponse> actual = teamService.getMyTeams(PLAYER_TWO_ID);

        assertThat(actual).isEmpty();
        verifyNoInteractions(teamRepository);
    }

    @Test
    void givenExistingTeams_findTeamByIdAndFindTeamsByIds_shouldReturnTeamViews() {
        TeamEntity team = buildCs2TeamEntity();
        List<TeamMemberEntity> members = List.of(buildSecondMemberEntity(), buildCaptainMemberEntity());
        TeamView expectedView = buildExpectedCs2TeamView();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(teamMemberRepository.findByTeamIdOrderByJoinedAtAsc(TEAM_ID))
                .thenReturn(List.of(buildCaptainMemberEntity(), buildSecondMemberEntity()));
        when(teamRepository.findByIdIn(List.of(TEAM_ID))).thenReturn(List.of(team));
        when(teamMemberRepository.findByTeamIdIn(List.of(TEAM_ID))).thenReturn(members);
        when(userQueryService.findUsersByIds(anyCollection())).thenReturn(Map.of(
                CAPTAIN_ID, buildCaptainProfileSummary(),
                PLAYER_TWO_ID, buildSecondPlayerProfileSummary()
        ));

        Optional<TeamView> single = teamService.findTeamById(TEAM_ID);
        Map<UUID, TeamView> batch = teamService.findTeamsByIds(List.of(TEAM_ID));

        assertThat(single).isPresent();
        assertThat(single.get()).usingRecursiveComparison().isEqualTo(expectedView);
        assertThat(batch).containsOnlyKeys(TEAM_ID);
        assertThat(batch.get(TEAM_ID)).usingRecursiveComparison().isEqualTo(expectedView);
    }
}

package com.tournado.core.teams.service;

import com.tournado.core.teams.api.dto.TeamDetailsResponse;
import com.tournado.core.teams.api.dto.TeamInvitePreviewResponse;
import com.tournado.core.teams.api.dto.TeamInviteResponse;
import com.tournado.core.teams.domain.TeamEntity;
import com.tournado.core.teams.domain.TeamInviteEntity;
import com.tournado.core.teams.domain.TeamMemberEntity;
import com.tournado.core.teams.exception.InviteLinkExpiredException;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.exception.PlayerAlreadyInTeamException;
import com.tournado.core.teams.exception.RosterFullException;
import com.tournado.core.teams.exception.RosterLockedException;
import com.tournado.core.teams.exception.TeamNotFoundException;
import com.tournado.core.teams.repository.TeamInviteRepository;
import com.tournado.core.teams.repository.TeamMemberRepository;
import com.tournado.core.teams.repository.TeamRepository;
import java.time.Clock;
import java.time.ZoneOffset;
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
class TeamInviteServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamInviteRepository teamInviteRepository;

    @Mock
    private TournamentRosterLockChecker rosterLockChecker;

    @Mock
    private TeamService teamService;

    private TeamInviteService teamInviteService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        teamInviteService = new TeamInviteService(
                teamRepository,
                teamMemberRepository,
                teamInviteRepository,
                rosterLockChecker,
                teamService,
                buildTeamAuthProperties(),
                fixedClock
        );
    }

    @Test
    void givenCaptainAndOpenRoster_createInvite_shouldPersistActiveInviteAndReturnUrl() {
        TeamEntity team = buildCs2TeamEntity();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(false);
        when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(1);
        when(teamInviteRepository.save(any(TeamInviteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamInviteResponse actual = teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID);

        ArgumentCaptor<TeamInviteEntity> captor = ArgumentCaptor.forClass(TeamInviteEntity.class);
        verify(teamInviteRepository).save(captor.capture());
        TeamInviteEntity savedInvite = captor.getValue();
        assertThat(savedInvite.getToken()).startsWith("inv_");
        assertThat(savedInvite)
                .usingRecursiveComparison()
                .ignoringFields("id", "token")
                .isEqualTo(buildActiveInviteEntity());
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(buildExpectedInviteResponse(savedInvite.getToken()));
    }

    @Test
    void givenMissingTeam_createInvite_shouldThrowTeamNotFoundException() {
        UUID missingTeamId = UUID.randomUUID();
        when(teamRepository.findById(missingTeamId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamInviteService.createInvite(missingTeamId, CAPTAIN_ID))
                .isInstanceOf(TeamNotFoundException.class);
        verifyNoInteractions(teamInviteRepository);
    }

    @Test
    void givenNonCaptainUser_createInvite_shouldThrowOnlyCaptainAllowedException() {
        TeamEntity team = buildCs2TeamEntity();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));

        assertThatThrownBy(() -> teamInviteService.createInvite(TEAM_ID, PLAYER_TWO_ID))
                .isInstanceOf(OnlyCaptainAllowedException.class);
        verifyNoInteractions(teamInviteRepository);
    }

    @Test
    void givenLockedRoster_createInvite_shouldThrowRosterLockedException() {
        TeamEntity team = buildCs2TeamEntity();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(true);

        assertThatThrownBy(() -> teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID))
                .isInstanceOf(RosterLockedException.class);
        verifyNoInteractions(teamInviteRepository);
    }

    @Test
    void givenFullRoster_createInvite_shouldThrowRosterFullException() {
        TeamEntity team = buildCs2TeamEntity();
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(false);
        when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(5);

        assertThatThrownBy(() -> teamInviteService.createInvite(TEAM_ID, CAPTAIN_ID))
                .isInstanceOf(RosterFullException.class);
        verifyNoInteractions(teamInviteRepository);
    }

    @Test
    void givenValidActiveInvite_previewInvite_shouldReturnTeamPreview() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        TeamEntity team = buildCs2TeamEntity();
        TeamInvitePreviewResponse expected = buildExpectedInvitePreviewResponse();
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(invite));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(2);

        TeamInvitePreviewResponse actual = teamInviteService.previewInvite(INVITE_TOKEN);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenExpiredInvite_previewInvite_shouldThrowInviteLinkExpiredException() {
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(buildExpiredInviteEntity()));

        assertThatThrownBy(() -> teamInviteService.previewInvite(INVITE_TOKEN))
                .isInstanceOf(InviteLinkExpiredException.class);
    }

    @Test
    void givenInactiveInvite_previewInvite_shouldThrowInviteLinkExpiredException() {
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(buildInactiveInviteEntity()));

        assertThatThrownBy(() -> teamInviteService.previewInvite(INVITE_TOKEN))
                .isInstanceOf(InviteLinkExpiredException.class);
    }

    @Test
    void givenValidInviteAndEligibleUser_joinTeamByToken_shouldPersistMemberAndReturnTeamDetails() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        TeamEntity team = buildCs2TeamEntity();
        TeamDetailsResponse expected = buildExpectedCs2TeamDetailsWithTwoMembersResponse();
        TeamMemberEntity expectedMember = buildSecondMemberEntity();
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(invite));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(false);
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, PLAYER_TWO_ID)).thenReturn(false);
        when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(1);
        when(teamMemberRepository.save(any(TeamMemberEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamService.getTeamById(TEAM_ID)).thenReturn(expected);

        TeamDetailsResponse actual = teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID);

        ArgumentCaptor<TeamMemberEntity> captor = ArgumentCaptor.forClass(TeamMemberEntity.class);
        verify(teamMemberRepository).save(captor.capture());
        assertThat(captor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id", "joinedAt")
                .isEqualTo(expectedMember);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenLockedRoster_joinTeamByToken_shouldThrowRosterLockedException() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        TeamEntity team = buildCs2TeamEntity();
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(invite));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(true);

        assertThatThrownBy(() -> teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID))
                .isInstanceOf(RosterLockedException.class);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void givenExistingTeamMember_joinTeamByToken_shouldThrowPlayerAlreadyInTeamException() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        TeamEntity team = buildCs2TeamEntity();
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(invite));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(false);
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, PLAYER_TWO_ID)).thenReturn(true);

        assertThatThrownBy(() -> teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID))
                .isInstanceOf(PlayerAlreadyInTeamException.class);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void givenFullRoster_joinTeamByToken_shouldThrowRosterFullException() {
        TeamInviteEntity invite = buildActiveInviteEntity();
        TeamEntity team = buildCs2TeamEntity();
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.of(invite));
        when(teamRepository.findById(TEAM_ID)).thenReturn(Optional.of(team));
        when(rosterLockChecker.isTeamRosterLocked(TEAM_ID)).thenReturn(false);
        when(teamMemberRepository.existsByTeamIdAndUserId(TEAM_ID, PLAYER_TWO_ID)).thenReturn(false);
        when(teamMemberRepository.countByTeamId(TEAM_ID)).thenReturn(5);

        assertThatThrownBy(() -> teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID))
                .isInstanceOf(RosterFullException.class);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void givenMissingInviteToken_joinTeamByToken_shouldThrowInviteLinkExpiredException() {
        when(teamInviteRepository.findByToken(INVITE_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamInviteService.joinTeamByToken(INVITE_TOKEN, PLAYER_TWO_ID))
                .isInstanceOf(InviteLinkExpiredException.class);
        verifyNoInteractions(teamMemberRepository);
    }
}

package com.tournado.core.matches.service;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.matches.api.dto.MatchCredentialsResponse;
import com.tournado.core.matches.api.dto.MatchRoomResponse;
import com.tournado.core.matches.api.dto.ReadyResponse;
import com.tournado.core.matches.domain.MatchBanEntity;
import com.tournado.core.matches.domain.MatchCredentialsEntity;
import com.tournado.core.matches.domain.MatchEntity;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.exception.BanPhaseCompletedException;
import com.tournado.core.matches.exception.MapAlreadyBannedException;
import com.tournado.core.matches.exception.MatchAccessDeniedException;
import com.tournado.core.matches.exception.MatchNotFoundException;
import com.tournado.core.matches.exception.MatchNotInProgressException;
import com.tournado.core.matches.exception.NotYourBanTurnException;
import com.tournado.core.matches.repository.MatchBanRepository;
import com.tournado.core.matches.repository.MatchCredentialsRepository;
import com.tournado.core.matches.repository.MatchRepository;
import com.tournado.core.teams.exception.OnlyCaptainAllowedException;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.matches.MatchTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchFlowServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private MatchBanRepository matchBanRepository;

    @Mock
    private MatchCredentialsRepository matchCredentialsRepository;

    @Mock
    private TeamQueryService teamQueryService;

    @Mock
    private TournamentQueryService tournamentQueryService;

    private MatchFlowService matchFlowService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        matchFlowService = new MatchFlowService(
                matchRepository,
                matchBanRepository,
                matchCredentialsRepository,
                teamQueryService,
                tournamentQueryService,
                fixedClock
        );
    }

    @Test
    void givenMissingMatch_getRoom_shouldThrowMatchNotFoundException() {
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchFlowService.getRoom(MATCH_ID, CAPTAIN1_ID))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void givenPendingMatchAndCaptain1_markReady_shouldOpenReadyCheckAndMarkTeam1Ready() {
        MatchEntity match = buildPendingMatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        ReadyResponse response = matchFlowService.markReady(MATCH_ID, CAPTAIN1_ID);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        assertThat(response).isEqualTo(new ReadyResponse(true));
        assertThat(captor.getValue().getStatus()).isEqualTo(MatchStatus.READY_CHECK);
        assertThat(captor.getValue().isTeam1Ready()).isTrue();
        assertThat(captor.getValue().isTeam2Ready()).isFalse();
        assertThat(captor.getValue().getReadyDeadlineAt()).isEqualTo(NOW.plusSeconds(600L));
    }

    @Test
    void givenNonCaptainUser_markReady_shouldThrowOnlyCaptainAllowedException() {
        MatchEntity match = buildPendingMatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> matchFlowService.markReady(MATCH_ID, OUTSIDER_USER_ID))
                .isInstanceOf(OnlyCaptainAllowedException.class);
        verify(matchRepository, never()).save(any());
    }

    @Test
    void givenBothCaptainsReadyForCs2Tournament_markReady_shouldTransitionToPickBanWithBanTurnAssigned() {
        MatchEntity match = buildReadyCheckMatchEntityWithTeam1Ready();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));

        ReadyResponse response = matchFlowService.markReady(MATCH_ID, CAPTAIN2_ID);

        ArgumentCaptor<MatchEntity> captor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(captor.capture());
        assertThat(response).isEqualTo(new ReadyResponse(true));
        assertThat(captor.getValue().getStatus()).isEqualTo(MatchStatus.PICK_BAN);
        assertThat(captor.getValue().getCurrentBanTurnTeamId()).isIn(TEAM1_ID, TEAM2_ID);
        assertThat(captor.getValue().getBanDeadlineAt()).isEqualTo(NOW.plusSeconds(30L));
    }

    @Test
    void givenBothCaptainsReadyForChessTournament_markReady_shouldSkipVetoAndGenerateChessCredentials() {
        MatchEntity match = buildReadyCheckMatchEntityWithTeam1Ready();
        match.setTournamentId(CHESS_TOURNAMENT_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CHESS_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildChessTournamentResponse()));
        when(matchCredentialsRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());

        matchFlowService.markReady(MATCH_ID, CAPTAIN2_ID);

        ArgumentCaptor<MatchEntity> matchCaptor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(matchCaptor.capture());
        assertThat(matchCaptor.getValue().getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(matchCaptor.getValue().getCurrentBanTurnTeamId()).isNull();

        ArgumentCaptor<MatchCredentialsEntity> credentialsCaptor = ArgumentCaptor.forClass(MatchCredentialsEntity.class);
        verify(matchCredentialsRepository).save(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue().getChessGameUrl()).isEqualTo("https://www.chess.com/live/" + MATCH_ID);
    }

    @Test
    void givenMatchAlreadyInPickBan_markReady_shouldBeIdempotentNoOp() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        ReadyResponse response = matchFlowService.markReady(MATCH_ID, CAPTAIN1_ID);

        assertThat(response).isEqualTo(new ReadyResponse(true));
        verify(matchRepository, never()).save(any());
    }

    @Test
    void givenValidTurnAndMap_banMap_shouldPersistBanAndFlipTurnToOtherTeam() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID)).thenReturn(List.of());

        MatchRoomResponse response = matchFlowService.banMap(MATCH_ID, CAPTAIN1_ID, "Vertigo");

        ArgumentCaptor<MatchBanEntity> banCaptor = ArgumentCaptor.forClass(MatchBanEntity.class);
        verify(matchBanRepository).save(banCaptor.capture());
        assertThat(banCaptor.getValue().getTeamId()).isEqualTo(TEAM1_ID);
        assertThat(banCaptor.getValue().getMapName()).isEqualTo("Vertigo");
        assertThat(banCaptor.getValue().getBanOrder()).isEqualTo(1);

        ArgumentCaptor<MatchEntity> matchCaptor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(matchCaptor.capture());
        assertThat(matchCaptor.getValue().getCurrentBanTurnTeamId()).isEqualTo(TEAM2_ID);
        assertThat(matchCaptor.getValue().getStatus()).isEqualTo(MatchStatus.PICK_BAN);
        assertThat(response.currentBanTurnTeamId()).isEqualTo(TEAM2_ID);
    }

    @Test
    void givenLastRemainingMap_banMap_shouldDecideMapAndGenerateCs2Credentials() {
        MatchEntity match = buildPickBanMatchEntity(TEAM2_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        List<MatchBanEntity> alreadyBanned = List.of(
                buildMatchBanEntity(TEAM1_ID, "Vertigo", 1),
                buildMatchBanEntity(TEAM2_ID, "Mirage", 2),
                buildMatchBanEntity(TEAM1_ID, "Inferno", 3),
                buildMatchBanEntity(TEAM2_ID, "Nuke", 4),
                buildMatchBanEntity(TEAM1_ID, "Ancient", 5)
        );
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID)).thenReturn(alreadyBanned);
        when(matchCredentialsRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());

        MatchRoomResponse response = matchFlowService.banMap(MATCH_ID, CAPTAIN2_ID, "Anubis");

        ArgumentCaptor<MatchEntity> matchCaptor = ArgumentCaptor.forClass(MatchEntity.class);
        verify(matchRepository).save(matchCaptor.capture());
        assertThat(matchCaptor.getValue().getDecidedMap()).isEqualTo("Dust II");
        assertThat(matchCaptor.getValue().getStatus()).isEqualTo(MatchStatus.IN_PROGRESS);
        assertThat(matchCaptor.getValue().getCurrentBanTurnTeamId()).isNull();
        assertThat(response.decidedMap()).isEqualTo("Dust II");

        ArgumentCaptor<MatchCredentialsEntity> credentialsCaptor = ArgumentCaptor.forClass(MatchCredentialsEntity.class);
        verify(matchCredentialsRepository).save(credentialsCaptor.capture());
        assertThat(credentialsCaptor.getValue().getCs2ConnectString()).isEqualTo(CS2_SERVER_CONNECT);
    }

    @Test
    void givenWrongTurnCaptain_banMap_shouldThrowNotYourBanTurnException() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> matchFlowService.banMap(MATCH_ID, CAPTAIN2_ID, "Vertigo"))
                .isInstanceOf(NotYourBanTurnException.class);
        verify(matchBanRepository, never()).save(any());
    }

    @Test
    void givenAlreadyBannedMap_banMap_shouldThrowMapAlreadyBannedException() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID))
                .thenReturn(List.of(buildMatchBanEntity(TEAM2_ID, "Vertigo", 1)));

        assertThatThrownBy(() -> matchFlowService.banMap(MATCH_ID, CAPTAIN1_ID, "Vertigo"))
                .isInstanceOf(MapAlreadyBannedException.class);
        verify(matchBanRepository, never()).save(any());
    }

    @Test
    void givenMapOutsideTournamentPool_banMap_shouldThrowMapAlreadyBannedException() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> matchFlowService.banMap(MATCH_ID, CAPTAIN1_ID, "de_overpass"))
                .isInstanceOf(MapAlreadyBannedException.class);
    }

    @Test
    void givenMatchNotInPickBanStatus_banMap_shouldThrowBanPhaseCompletedException() {
        MatchEntity match = buildPendingMatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> matchFlowService.banMap(MATCH_ID, CAPTAIN1_ID, "Vertigo"))
                .isInstanceOf(BanPhaseCompletedException.class);
    }

    @Test
    void givenParticipantAndGeneratedCredentials_getRoom_shouldIncludeCredentialsSnapshot() {
        MatchEntity match = buildInProgressCs2MatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID)).thenReturn(List.of());
        when(matchCredentialsRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(buildCs2CredentialsEntity()));

        MatchRoomResponse response = matchFlowService.getRoom(MATCH_ID, CAPTAIN1_ID);

        assertThat(response.credentials()).isEqualTo(
                new MatchCredentialsResponse(Discipline.CS2, CS2_SERVER_CONNECT, null, null, "Dust II")
        );
    }

    @Test
    void givenNonParticipant_getRoom_shouldHideCredentials() {
        MatchEntity match = buildInProgressCs2MatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(matchBanRepository.findByMatchIdOrderByBanOrderAsc(MATCH_ID)).thenReturn(List.of());

        MatchRoomResponse response = matchFlowService.getRoom(MATCH_ID, OUTSIDER_USER_ID);

        assertThat(response.credentials()).isNull();
        verify(matchCredentialsRepository, never()).findByMatchId(any());
    }

    @Test
    void givenNonParticipant_getCredentials_shouldThrowMatchAccessDeniedException() {
        MatchEntity match = buildInProgressCs2MatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());

        assertThatThrownBy(() -> matchFlowService.getCredentials(MATCH_ID, OUTSIDER_USER_ID))
                .isInstanceOf(MatchAccessDeniedException.class);
    }

    @Test
    void givenCredentialsNotYetGenerated_getCredentials_shouldThrowMatchNotInProgressException() {
        MatchEntity match = buildPickBanMatchEntity(TEAM1_ID);
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(matchCredentialsRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchFlowService.getCredentials(MATCH_ID, CAPTAIN1_ID))
                .isInstanceOf(MatchNotInProgressException.class);
    }

    @Test
    void givenParticipant_getCredentials_shouldReturnCs2ConnectStringAndDecidedMap() {
        MatchEntity match = buildInProgressCs2MatchEntity();
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
        when(teamQueryService.findTeamsByIds(Set.of(TEAM1_ID, TEAM2_ID))).thenReturn(buildTeamsById());
        when(matchCredentialsRepository.findByMatchId(MATCH_ID)).thenReturn(Optional.of(buildCs2CredentialsEntity()));
        when(tournamentQueryService.findTournamentById(CS2_TOURNAMENT_ID))
                .thenReturn(Optional.of(buildCs2TournamentResponse()));

        MatchCredentialsResponse response = matchFlowService.getCredentials(MATCH_ID, CAPTAIN2_ID);

        assertThat(response).isEqualTo(
                new MatchCredentialsResponse(Discipline.CS2, CS2_SERVER_CONNECT, null, null, "Dust II")
        );
    }
}

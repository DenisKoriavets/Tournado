package com.tournado.core.bracket.service;

import com.tournado.core.bracket.api.dto.BracketResponse;
import com.tournado.core.bracket.api.dto.GenerateBracketResponse;
import com.tournado.core.bracket.api.dto.StandingsResponse;
import com.tournado.core.bracket.domain.BracketEntity;
import com.tournado.core.bracket.domain.TournamentStandingEntity;
import com.tournado.core.bracket.exception.BracketAlreadyGeneratedException;
import com.tournado.core.bracket.exception.InsufficientTeamsForBracketException;
import com.tournado.core.bracket.repository.BracketRepository;
import com.tournado.core.bracket.repository.TournamentStandingRepository;
import com.tournado.core.matches.domain.MatchStatus;
import com.tournado.core.matches.service.BracketMatchSeed;
import com.tournado.core.matches.service.MatchBracketService;
import com.tournado.core.teams.service.TeamQueryService;
import com.tournado.core.tournaments.exception.TournamentNotFoundException;
import com.tournado.core.tournaments.service.TournamentBracketService;
import com.tournado.core.tournaments.service.TournamentQueryService;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.bracket.BracketTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private BracketRepository bracketRepository;

    @Mock
    private TournamentStandingRepository standingRepository;

    @Mock
    private TournamentBracketService tournamentBracketService;

    @Mock
    private TournamentQueryService tournamentQueryService;

    @Mock
    private MatchBracketService matchBracketService;

    @Mock
    private TeamQueryService teamQueryService;

    @Captor
    private ArgumentCaptor<List<BracketMatchSeed>> matchSeedsCaptor;

    @Captor
    private ArgumentCaptor<List<TournamentStandingEntity>> standingsCaptor;

    private BracketService bracketService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        Random seededRandom = new Random(42L);
        bracketService = new BracketService(
                bracketRepository,
                standingRepository,
                tournamentBracketService,
                tournamentQueryService,
                matchBracketService,
                teamQueryService,
                fixedClock,
                seededRandom
        );
    }

    @Test
    void givenSixCheckedInTeams_generateBracket_shouldCreateSingleEliminationWithTwoByesAndAdvanceToRoundTwo() {
        List<UUID> checkedInTeams = buildSixCheckedInTeams();
        GenerateBracketResponse expectedResponse = buildExpectedSixTeamsGenerateResponse();
        when(bracketRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(false);
        when(tournamentBracketService.prepareAndStartTournament(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .thenReturn(checkedInTeams);
        when(bracketRepository.save(any(BracketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateBracketResponse response = bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("bracketId")
                .isEqualTo(expectedResponse);
        verify(matchBracketService).createBracketMatches(matchSeedsCaptor.capture());
        List<BracketMatchSeed> generatedMatches = matchSeedsCaptor.getValue();
        List<BracketMatchSeed> round1Matches = generatedMatches.stream()
                .filter(m -> m.roundNumber() == 1)
                .toList();
        List<BracketMatchSeed> round2Matches = generatedMatches.stream()
                .filter(m -> m.roundNumber() == 2)
                .toList();
        assertThat(generatedMatches).hasSize(7);
        assertThat(round1Matches.stream().filter(BracketMatchSeed::isBye).count()).isEqualTo(2);
        assertThat(round1Matches.stream().filter(m -> m.status() == MatchStatus.READY_CHECK).count()).isEqualTo(2);
        assertThat(round2Matches.stream().filter(m -> m.team1Id() != null || m.team2Id() != null).count()).isEqualTo(2);
    }

    @Test
    void givenFewerThanFourCheckedInTeams_generateBracket_shouldThrowInsufficientTeamsException() {
        List<UUID> insufficientTeams = List.of(TEAM_1_ID, TEAM_2_ID, TEAM_3_ID);
        when(bracketRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(false);
        when(tournamentBracketService.prepareAndStartTournament(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .thenReturn(insufficientTeams);

        assertThatThrownBy(() -> bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .isInstanceOf(InsufficientTeamsForBracketException.class)
                .hasMessageContaining("At least 4 checked-in teams are required");
    }

    @Test
    void givenAlreadyGeneratedBracket_generateBracket_shouldThrowBracketAlreadyGeneratedException() {
        when(bracketRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> bracketService.generateBracket(TOURNAMENT_ID, ORGANIZER_USER_ID))
                .isInstanceOf(BracketAlreadyGeneratedException.class)
                .hasMessageContaining(TOURNAMENT_ID.toString());
    }

    @Test
    void givenExistingBracketAndMatches_getBracket_shouldReturnStructuredRoundsWithTeamNames() {
        BracketEntity bracket = buildBracketEntity(2);
        BracketResponse expected = buildExpectedFourTeamBracketResponse();
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID))
                .thenReturn(Optional.of(buildInProgressTournamentResponse()));
        when(bracketRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.of(bracket));
        when(matchBracketService.findMatchesByTournamentId(TOURNAMENT_ID))
                .thenReturn(buildFourTeamBracketMatches());
        when(teamQueryService.findTeamsByIds(Set.of(TEAM_1_ID, TEAM_2_ID, TEAM_3_ID, TEAM_4_ID)))
                .thenReturn(buildFourTeamsMap());

        BracketResponse actual = bracketService.getBracket(TOURNAMENT_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenNonExistentTournament_getBracket_shouldThrowTournamentNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(tournamentQueryService.findTournamentById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bracketService.getBracket(unknownId))
                .isInstanceOf(TournamentNotFoundException.class);
    }

    @Test
    void givenNonFinalMatchCompleted_advanceWinnerOnMatchCompleted_shouldAdvanceWinnerToNextRoundSlot() {
        BracketEntity bracket = buildBracketEntity(3);
        when(bracketRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.of(bracket));

        bracketService.advanceWinnerOnMatchCompleted(TOURNAMENT_ID, 1, 2, TEAM_3_ID);

        verify(matchBracketService).advanceTeamToSlot(
                TOURNAMENT_ID,
                2,
                1,
                false,
                TEAM_3_ID,
                READY_DEADLINE
        );
        verify(tournamentBracketService, never()).completeTournament(any());
    }

    @Test
    void givenFinalMatchCompleted_advanceWinnerOnMatchCompleted_shouldPersistTopThreeStandingsAndCompleteTournament() {
        BracketEntity bracket = buildBracketEntity(2);
        List<TournamentStandingEntity> expectedStandings = buildThreeStandingEntities();
        when(bracketRepository.findByTournamentId(TOURNAMENT_ID)).thenReturn(Optional.of(bracket));
        when(standingRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(false);
        when(matchBracketService.findMatchesByTournamentId(TOURNAMENT_ID))
                .thenReturn(buildFourTeamBracketMatches());

        bracketService.advanceWinnerOnMatchCompleted(TOURNAMENT_ID, 2, 1, TEAM_1_ID);

        verify(standingRepository).saveAll(standingsCaptor.capture());
        assertThat(standingsCaptor.getValue())
                .usingRecursiveComparison()
                .ignoringFields("id")
                .isEqualTo(expectedStandings);
        verify(tournamentBracketService).completeTournament(TOURNAMENT_ID);
    }

    @Test
    void givenPersistedStandings_getStandings_shouldReturnOrderedPlacesWithTeamNames() {
        StandingsResponse expected = buildExpectedStandingsResponse();
        when(tournamentQueryService.findTournamentById(TOURNAMENT_ID))
                .thenReturn(Optional.of(buildInProgressTournamentResponse()));
        when(standingRepository.findByTournamentIdOrderByRankPlaceAsc(TOURNAMENT_ID))
                .thenReturn(buildThreeStandingEntities());
        when(teamQueryService.findTeamsByIds(List.of(TEAM_1_ID, TEAM_2_ID, TEAM_4_ID)))
                .thenReturn(buildFourTeamsMap());

        StandingsResponse actual = bracketService.getStandings(TOURNAMENT_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
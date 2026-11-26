package com.tournado.core.support;

import com.tournado.core.common.domain.Discipline;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Plain-SQL test data builders for integration tests.
 *
 * <p>Only the "Given" part of a scenario is inserted directly (states the public API cannot produce on demand, such as
 * a tournament whose registration deadline is already in the past or a match that is already IN_PROGRESS).
 * The "When" part of every test always goes through the real HTTP endpoint.
 */
public final class DbFixtures {

    private final JdbcTemplate jdbc;

    DbFixtures(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** A player with all three game identifiers filled in (eligible for any discipline). */
    public UUID player(String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, email, steam_id, display_name, supercell_player_tag, chess_username) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id,
                id + "@test.tournado.ua",
                randomSteamId(),
                displayName,
                "#" + id.toString().substring(0, 8).toUpperCase().replaceAll("[^0-9A-Z]", "P"),
                "chess_" + id.toString().substring(0, 8)
        );
        return id;
    }

    /** A player who logged in by email and never linked any game identifier (US-02 Sc.4). */
    public UUID playerWithoutGameIds(String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, display_name) VALUES (?, ?, ?)", id, id + "@test.tournado.ua",
                displayName);
        return id;
    }

    /** A team whose roster is the captain followed by the given players. */
    public UUID team(String name, Discipline discipline, UUID captainId, UUID... players) {
        UUID teamId = UUID.randomUUID();
        jdbc.update("INSERT INTO teams (id, name, discipline, captain_id) VALUES (?, ?, ?, ?)",
                teamId, name, discipline.name(), captainId);
        addMember(teamId, captainId);
        for (UUID player : players) {
            addMember(teamId, player);
        }
        return teamId;
    }

    /** A team whose roster is full for its discipline: the captain plus freshly created players. */
    public UUID fullTeam(String name, Discipline discipline, UUID captainId) {
        UUID[] players = new UUID[discipline.getTeamSize() - 1];
        for (int i = 0; i < players.length; i++) {
            players[i] = player(name + " player " + (i + 2));
        }
        return team(name, discipline, captainId, players);
    }

    public void addMember(UUID teamId, UUID userId) {
        jdbc.update("INSERT INTO team_members (id, team_id, user_id) VALUES (?, ?, ?)",
                UUID.randomUUID(), teamId, userId);
    }

    public void invite(UUID teamId, UUID createdBy, String token, boolean active, Instant expiresAt) {
        jdbc.update("INSERT INTO team_invites (id, team_id, token, created_by, is_active, expires_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), teamId, token, createdBy, active, Timestamp.from(expiresAt));
    }

    /**
     * A Single Elimination tournament. Check-in opens one hour after the registration deadline and the start is one
     * hour after that, which satisfies {@code chk_tournaments_schedule_order}.
     */
    public UUID tournament(UUID organizerId, Discipline discipline, int slotLimit, String status,
                           Instant registrationDeadline) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tournaments (id, name, discipline, slot_limit, status, registration_deadline_at, "
                        + "check_in_opens_at, start_time_at, created_by_user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                "IT Cup " + id.toString().substring(0, 8),
                discipline.name(),
                slotLimit,
                status,
                Timestamp.from(registrationDeadline),
                Timestamp.from(registrationDeadline.plus(1, ChronoUnit.HOURS)),
                Timestamp.from(registrationDeadline.plus(2, ChronoUnit.HOURS)),
                organizerId
        );
        return id;
    }

    /** A tournament that is accepting registrations, with the deadline two days from now. */
    public UUID openTournament(UUID organizerId, Discipline discipline, int slotLimit) {
        return tournament(organizerId, discipline, slotLimit, "REGISTRATION_OPEN",
                Instant.now().plus(2, ChronoUnit.DAYS));
    }

    public void registration(UUID tournamentId, UUID teamId, String status) {
        jdbc.update("INSERT INTO registrations (id, tournament_id, team_id, status) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), tournamentId, teamId, status);
    }

    public UUID bracket(UUID tournamentId, int totalRounds) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO brackets (id, tournament_id, total_rounds) VALUES (?, ?, ?)",
                id, tournamentId, totalRounds);
        return id;
    }

    /** A bracket match; {@code team1Id}/{@code team2Id} may be null for a not-yet-filled next-round slot. */
    public UUID match(UUID tournamentId, UUID bracketId, int round, int order, UUID team1Id, UUID team2Id,
                      String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO matches (id, tournament_id, bracket_id, round_number, match_order, team1_id, "
                        + "team2_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, tournamentId, bracketId, round, order, team1Id, team2Id, status);
        return id;
    }

    /** An OPEN dispute with a stored screenshot reference (the file itself is not needed to resolve it). */
    public UUID dispute(UUID matchId, UUID openedByTeamId, UUID openedByUserId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO disputes (id, match_id, opened_by_team_id, opened_by_user_id, status, "
                        + "screenshot_file_path, screenshot_mime_type, screenshot_file_size_bytes) "
                        + "VALUES (?, ?, ?, ?, 'OPEN', ?, 'image/png', 1024)",
                id, matchId, openedByTeamId, openedByUserId, "it/" + id + ".png");
        return id;
    }

    /** The asynchronous AI result for a dispute (US-32); scores are null for UNRECOGNIZED / FAILED. */
    public void aiAnalysis(UUID disputeId, String status, Integer team1Score, Integer team2Score) {
        jdbc.update("INSERT INTO ai_analysis_results (id, dispute_id, status, recognized_team1_score, "
                        + "recognized_team2_score, confidence, nicknames_matched_all) VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), disputeId, status, team1Score, team2Score,
                team1Score == null ? null : new BigDecimal("0.920"), team1Score != null);
    }

    public int count(String sql, Object... args) {
        Integer value = jdbc.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    private static String randomSteamId() {
        return "7656119" + String.format("%010d", ThreadLocalRandom.current().nextLong(10_000_000_000L));
    }
}

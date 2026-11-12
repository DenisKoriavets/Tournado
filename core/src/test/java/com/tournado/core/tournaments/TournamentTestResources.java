package com.tournado.core.tournaments;

import com.tournado.core.tournaments.api.dto.CreateTournamentRequest;
import com.tournado.core.tournaments.api.dto.TournamentResponse;
import com.tournado.core.tournaments.api.dto.TournamentSummaryResponse;
import com.tournado.core.tournaments.domain.Discipline;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentFormat;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public final class TournamentTestResources {

    private TournamentTestResources() {
    }

    public static final UUID TOURNAMENT_ID = UUID.fromString("9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d");
    public static final UUID SYSTEM_ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final Instant NOW = Instant.parse("2026-11-01T12:00:00Z");
    public static final Instant REGISTRATION_DEADLINE = NOW.plus(2, ChronoUnit.HOURS);
    public static final Instant CHECK_IN_OPENS = NOW.plus(3, ChronoUnit.HOURS);
    public static final Instant START_TIME = NOW.plus(4, ChronoUnit.HOURS);

    public static CreateTournamentRequest buildValidCreateTournamentRequest() {
        return new CreateTournamentRequest(
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                16,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                "connect 194.44.12.5:27015; password kma_cup"
        );
    }

    public static CreateTournamentRequest buildInvalidScheduleTournamentRequest() {
        return new CreateTournamentRequest(
                "KMA CS2 Autumn Cup 2026",
                "Invalid schedule order.",
                Discipline.CS2,
                16,
                START_TIME,
                CHECK_IN_OPENS,
                REGISTRATION_DEADLINE,
                null
        );
    }

    public static TournamentEntity buildTournamentEntity(UUID id, Discipline discipline, int slotLimit) {
        TournamentEntity entity = new TournamentEntity();
        entity.setId(id);
        entity.setName("KMA CS2 Autumn Cup 2026");
        entity.setDescription("Official faculty tournament. Overtime rules: MR3 $10k.");
        entity.setDiscipline(discipline);
        entity.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        entity.setSlotLimit(slotLimit);
        entity.setStatus(TournamentStatus.REGISTRATION_OPEN);
        entity.setRegistrationDeadlineAt(REGISTRATION_DEADLINE);
        entity.setCheckInOpensAt(CHECK_IN_OPENS);
        entity.setStartTimeAt(START_TIME);
        entity.setCs2ServerConnect("connect 194.44.12.5:27015; password kma_cup");
        entity.setCreatedByUserId(SYSTEM_ORGANIZER_ID);
        entity.setCreatedAt(NOW);
        entity.setUpdatedAt(NOW);
        entity.setVersion(0L);

        List<String> mapNames = discipline.getDefaultMapPool();
        for (int i = 0; i < mapNames.size(); i++) {
            entity.addMap(mapNames.get(i), i);
        }

        return entity;
    }

    public static TournamentResponse buildExpectedTournamentResponse() {
        return new TournamentResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                "connect 194.44.12.5:27015; password kma_cup",
                List.of("de_mirage", "de_inferno", "de_nuke", "de_ancient", "de_anubis", "de_dust2", "de_vertigo"),
                0,
                NOW
        );
    }

    public static TournamentSummaryResponse buildExpectedTournamentSummaryResponse() {
        return new TournamentSummaryResponse(
                TOURNAMENT_ID,
                TOURNAMENT_ID.toString(),
                "KMA CS2 Autumn Cup 2026",
                "Official faculty tournament. Overtime rules: MR3 $10k.",
                Discipline.CS2,
                5,
                TournamentFormat.SINGLE_ELIMINATION,
                16,
                TournamentStatus.REGISTRATION_OPEN,
                REGISTRATION_DEADLINE,
                CHECK_IN_OPENS,
                START_TIME,
                0,
                NOW
        );
    }
}

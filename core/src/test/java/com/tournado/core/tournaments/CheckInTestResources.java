package com.tournado.core.tournaments;

import com.tournado.core.common.domain.Discipline;
import com.tournado.core.tournaments.api.dto.CheckInRequest;
import com.tournado.core.tournaments.api.dto.CheckInResponse;
import com.tournado.core.tournaments.domain.RegistrationStatus;
import com.tournado.core.tournaments.domain.TournamentEntity;
import com.tournado.core.tournaments.domain.TournamentStatus;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static com.tournado.core.tournaments.TournamentTestResources.*;

public final class CheckInTestResources {

    private CheckInTestResources() {
    }

    public static final Instant ACTIVE_CHECKIN_TIME = CHECK_IN_OPENS.plus(10, ChronoUnit.MINUTES);
    public static final Instant BEFORE_CHECKIN_TIME = CHECK_IN_OPENS.minus(5, ChronoUnit.MINUTES);
    public static final Instant AFTER_START_TIME = START_TIME.plus(1, ChronoUnit.MINUTES);

    public static CheckInRequest buildCheckInRequest() {
        return new CheckInRequest(TEAM_ID);
    }

    public static CheckInResponse buildExpectedCheckInResponse() {
        return new CheckInResponse(RegistrationStatus.CHECKED_IN, ACTIVE_CHECKIN_TIME);
    }

    public static TournamentEntity buildTournamentWithStatus(TournamentStatus status) {
        TournamentEntity entity = buildTournamentEntity(TOURNAMENT_ID, Discipline.CS2, 16);
        entity.setStatus(status);
        return entity;
    }

    public static ProblemDetail buildCheckInNotOpenProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Check-in window is not open yet for tournament '" + TOURNAMENT_ID + "'."
        );
        problem.setType(URI.create("https://tournado.ua/errors/CHECKIN_NOT_OPEN"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/check-in"));
        problem.setProperty("code", "CHECKIN_NOT_OPEN");
        return problem;
    }

    public static ProblemDetail buildCheckInExpiredProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Check-in window has closed for tournament '" + TOURNAMENT_ID + "'."
        );
        problem.setType(URI.create("https://tournado.ua/errors/CHECKIN_EXPIRED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/check-in"));
        problem.setProperty("code", "CHECKIN_EXPIRED");
        return problem;
    }

    public static ProblemDetail buildCheckInAlreadyConfirmedProblemDetail() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Captain has already confirmed check-in for team '" + TEAM_ID + "' in tournament '" + TOURNAMENT_ID + "'."
        );
        problem.setType(URI.create("https://tournado.ua/errors/CHECKIN_ALREADY_CONFIRMED"));
        problem.setTitle("Conflict");
        problem.setInstance(URI.create("/api/v1/tournaments/" + TOURNAMENT_ID + "/check-in"));
        problem.setProperty("code", "CHECKIN_ALREADY_CONFIRMED");
        return problem;
    }
}
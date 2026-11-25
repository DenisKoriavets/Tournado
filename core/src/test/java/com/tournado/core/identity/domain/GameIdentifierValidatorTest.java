package com.tournado.core.identity.domain;

import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.identity.domain.GameIdentifierValidator.NormalizedGameIdentifiers;
import com.tournado.core.identity.exception.InvalidGameIdentifierException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.tournado.core.identity.IdentityTestResources.*;
import static org.assertj.core.api.Assertions.*;

class GameIdentifierValidatorTest {

    private final GameIdentifierValidator validator = new GameIdentifierValidator();

    @Test
    void givenValidWithWhitespaceAndLowercaseTag_validateAndNormalize_shouldTrimAndUppercase() {
        NormalizedGameIdentifiers expected = buildExpectedNormalizedIdentifiers();

        NormalizedGameIdentifiers actual = validator.validateAndNormalize(
                "  " + STEAM_ID + " ",
                " #2pp98l9 ",
                " " + CHESS_USERNAME + " "
        );

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenBlankAndNullIdentifiers_validateAndNormalize_shouldReturnAllNulls() {
        NormalizedGameIdentifiers expected = new NormalizedGameIdentifiers(null, null, null);

        NormalizedGameIdentifiers actual = validator.validateAndNormalize("", "   ", null);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenAllInvalidIdentifiers_validateAndNormalize_shouldThrowWithAllInvalidParams() {
        List<InvalidParam> expectedParams = buildExpectedAllInvalidGameIdentifierParams();

        assertThatThrownBy(() -> validator.validateAndNormalize("12345", "2PP98L9", "ab"))
                .isInstanceOf(InvalidGameIdentifierException.class)
                .satisfies(ex -> {
                    InvalidGameIdentifierException domainEx = (InvalidGameIdentifierException) ex;
                    assertThat(domainEx.getInvalidParams())
                            .usingRecursiveComparison()
                            .isEqualTo(expectedParams);
                });
    }

    @Test
    void givenInvalidSteamIdPrefix_validateAndNormalize_shouldRejectSteamId() {
        List<InvalidParam> expectedParams = List.of(
                new InvalidParam("steamId", "Steam ID must be a 17-digit SteamID64 starting with 7656119.")
        );

        assertThatThrownBy(() -> validator.validateAndNormalize("16561198012345678", SUPERCELL_TAG, CHESS_USERNAME))
                .isInstanceOf(InvalidGameIdentifierException.class)
                .satisfies(ex -> assertThat(((InvalidGameIdentifierException) ex).getInvalidParams())
                        .usingRecursiveComparison()
                        .isEqualTo(expectedParams));
    }

    @Test
    void givenTooShortSupercellTag_validateAndNormalize_shouldRejectSupercellTag() {
        List<InvalidParam> expectedParams = List.of(
                new InvalidParam(
                        "supercellPlayerTag",
                        "Supercell player tag must start with '#' followed by 3 to 14 alphanumeric characters."
                )
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(STEAM_ID, "#AB", CHESS_USERNAME))
                .isInstanceOf(InvalidGameIdentifierException.class)
                .satisfies(ex -> assertThat(((InvalidGameIdentifierException) ex).getInvalidParams())
                        .usingRecursiveComparison()
                        .isEqualTo(expectedParams));
    }

    @Test
    void givenChessUsernameWithIllegalChars_validateAndNormalize_shouldRejectChessUsername() {
        List<InvalidParam> expectedParams = List.of(
                new InvalidParam(
                        "chessUsername",
                        "Chess.com username must be 3-25 characters using letters, digits, underscores, or hyphens."
                )
        );

        assertThatThrownBy(() -> validator.validateAndNormalize(STEAM_ID, SUPERCELL_TAG, "bad user!"))
                .isInstanceOf(InvalidGameIdentifierException.class)
                .satisfies(ex -> assertThat(((InvalidGameIdentifierException) ex).getInvalidParams())
                        .usingRecursiveComparison()
                        .isEqualTo(expectedParams));
    }
}

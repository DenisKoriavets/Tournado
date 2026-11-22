package com.tournado.core.identity.domain;

import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.identity.exception.InvalidGameIdentifierException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class GameIdentifierValidator {

    private static final Pattern STEAM_ID_PATTERN = Pattern.compile("^7656119[0-9]{10}$");
    private static final Pattern SUPERCELL_TAG_PATTERN = Pattern.compile("^#[A-Z0-9]{3,14}$");
    private static final Pattern CHESS_USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,25}$");

    public NormalizedGameIdentifiers validateAndNormalize(
            String rawSteamId,
            String rawSupercellPlayerTag,
            String rawChessUsername) {
        List<InvalidParam> errors = new ArrayList<>();

        String steamId = normalizeSteamId(rawSteamId, errors);
        String supercellPlayerTag = normalizeSupercellTag(rawSupercellPlayerTag, errors);
        String chessUsername = normalizeChessUsername(rawChessUsername, errors);

        if (!errors.isEmpty()) {
            throw new InvalidGameIdentifierException(errors);
        }

        return new NormalizedGameIdentifiers(steamId, supercellPlayerTag, chessUsername);
    }

    private String normalizeSteamId(String rawSteamId, List<InvalidParam> errors) {
        String trimmed = trimToNull(rawSteamId);
        if (trimmed != null && !STEAM_ID_PATTERN.matcher(trimmed).matches()) {
            errors.add(new InvalidParam("steamId", "Steam ID must be a 17-digit SteamID64 starting with 7656119."));
        }
        return trimmed;
    }

    private String normalizeSupercellTag(String rawSupercellPlayerTag, List<InvalidParam> errors) {
        String trimmed = trimToNull(rawSupercellPlayerTag);
        if (trimmed == null) {
            return null;
        }
        String normalized = trimmed.toUpperCase(Locale.ROOT);
        if (!SUPERCELL_TAG_PATTERN.matcher(normalized).matches()) {
            errors.add(new InvalidParam(
                    "supercellPlayerTag",
                    "Supercell player tag must start with '#' followed by 3 to 14 alphanumeric characters."
            ));
        }
        return normalized;
    }

    private String normalizeChessUsername(String rawChessUsername, List<InvalidParam> errors) {
        String trimmed = trimToNull(rawChessUsername);
        if (trimmed != null && !CHESS_USERNAME_PATTERN.matcher(trimmed).matches()) {
            errors.add(new InvalidParam(
                    "chessUsername",
                    "Chess.com username must be 3-25 characters using letters, digits, underscores, or hyphens."
            ));
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record NormalizedGameIdentifiers(
            String steamId,
            String supercellPlayerTag,
            String chessUsername
    ) {
    }
}

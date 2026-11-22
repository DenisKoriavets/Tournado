package com.tournado.core.identity.service;

import com.tournado.core.common.exception.InvalidParam;
import com.tournado.core.identity.api.dto.UpdateGameIdentifiersRequest;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.domain.GameIdentifierValidator;
import com.tournado.core.identity.domain.GameIdentifierValidator.NormalizedGameIdentifiers;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.InvalidGameIdentifierException;
import com.tournado.core.identity.exception.UserNotFoundException;
import com.tournado.core.identity.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService implements UserQueryService {

    private final UserRepository userRepository;
    private final GameIdentifierValidator gameIdentifierValidator;

    public UserProfileService(UserRepository userRepository, GameIdentifierValidator gameIdentifierValidator) {
        this.userRepository = userRepository;
        this.gameIdentifierValidator = gameIdentifierValidator;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateGameIdentifiers(UUID userId, UpdateGameIdentifiersRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        NormalizedGameIdentifiers normalized = gameIdentifierValidator.validateAndNormalize(
                request.steamId(),
                request.supercellPlayerTag(),
                request.chessUsername()
        );

        if (user.getEmail() == null && normalized.steamId() == null) {
            throw new InvalidGameIdentifierException(List.of(new InvalidParam(
                    "steamId",
                    "Cannot remove Steam ID when no email address is linked to the account."
            )));
        }

        user.setSteamId(normalized.steamId());
        user.setSupercellPlayerTag(normalized.supercellPlayerTag());
        user.setChessUsername(normalized.chessUsername());
        UserEntity saved = userRepository.save(user);

        return toProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileSummary> findUserById(UUID userId) {
        return userRepository.findById(userId).map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserProfileSummary> findUsersByIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toMap(UserProfileSummary::id, Function.identity()));
    }

    private UserProfileResponse toProfileResponse(UserEntity user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getSteamId(),
                user.getDisplayName(),
                user.getSupercellPlayerTag(),
                user.getChessUsername(),
                user.getAvatarUrl()
        );
    }

    private UserProfileSummary toSummary(UserEntity user) {
        return new UserProfileSummary(
                user.getId(),
                user.getEmail(),
                user.getSteamId(),
                user.getDisplayName(),
                user.getSupercellPlayerTag(),
                user.getChessUsername(),
                user.getAvatarUrl()
        );
    }
}

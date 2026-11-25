package com.tournado.core.identity.service;

import com.tournado.core.identity.api.dto.UpdateGameIdentifiersRequest;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.domain.GameIdentifierValidator;
import com.tournado.core.identity.domain.UserEntity;
import com.tournado.core.identity.exception.InvalidGameIdentifierException;
import com.tournado.core.identity.exception.UserNotFoundException;
import com.tournado.core.identity.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tournado.core.identity.IdentityTestResources.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(userRepository, new GameIdentifierValidator());
    }

    @Test
    void givenExistingUser_getCurrentUserProfile_shouldReturnUserProfileResponse() {
        UserEntity entity = buildEmailUserEntity();
        UserProfileResponse expected = buildExpectedUserProfileResponse();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(entity));

        UserProfileResponse actual = userProfileService.getCurrentUserProfile(USER_ID);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenMissingUser_getCurrentUserProfile_shouldThrowUserNotFoundException() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getCurrentUserProfile(missingId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void givenValidGameIdentifiers_updateGameIdentifiers_shouldNormalizeAndPersist() {
        UserEntity user = buildEmailOnlyUserEntity();
        user.setAvatarUrl(AVATAR_URL);
        UpdateGameIdentifiersRequest request = buildValidUpdateGameIdentifiersRequest();
        UserProfileResponse expected = buildExpectedUserProfileResponse();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse actual = userProfileService.updateGameIdentifiers(USER_ID, request);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenBlankGameIdentifiersForEmailUser_updateGameIdentifiers_shouldClearIdentifiers() {
        UserEntity user = buildEmailUserEntity();
        UpdateGameIdentifiersRequest request = buildClearGameIdentifiersRequest();
        UserProfileResponse expected = buildClearedUserProfileResponse();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse actual = userProfileService.updateGameIdentifiers(USER_ID, request);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void givenSteamOnlyUserClearingSteamId_updateGameIdentifiers_shouldThrowInvalidGameIdentifierException() {
        UserEntity steamOnlyUser = buildSteamOnlyUserEntity();
        UpdateGameIdentifiersRequest request = buildClearGameIdentifiersRequest();
        when(userRepository.findById(SECOND_USER_ID)).thenReturn(Optional.of(steamOnlyUser));

        assertThatThrownBy(() -> userProfileService.updateGameIdentifiers(SECOND_USER_ID, request))
                .isInstanceOf(InvalidGameIdentifierException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenInvalidGameIdentifiers_updateGameIdentifiers_shouldThrowInvalidGameIdentifierException() {
        UserEntity user = buildEmailOnlyUserEntity();
        UpdateGameIdentifiersRequest request = buildInvalidGameIdentifiersRequest();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userProfileService.updateGameIdentifiers(USER_ID, request))
                .isInstanceOf(InvalidGameIdentifierException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void givenExistingUsers_findUserByIdAndFindUsersByIds_shouldReturnSummaries() {
        UserEntity user = buildEmailUserEntity();
        UserProfileSummary expectedSummary = buildExpectedUserProfileSummary();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findAllById(List.of(USER_ID))).thenReturn(List.of(user));

        Optional<UserProfileSummary> single = userProfileService.findUserById(USER_ID);
        Map<UUID, UserProfileSummary> batch = userProfileService.findUsersByIds(List.of(USER_ID));

        assertThat(single).isPresent();
        assertThat(single.get()).usingRecursiveComparison().isEqualTo(expectedSummary);
        assertThat(batch).containsOnlyKeys(USER_ID);
        assertThat(batch.get(USER_ID)).usingRecursiveComparison().isEqualTo(expectedSummary);
    }
}

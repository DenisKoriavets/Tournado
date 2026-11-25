package com.tournado.core.identity.api;

import com.tournado.core.common.security.AuthenticatedUser;
import com.tournado.core.common.security.CurrentUserProvider;
import com.tournado.core.identity.api.dto.UpdateGameIdentifiersRequest;
import com.tournado.core.identity.api.dto.UserProfileResponse;
import com.tournado.core.identity.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserProvider currentUserProvider;

    public UserProfileController(UserProfileService userProfileService, CurrentUserProvider currentUserProvider) {
        this.userProfileService = userProfileService;
        this.currentUserProvider = currentUserProvider;
    }

    @PatchMapping("/me/game-identifiers")
    public ResponseEntity<UserProfileResponse> updateGameIdentifiers(
            @RequestBody UpdateGameIdentifiersRequest request) {
        AuthenticatedUser currentUser = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(userProfileService.updateGameIdentifiers(currentUser.userId(), request));
    }
}

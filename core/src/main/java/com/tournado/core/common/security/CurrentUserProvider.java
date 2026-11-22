package com.tournado.core.common.security;

import java.util.Optional;

public interface CurrentUserProvider {

    Optional<AuthenticatedUser> findCurrentUser();

    AuthenticatedUser requireCurrentUser();
}

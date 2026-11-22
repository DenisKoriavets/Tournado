package com.tournado.core.identity.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface UserQueryService {

    Optional<UserProfileSummary> findUserById(UUID userId);

    Map<UUID, UserProfileSummary> findUsersByIds(Collection<UUID> userIds);
}

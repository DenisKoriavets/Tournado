package com.tournado.core.identity.repository;

import com.tournado.core.identity.domain.EmailLoginTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLoginTokenRepository extends JpaRepository<EmailLoginTokenEntity, UUID> {

    Optional<EmailLoginTokenEntity> findByTokenHash(String tokenHash);
}

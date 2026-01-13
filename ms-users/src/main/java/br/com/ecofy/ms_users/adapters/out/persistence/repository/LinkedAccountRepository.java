package br.com.ecofy.ms_users.adapters.out.persistence.repository;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.LinkedAccountEntity;
import br.com.ecofy.ms_users.core.domain.enums.AccountProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccountEntity, UUID> {
    List<LinkedAccountEntity> findByUserId(UUID userId);
    Optional<LinkedAccountEntity> findByUserIdAndProviderAndExternalAccountRef(UUID userId, AccountProvider provider, String externalAccountRef);
}

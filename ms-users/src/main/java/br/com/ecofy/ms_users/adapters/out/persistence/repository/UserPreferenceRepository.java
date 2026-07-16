package br.com.ecofy.ms_users.adapters.out.persistence.repository;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserPreferenceEntity;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, UUID> {
    List<UserPreferenceEntity> findByUserId(UUID userId);
    Optional<UserPreferenceEntity> findByUserIdAndKey(UUID userId, PreferenceKey key);
    int deleteByUserIdAndKeyIn(UUID userId, Collection<PreferenceKey> keys);
}

package br.com.ecofy.ms_users.adapters.out.persistence.repository;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByExternalAuthId(String externalAuthId);
}

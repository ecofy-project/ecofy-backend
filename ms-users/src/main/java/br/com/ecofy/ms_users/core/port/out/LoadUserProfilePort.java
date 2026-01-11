package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.EcoUserProfile;

import java.util.Optional;
import java.util.UUID;

public interface LoadUserProfilePort {
    Optional<EcoUserProfile> findById(UUID id);
    Optional<EcoUserProfile> findByExternalAuthId(String externalAuthId);
}
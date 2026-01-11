package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.UserPreference;

import java.util.List;
import java.util.UUID;

public interface LoadUserPreferencesPort {
    List<UserPreference> findByUserId(UUID userId);
}

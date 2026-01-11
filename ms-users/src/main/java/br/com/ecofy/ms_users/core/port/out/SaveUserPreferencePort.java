package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.UserPreference;

import java.util.List;

public interface SaveUserPreferencePort {
    List<UserPreference> upsertAll(List<UserPreference> prefs);
}

package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.EcoUserProfile;

public interface SaveUserProfilePort {
    EcoUserProfile save(EcoUserProfile profile);
}

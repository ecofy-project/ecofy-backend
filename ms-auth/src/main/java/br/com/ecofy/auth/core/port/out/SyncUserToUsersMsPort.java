package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.AuthUser;

public interface SyncUserToUsersMsPort {

    void upsertUser(AuthUser user);

}
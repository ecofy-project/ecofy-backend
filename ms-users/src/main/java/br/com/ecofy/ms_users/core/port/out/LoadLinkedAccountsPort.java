package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.LinkedAccount;

import java.util.List;
import java.util.UUID;

public interface LoadLinkedAccountsPort {
    List<LinkedAccount> findByUserId(UUID userId);
}
package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.LinkedAccount;

public interface SaveLinkedAccountPort {
    LinkedAccount save(LinkedAccount acc);
}

package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.Connection;

public interface SaveConnectionPort {
    Connection save(Connection connection);
}
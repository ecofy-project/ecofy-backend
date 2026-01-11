package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.result.ConnectionResult;

import java.util.List;
import java.util.UUID;

public interface ListConnectionsUseCase {
    List<ConnectionResult> listByUserId(UUID userId);
}
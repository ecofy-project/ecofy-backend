package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.pagination.PagedResult;
import br.com.ecofy.ms_users.core.application.pagination.PageQuery;
import br.com.ecofy.ms_users.core.application.result.ConnectionResult;

import java.util.List;
import java.util.UUID;

public interface ListConnectionsUseCase {

    List<ConnectionResult> listByUserId(UUID userId);

    PagedResult<ConnectionResult> listByUserId(UUID userId, PageQuery query);
}
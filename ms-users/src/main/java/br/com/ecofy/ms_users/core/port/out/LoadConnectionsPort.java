package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.application.pagination.PagedResult;
import br.com.ecofy.ms_users.core.application.pagination.PageQuery;
import br.com.ecofy.ms_users.core.domain.Connection;

import java.util.List;
import java.util.UUID;

public interface LoadConnectionsPort {

    List<Connection> findByUserId(UUID userId);

    PagedResult<Connection> findByUserId(UUID userId, PageQuery query);
}
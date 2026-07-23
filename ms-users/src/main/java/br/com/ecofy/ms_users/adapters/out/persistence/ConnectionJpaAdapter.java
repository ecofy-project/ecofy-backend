package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.mapper.ConnectionMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.ConnectionRepository;
import br.com.ecofy.ms_users.core.domain.Connection;
import br.com.ecofy.ms_users.core.port.out.LoadConnectionsPort;
import br.com.ecofy.ms_users.core.port.out.SaveConnectionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class ConnectionJpaAdapter implements SaveConnectionPort, LoadConnectionsPort {

    private final ConnectionRepository repo;
    private final ConnectionMapper mapper;

    // Inicializa o adapter JPA de Connection com o repositório e o mapper (entity <-> domain).
    public ConnectionJpaAdapter(ConnectionRepository repo, ConnectionMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    // Persiste uma Connection no banco via JPA e retorna a entidade salva convertida para domínio.
    @Override
    public Connection save(Connection connection) {
        Objects.requireNonNull(connection, "connection must not be null");
        Objects.requireNonNull(connection.getUserId(), "connection.userId must not be null");

        log.debug(
                "[ConnectionJpaAdapter] - [save] -> userId={} type={} provider={}",
                connection.getUserId().value(),
                connection.getType(),
                connection.getProvider()
        );

        var saved = repo.save(mapper.toEntity(connection));

        log.info(
                "[ConnectionJpaAdapter] - [save] -> saved connectionId={} userId={} type={} provider={}",
                saved.getId(),
                saved.getUserId(),
                saved.getType(),
                saved.getProvider()
        );

        return mapper.toDomain(saved);
    }

    // Busca as conexões do usuário paginadas no banco, a partir de uma consulta já saneada.
    @Override
    public br.com.ecofy.ms_users.core.application.pagination.PagedResult<Connection> findByUserId(
            UUID userId,
            br.com.ecofy.ms_users.core.application.pagination.PageQuery query
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(query, "query must not be null");

        var direction = query.direction() == br.com.ecofy.ms_users.core.application.pagination.PageQuery.Direction.ASC
                ? org.springframework.data.domain.Sort.Direction.ASC
                : org.springframework.data.domain.Sort.Direction.DESC;

        var pageable = org.springframework.data.domain.PageRequest.of(
                query.page(), query.size(), org.springframework.data.domain.Sort.by(direction, query.sortBy()));

        var page = repo.findByUserId(userId, pageable);

        log.debug("[ConnectionJpaAdapter] - [findByUserId(paged)] -> userId={} page={} size={} total={}",
                userId, query.page(), query.size(), page.getTotalElements());

        return new br.com.ecofy.ms_users.core.application.pagination.PagedResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                query.page(),
                query.size(),
                page.getTotalElements());
    }

    // Carrega todas as Connections associadas a um userId, convertendo de entity para domínio.
    @Override
    public List<Connection> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        log.debug("[ConnectionJpaAdapter] - [findByUserId] -> userId={}", userId);

        var list = repo.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();

        log.info(
                "[ConnectionJpaAdapter] - [findByUserId] -> userId={} resultSize={}",
                userId,
                list.size()
        );

        return list;
    }

}
